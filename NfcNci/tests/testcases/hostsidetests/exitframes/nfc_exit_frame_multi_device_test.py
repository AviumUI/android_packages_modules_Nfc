#  Copyright (C) 2025 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

# Lint as: python3
"""
This test suite is intended to be run on devices that support the exit frame
capability added in Android 16. The tests set the exit frame table, then use a
PN532 or Casimir to send polling frames to the device under test.

The tests are currently very basic. They do not test that the device under test
actually supports exit frames, and it's possible these tests can pass with the
NFC stack's autotransact implementation. Device logs should be used to verify
observe mode was actually disabled at the firmware level.
"""

import sys

from http.client import HTTPSConnection
import json
import logging
import ssl
import sys
import time

from mobly import asserts
from mobly import base_test
from mobly import test_runner
from mobly import utils
from mobly.controllers import android_device
from mobly.controllers import android_device_lib
from mobly.controllers.android_device_lib import adb
from mobly.snippet import errors


_LOG = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)
try:
    import pn532
    from pn532.nfcutils import (
        get_apdus,
        poll_and_transact,
    )
except ImportError as e:
    _LOG.warning(f"Cannot import PN532 library due to {e}")

# Timeout to give the NFC service time to perform async actions such as
# discover tags.
_NFC_TIMEOUT_SEC = 10

_NUM_POLLING_LOOPS = 50
_FAILED_TAG_MSG =  "Reader did not detect tag, transaction not attempted."
_FAILED_TRANSACTION_MSG = "Transaction failed, check device logs for more information."

_SERVICE_PACKAGE = "com.android.nfc.service"
_PAYMENT_SERVICE_1 = _SERVICE_PACKAGE + ".PaymentService1"

class NfcExitFrameMultiDeviceTestCases(base_test.BaseTestClass):
    def _set_up_emulator(self, *args, start_emulator_fun=None, service_list=[],
                 expected_service=None, is_payment=False, preferred_service=None,
                 payment_default_service=None, should_disable_services_on_destroy=True):
        """
        Sets up emulator device for multidevice tests.
        :param is_payment: bool
            Whether test is setting up payment services. If so, this function will register
            this app as the default wallet.
        :param start_emulator_fun: fun
            Custom function to start the emulator activity. If not present,
            startSimpleEmulatorActivity will be used.
        :param service_list: list
            List of services to set up. Only used if a custom function is not called.
        :param expected_service: String
            Class name of the service expected to handle the APDUs.
        :param preferred_service: String
            Service to set as preferred service, if any.
        :param payment_default_service: String
            For payment tests only: the default payment service that is expected to handle APDUs.
        :param args: arguments for start_emulator_fun, if any

        :return:
        """
        role_held_handler = self.emulator.nfc_emulator.asyncWaitForRoleHeld(
            'RoleHeld')
        if start_emulator_fun is not None:
            start_emulator_fun(*args)
        else:
            if preferred_service is None:
                self.emulator.nfc_emulator.startSimpleEmulatorActivity(
                            service_list, expected_service, is_payment,
                            should_disable_services_on_destroy)
            else:
                self.emulator.nfc_emulator.startSimpleEmulatorActivityWithPreferredService(
                    service_list, expected_service, preferred_service, is_payment
                )

        if is_payment:
            role_held_handler.waitAndGet('RoleHeld', _NFC_TIMEOUT_SEC)
            if payment_default_service is None:
                raise Exception("Must define payment_default_service for payment tests.")
            self.emulator.nfc_emulator.waitForService(payment_default_service)

    def _is_cuttlefish_device(self, ad: android_device.AndroidDevice) -> bool:
        product_name = ad.adb.getprop("ro.product.name")
        return "cf_x86" in product_name

    def _get_casimir_id_for_device(self):
        host = "localhost"
        conn = HTTPSConnection(host, 1443, context=ssl._create_unverified_context())
        path = '/devices'
        headers = {'Content-type': 'application/json'}
        conn.request("GET", path, {}, headers)
        response = conn.getresponse()
        json_obj = json.loads(response.read())
        first_device = json_obj[0]
        return first_device["device_id"]

    def setup_class(self):
        """
        Sets up class by registering an emulator device, enabling NFC, and loading snippets.

        If a PN532 serial path is found, it uses this to configure the device. Otherwise, set up a
        second phone as a reader device.
        """
        self.pn532 = None

        # This tracks the error message for a setup failure.
        # It is set to None only if the entire setup_class runs successfully.
        self._setup_failure_reason = 'Failed to find Android device(s).'

        # Indicates if the setup failure should block (FAIL) or not block (SKIP) test cases.
        # Blocking failures indicate that something unexpectedly went wrong during test setup,
        # and the user should have it fixed.
        # Non-blocking failures indicate that the device(s) did not meet the test requirements,
        # and the test does not need to be run.
        self._setup_failure_should_block_tests = True

        try:
            devices = self.register_controller(android_device)[:1]
            if len(devices) == 1:
                self.emulator = devices[0]
            else:
                self.emulator, self.reader = devices

            self._setup_failure_reason = (
                'Cannot load emulator snippet. Is NfcEmulatorTestApp.apk '
                'installed on the emulator?'
            )
            self.emulator.load_snippet(
                'nfc_emulator', 'com.android.nfc.emulator'
            )
            self.emulator.debug_tag = 'emulator'
            if (
                not self.emulator.nfc_emulator.isNfcSupported() or
                not self.emulator.nfc_emulator.isNfcHceSupported()
            ):
                self._setup_failure_reason = f'NFC is not supported on {self.emulator}'
                self._setup_failure_should_block_tests = False
                return
            try:
                self.emulator.adb.shell(['svc', 'nfc', 'enable'])
            except adb.AdbError:
                _LOG.info("Could not enable nfc through adb.")
                self.emulator.nfc_emulator.setNfcState(True)
            if (
                hasattr(self.emulator, 'dimensions')
                and 'pn532_serial_path' in self.emulator.dimensions
            ):
                pn532_serial_path = self.emulator.dimensions["pn532_serial_path"]
            else:
                pn532_serial_path = self.user_params.get("pn532_serial_path", "")

            casimir_id = None
            if self._is_cuttlefish_device(self.emulator):
                self._setup_failure_reason = 'Failed to set up casimir connection for Cuttlefish device'
                casimir_id = self._get_casimir_id_for_device()

            if casimir_id is not None and len(casimir_id) > 0:
                self._setup_failure_reason = 'Failed to connect to casimir'
                _LOG.info("casimir_id = " + casimir_id)
                self.pn532 = pn532.Casimir(casimir_id)
            else:
                self._setup_failure_reason = 'Failed to connect to PN532 board.'
                self.pn532 = pn532.PN532(pn532_serial_path)
                self.pn532.mute()
        except Exception as e:
            _LOG.warning('setup_class failed with error %s', e)
            return
        self._setup_failure_reason = None

    def setup_test(self):
        """
        Turns emulator/reader screen on and unlocks between tests as some tests will
        turn the screen off.
        """
        if self._setup_failure_should_block_tests:
            asserts.assert_true(
                self._setup_failure_reason is None, self._setup_failure_reason
            )
        else:
            asserts.skip_if(
                self._setup_failure_reason is not None, self._setup_failure_reason
            )

        self.emulator.nfc_emulator.logInfo("*** TEST START: " + self.current_test_info.name +
                                           " ***")
        self.emulator.nfc_emulator.turnScreenOn()
        self.emulator.nfc_emulator.pressMenu()

    """Tests the autotransact functionality with exit frames.

    Test Steps:
        1. Start emulator activity and set up payment HCE Service.
        2. Enable observe mode.
        3. Poll with a broadcast frame, and attempt to transact.
        4. Wait for observe mode to be reenabled.
        5. Poll again and verify that the tag is not detected.

        Verifies:
        1. Observe mode is disabled and a transaction occurs.
        2. Verify correct exit frame was used for transaction.
        3. After the first transaction, verifies that observe mode is reenabled.
        4. After observe mode is reenabled, verifies that the tag is not
        detected.
    """
    def test_exit_frames_manifest_filter(self):
        self._set_up_emulator(
            "41fbc7b9", [], True,
            start_emulator_fun=self.emulator.nfc_emulator.startExitFrameActivity,
            service_list=[_PAYMENT_SERVICE_1],
            expected_service=_PAYMENT_SERVICE_1,
            is_payment=True,
            payment_default_service=_PAYMENT_SERVICE_1
        )
        asserts.skip_if(
                    not self.emulator.nfc_emulator.isObserveModeSupported(),
                    f"{self.emulator} observe mode not supported",
                )
        asserts.assert_true(
            self.emulator.nfc_emulator.setObserveModeEnabled(True),
            f"{self.emulator} could not set observe mode",
        )

        command_apdus, response_apdus = get_apdus(self.emulator.nfc_emulator,
                                                      _PAYMENT_SERVICE_1)
        test_pass_handler = self.emulator.nfc_emulator.asyncWaitForTestPass(
            'ExitFrameListenerSuccess'
        )
        tag_detected, transacted = poll_and_transact(
                self.pn532, command_apdus, response_apdus, "41fbc7b9")
        asserts.assert_true(
            tag_detected, _FAILED_TAG_MSG
        )
        asserts.assert_true(transacted, _FAILED_TRANSACTION_MSG)
        test_pass_handler.waitAndGet('ExitFrameListenerSuccess', _NFC_TIMEOUT_SEC)


        time.sleep(_NFC_TIMEOUT_SEC)

        # Poll again and see if tag is detected, observe mode should be enabled
        # by now.
        asserts.assert_true(
                    self.emulator.nfc_emulator.isObserveModeEnabled(),
                    f"{self.emulator} isObserveModeEnabled did not return True",
                )
        tag_detected, _ = poll_and_transact(
                self.pn532, command_apdus, response_apdus)
        asserts.assert_false(
                    tag_detected,
                    "Reader detected emulator even though observe mode was enabled."
                )

        self.emulator.nfc_emulator.setObserveModeEnabled(False)

    """Tests the autotransact functionality with exit frames.

    Test Steps:
        1. Start emulator activity and set up payment HCE Service.
        2. Enable observe mode.
        3. Poll with a broadcast frame, and attempt to transact.
        4. Wait for observe mode to be reenabled.
        5. Poll again and verify that the tag is not detected.

        Verifies:
        1. Observe mode is disabled and a transaction occurs.
        2. Verify correct exit frame was used for transaction.
        3. After the first transaction, verifies that observe mode is reenabled.
        4. After observe mode is reenabled, verifies that the tag is not
        detected.
    """
    def test_exit_frames_registered_filters(self):
        self._set_up_emulator(
            "12345678", ["12345678", "aaaa"], True,
            start_emulator_fun=self.emulator.nfc_emulator.startExitFrameActivity,
            service_list=[_PAYMENT_SERVICE_1],
            expected_service=_PAYMENT_SERVICE_1,
            is_payment=True,
            payment_default_service=_PAYMENT_SERVICE_1
        )
        asserts.skip_if(
                    not self.emulator.nfc_emulator.isObserveModeSupported(),
                    f"{self.emulator} observe mode not supported",
                )
        asserts.assert_true(
            self.emulator.nfc_emulator.setObserveModeEnabled(True),
            f"{self.emulator} could not set observe mode",
        )

        command_apdus, response_apdus = get_apdus(self.emulator.nfc_emulator,
                                                      _PAYMENT_SERVICE_1)
        test_pass_handler = self.emulator.nfc_emulator.asyncWaitForTestPass(
            'ExitFrameListenerSuccess'
        )
        tag_detected, transacted = poll_and_transact(
                self.pn532, command_apdus, response_apdus, "12345678")
        asserts.assert_true(
            tag_detected, _FAILED_TAG_MSG
        )
        asserts.assert_true(transacted, _FAILED_TRANSACTION_MSG)
        test_pass_handler.waitAndGet('ExitFrameListenerSuccess', _NFC_TIMEOUT_SEC)


        time.sleep(_NFC_TIMEOUT_SEC)

        # Poll again and see if tag is detected, observe mode should be enabled
        # by now.
        asserts.assert_true(
                    self.emulator.nfc_emulator.isObserveModeEnabled(),
                    f"{self.emulator} isObserveModeEnabled did not return True",
                )
        tag_detected, _ = poll_and_transact(
                self.pn532, command_apdus, response_apdus)
        asserts.assert_false(
                    tag_detected,
                    "Reader detected emulator even though observe mode was enabled."
                )

        self.emulator.nfc_emulator.setObserveModeEnabled(False)

    """Tests the autotransact functionality with exit frames.

    Test Steps:
        1. Start emulator activity and set up payment HCE Service.
        2. Enable observe mode.
        3. Poll with a broadcast frame, and attempt to transact.
        4. Wait for observe mode to be reenabled.
        5. Poll again and verify that the tag is not detected.

        Verifies:
        1. Observe mode is disabled and a transaction occurs.
        2. Verify correct exit frame was used for transaction.
        3. After the first transaction, verifies that observe mode is reenabled.
        4. After observe mode is reenabled, verifies that the tag is not
        detected.
    """
    def test_exit_frames_prefix_match(self):
        self._set_up_emulator(
            "dd1234", ["12345678", "dd.*", "ee.*", "ff.."], True,
            start_emulator_fun=self.emulator.nfc_emulator.startExitFrameActivity,
            service_list=[_PAYMENT_SERVICE_1],
            expected_service=_PAYMENT_SERVICE_1,
            is_payment=True,
            payment_default_service=_PAYMENT_SERVICE_1
        )
        asserts.skip_if(
                    not self.emulator.nfc_emulator.isObserveModeSupported(),
                    f"{self.emulator} observe mode not supported",
                )
        asserts.assert_true(
            self.emulator.nfc_emulator.setObserveModeEnabled(True),
            f"{self.emulator} could not set observe mode",
        )

        command_apdus, response_apdus = get_apdus(self.emulator.nfc_emulator,
                                                      _PAYMENT_SERVICE_1)
        test_pass_handler = self.emulator.nfc_emulator.asyncWaitForTestPass(
            'ExitFrameListenerSuccess'
        )
        tag_detected, transacted = poll_and_transact(
                self.pn532, command_apdus, response_apdus, "dd1234")
        asserts.assert_true(
            tag_detected, _FAILED_TAG_MSG
        )
        asserts.assert_true(transacted, _FAILED_TRANSACTION_MSG)
        test_pass_handler.waitAndGet('ExitFrameListenerSuccess', _NFC_TIMEOUT_SEC)


        time.sleep(_NFC_TIMEOUT_SEC)

        # Poll again and see if tag is detected, observe mode should be enabled
        # by now.
        asserts.assert_true(
                    self.emulator.nfc_emulator.isObserveModeEnabled(),
                    f"{self.emulator} isObserveModeEnabled did not return True",
                )
        tag_detected, _ = poll_and_transact(
                self.pn532, command_apdus, response_apdus)
        asserts.assert_false(
                    tag_detected,
                    "Reader detected emulator even though observe mode was enabled."
                )

        self.emulator.nfc_emulator.setObserveModeEnabled(False)

    """Tests the autotransact functionality with exit frames.

    Test Steps:
        1. Start emulator activity and set up payment HCE Service.
        2. Enable observe mode.
        3. Poll with a broadcast frame, and attempt to transact.
        4. Wait for observe mode to be reenabled.
        5. Poll again and verify that the tag is not detected.

        Verifies:
        1. Observe mode is disabled and a transaction occurs.
        2. Verify correct exit frame was used for transaction.
        3. After the first transaction, verifies that observe mode is reenabled.
        4. After observe mode is reenabled, verifies that the tag is not
        detected.
    """
    def test_exit_frames_mask_match(self):
        self._set_up_emulator(
            "ff11", ["12345678", "ff.."], True,
            start_emulator_fun=self.emulator.nfc_emulator.startExitFrameActivity,
            service_list=[_PAYMENT_SERVICE_1],
            expected_service=_PAYMENT_SERVICE_1,
            is_payment=True,
            payment_default_service=_PAYMENT_SERVICE_1
        )
        asserts.skip_if(
                    not self.emulator.nfc_emulator.isObserveModeSupported(),
                    f"{self.emulator} observe mode not supported",
                )
        asserts.assert_true(
            self.emulator.nfc_emulator.setObserveModeEnabled(True),
            f"{self.emulator} could not set observe mode",
        )

        command_apdus, response_apdus = get_apdus(self.emulator.nfc_emulator,
                                                      _PAYMENT_SERVICE_1)
        test_pass_handler = self.emulator.nfc_emulator.asyncWaitForTestPass(
            'ExitFrameListenerSuccess'
        )
        tag_detected, transacted = poll_and_transact(
                self.pn532, command_apdus, response_apdus, "ff11")
        asserts.assert_true(
            tag_detected, _FAILED_TAG_MSG
        )
        asserts.assert_true(transacted, _FAILED_TRANSACTION_MSG)
        test_pass_handler.waitAndGet('ExitFrameListenerSuccess', _NFC_TIMEOUT_SEC)


        time.sleep(_NFC_TIMEOUT_SEC)

        # Poll again and see if tag is detected, observe mode should be enabled
        # by now.
        asserts.assert_true(
                    self.emulator.nfc_emulator.isObserveModeEnabled(),
                    f"{self.emulator} isObserveModeEnabled did not return True",
                )
        tag_detected, _ = poll_and_transact(
                self.pn532, command_apdus, response_apdus)
        asserts.assert_false(
                    tag_detected,
                    "Reader detected emulator even though observe mode was enabled."
                )

        self.emulator.nfc_emulator.setObserveModeEnabled(False)

    """Tests the autotransact functionality with exit frames.

    Test Steps:
        1. Start emulator activity and set up payment HCE Service.
        2. Enable observe mode.
        3. Poll with a broadcast frame, and attempt to transact.
        4. Wait for observe mode to be reenabled.
        5. Poll again and verify that the tag is not detected.

        Verifies:
        1. Observe mode is disabled and a transaction occurs.
        2. Verify correct exit frame was used for transaction.
        3. After the first transaction, verifies that observe mode is reenabled.
        4. After observe mode is reenabled, verifies that the tag is not
        detected.
    """
    def test_exit_frames_mask_and_prefix_match(self):
        self._set_up_emulator(
            "ddfe1134", ["12345678", "dd..11.*", "ee.*", "ff.."], True,
            start_emulator_fun=self.emulator.nfc_emulator.startExitFrameActivity,
            service_list=[_PAYMENT_SERVICE_1],
            expected_service=_PAYMENT_SERVICE_1,
            is_payment=True,
            payment_default_service=_PAYMENT_SERVICE_1
        )
        asserts.skip_if(
                    not self.emulator.nfc_emulator.isObserveModeSupported(),
                    f"{self.emulator} observe mode not supported",
                )
        asserts.assert_true(
            self.emulator.nfc_emulator.setObserveModeEnabled(True),
            f"{self.emulator} could not set observe mode",
        )

        command_apdus, response_apdus = get_apdus(self.emulator.nfc_emulator,
                                                      _PAYMENT_SERVICE_1)
        test_pass_handler = self.emulator.nfc_emulator.asyncWaitForTestPass(
            'ExitFrameListenerSuccess'
        )
        tag_detected, transacted = poll_and_transact(
                self.pn532, command_apdus, response_apdus, "ddfe1134")
        asserts.assert_true(
            tag_detected, _FAILED_TAG_MSG
        )
        asserts.assert_true(transacted, _FAILED_TRANSACTION_MSG)
        test_pass_handler.waitAndGet('ExitFrameListenerSuccess', _NFC_TIMEOUT_SEC)


        time.sleep(_NFC_TIMEOUT_SEC)

        # Poll again and see if tag is detected, observe mode should be enabled
        # by now.
        asserts.assert_true(
                    self.emulator.nfc_emulator.isObserveModeEnabled(),
                    f"{self.emulator} isObserveModeEnabled did not return True",
                )
        tag_detected, _ = poll_and_transact(
                self.pn532, command_apdus, response_apdus)
        asserts.assert_false(
                    tag_detected,
                    "Reader detected emulator even though observe mode was enabled."
                )

        self.emulator.nfc_emulator.setObserveModeEnabled(False)

    """Tests the autotransact functionality with exit frames.

    Test Steps:
        1. Start emulator activity and set up payment HCE Service.
        2. Enable observe mode.
        3. Poll with a broadcast frame, don't transact though.
        4. Wait for observe mode to be reenabled.
        5. Poll again and verify that the tag is not detected.

        Verifies:
        1. Observe mode is disabled.
        2. Verify correct exit frame was used for transaction.
        3. After the first transaction, verifies that observe mode is reenabled.
        4. After observe mode is reenabled, verifies that the tag is not
        detected.
    """
    def test_exit_frames_no_transaction_observe_mode_reenabled(self):
        self._set_up_emulator(
            "12345678", ["12345678", "aaaa"], False,
            start_emulator_fun=self.emulator.nfc_emulator.startExitFrameActivity,
            service_list=[_PAYMENT_SERVICE_1],
            expected_service=_PAYMENT_SERVICE_1,
            is_payment=True,
            payment_default_service=_PAYMENT_SERVICE_1
        )
        asserts.skip_if(
                    not self.emulator.nfc_emulator.isObserveModeSupported(),
                    f"{self.emulator} observe mode not supported",
                )
        asserts.assert_true(
            self.emulator.nfc_emulator.setObserveModeEnabled(True),
            f"{self.emulator} could not set observe mode",
        )

        command_apdus, response_apdus = get_apdus(self.emulator.nfc_emulator,
                                                      _PAYMENT_SERVICE_1)
        test_pass_handler = self.emulator.nfc_emulator.asyncWaitForTestPass(
            'ExitFrameListenerSuccess'
        )
        tag_detected, transacted = poll_and_transact(
                self.pn532, [], [], "12345678")
        asserts.assert_true(
            tag_detected, _FAILED_TAG_MSG
        )
        test_pass_handler.waitAndGet('ExitFrameListenerSuccess', _NFC_TIMEOUT_SEC)


        time.sleep(_NFC_TIMEOUT_SEC)

        # Poll again and see if tag is detected, observe mode should be enabled
        # by now.
        asserts.assert_true(
                    self.emulator.nfc_emulator.isObserveModeEnabled(),
                    f"{self.emulator} isObserveModeEnabled did not return True",
                )
        tag_detected, _ = poll_and_transact(
                self.pn532, [], [])
        asserts.assert_false(
                    tag_detected,
                    "Reader detected emulator even though observe mode was enabled."
                )

        self.emulator.nfc_emulator.setObserveModeEnabled(False)

    def teardown_test(self):
        if hasattr(self, 'emulator') and hasattr(self.emulator, 'nfc_emulator'):
            self.emulator.nfc_emulator.closeActivity()
            self.emulator.nfc_emulator.logInfo(
                "*** TEST END: " + self.current_test_info.name + " ***")
        self.pn532.reset_buffers()
        self.pn532.mute()
        param_list = [[self.emulator]]
        utils.concurrent_exec(lambda d: d.services.create_output_excerpts_all(
            self.current_test_info),
                              param_list=param_list,
                              raise_on_exception=True)

if __name__ == '__main__':
    # Take test args
    if '--' in sys.argv:
        index = sys.argv.index('--')
        sys.argv = sys.argv[:1] + sys.argv[index + 1:]
    test_runner.main()