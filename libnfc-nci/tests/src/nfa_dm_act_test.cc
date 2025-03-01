//
// Copyright (C) 2024 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

#include "nfa_dm_act.cc"

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include "mock_gki_utils.h"

class NfaDmActDataCbackTest : public ::testing::Test {
protected:
    void SetUp() override {
        memset(&nfa_dm_cb, 0, sizeof(nfa_dm_cb));
    }
    void TearDown() override {
    }
};

TEST_F(NfaDmActDataCbackTest, TestNfcDataCevtWithValidData) {
    uint8_t conn_id = 0;
    tNFC_CONN_EVT event = NFC_DATA_CEVT;
    NFC_HDR msg;
    uint8_t data[10] = {0};
    tNFC_CONN p_data = {0};
    msg.len = 10;
    msg.offset = 0;
    tNFA_CONN_EVT_DATA evt_data = {};
    evt_data.data.status = NFA_STATUS_OK;
    evt_data.data.p_data = data;
    evt_data.data.len = msg.len;
    nfa_dm_act_data_cback(conn_id, event, &p_data);
}

TEST_F(NfaDmActDataCbackTest, TestNfcDataCevtWithNullData) {
    uint8_t conn_id = 0;
    tNFC_CONN_EVT event = NFC_DATA_CEVT;
    tNFC_CONN p_data = {};
    p_data.data.p_data = nullptr;
    nfa_dm_act_data_cback(conn_id, event, &p_data);
}

TEST_F(NfaDmActDataCbackTest, TestNfcDeactivateCevt) {
    uint8_t conn_id = 0;
    tNFC_CONN_EVT event = NFC_DEACTIVATE_CEVT;
    tNFC_CONN p_data = {};
    nfa_dm_act_data_cback(conn_id, event, &p_data);
}

class NfaDmActDeactivateTest : public ::testing::Test {
protected:
    void SetUp() override {
        memset(&nfa_dm_cb, 0, sizeof(nfa_dm_cb));
        nfa_dm_cb.disc_cb.activated_protocol = NFA_PROTOCOL_T1T;
        nfa_dm_cb.disc_cb.disc_state = NFA_DM_RFST_W4_HOST_SELECT;
        nfa_dm_cb.disc_cb.kovio_tle.in_use = false;
        appl_dta_mode_flag = false;
    }
    void TearDown() override {
    }
    MOCK_METHOD(tNFC_STATUS, nfa_dm_rf_deactivate, (tNFA_DEACTIVATE_TYPE deact_type), ());
    MOCK_METHOD(void, nfa_sys_stop_timer, (TIMER_LIST_ENT* p_tle), ());
    MOCK_METHOD(void, nfa_rw_stop_presence_check_timer, (), ());
    MOCK_METHOD(void, nfa_dm_conn_cback_event_notify, (
            uint8_t event, tNFA_CONN_EVT_DATA* p_data), ());
};

TEST_F(NfaDmActDeactivateTest, TestDeactivationToIdle) {
    tNFA_DM_MSG deactivate_msg = {};
    deactivate_msg.deactivate.sleep_mode = false;
    EXPECT_CALL(*this, nfa_dm_rf_deactivate(NFA_DEACTIVATE_TYPE_IDLE)).Times(0);
    bool result = nfa_dm_act_deactivate(&deactivate_msg);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmActDeactivateTest, TestDeactivationToSleepInvalidProtocol) {
    tNFA_DM_MSG deactivate_msg = {};
    deactivate_msg.deactivate.sleep_mode = true;
    nfa_dm_cb.disc_cb.activated_protocol = 0xFF;
    EXPECT_CALL(*this, nfa_dm_rf_deactivate(testing::_)).Times(0);
    EXPECT_CALL(*this, nfa_rw_stop_presence_check_timer()).Times(0);
    bool result = nfa_dm_act_deactivate(&deactivate_msg);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmActDeactivateTest, TestDeactivationInvalidState) {
    tNFA_DM_MSG deactivate_msg = {};
    deactivate_msg.deactivate.sleep_mode = false;
    nfa_dm_cb.disc_cb.disc_state = NFA_DM_RFST_W4_ALL_DISCOVERIES;
    nfa_dm_cb.disc_cb.activated_protocol = NFA_PROTOCOL_T1T;
    EXPECT_CALL(*this, nfa_dm_rf_deactivate(NFA_DEACTIVATE_TYPE_IDLE)).Times(0);
    bool result = nfa_dm_act_deactivate(&deactivate_msg);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmActDeactivateTest, TestDeactivationInvalidProtocol) {
    tNFA_DM_MSG deactivate_msg = {};
    deactivate_msg.deactivate.sleep_mode = true;
    nfa_dm_cb.disc_cb.activated_protocol = NFA_PROTOCOL_NFC_DEP;
    appl_dta_mode_flag = true;
    EXPECT_CALL(*this, nfa_dm_rf_deactivate(testing::_)).Times(0);
    EXPECT_CALL(*this, nfa_dm_conn_cback_event_notify(
            NFA_DEACTIVATE_FAIL_EVT, testing::_)).Times(0);
    bool result = nfa_dm_act_deactivate(&deactivate_msg);
    EXPECT_TRUE(result);
}

class NfaDmActDisableTimeoutTest : public ::testing::Test {
protected:
    void SetUp() override {
    }
    void TearDown() override {
    }
};

TEST_F(NfaDmActDisableTimeoutTest, TestDisableTimeoutBasic) {
    tNFA_DM_MSG msg = {};
    bool result = nfa_dm_act_disable_timeout(&msg);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmActDisableTimeoutTest, TestGracefulFlagIsFalse) {
    tNFA_DM_MSG msg = {};
    bool result = nfa_dm_act_disable_timeout(&msg);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmActDisableTimeoutTest, TestDisableTimeoutNoException) {
    tNFA_DM_MSG msg = {};
    bool result = nfa_dm_act_disable_timeout(&msg);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmActDisableTimeoutTest, TestDisableTimeoutReturnsTrue) {
    tNFA_DM_MSG msg = {};
    bool result = nfa_dm_act_disable_timeout(&msg);
    EXPECT_TRUE(result);
}

class NfaDmActGetRfDiscDurationTest : public ::testing::Test {
protected:
    void SetUp() override {
        nfa_dm_cb.disc_cb.disc_duration = 0;
    }
    void TearDown() override {
    }
};

TEST_F(NfaDmActGetRfDiscDurationTest, TestDefaultValue) {
    EXPECT_EQ(nfa_dm_act_get_rf_disc_duration(), 0);
}

TEST_F(NfaDmActGetRfDiscDurationTest, TestSetValue) {
    nfa_dm_cb.disc_cb.disc_duration = 100;
    EXPECT_EQ(nfa_dm_act_get_rf_disc_duration(), 100);
}

TEST_F(NfaDmActGetRfDiscDurationTest, TestBoundaryValueZero) {
    nfa_dm_cb.disc_cb.disc_duration = 0;
    EXPECT_EQ(nfa_dm_act_get_rf_disc_duration(), 0);
}

TEST_F(NfaDmActGetRfDiscDurationTest, TestBoundaryValueMax) {
    nfa_dm_cb.disc_cb.disc_duration = UINT16_MAX;
    EXPECT_EQ(nfa_dm_act_get_rf_disc_duration(), UINT16_MAX);
}

TEST_F(NfaDmActGetRfDiscDurationTest, TestRandomValue) {
    uint16_t random_value = 12345;
    nfa_dm_cb.disc_cb.disc_duration = random_value;
    EXPECT_EQ(nfa_dm_act_get_rf_disc_duration(), random_value);
}

class NfaDmActPowerOffSleepTest : public ::testing::Test {
protected:
    void SetUp() override {
    }
    void TearDown() override {
    }
    MOCK_METHOD(void, NFC_SetPowerOffSleep, (bool enable), ());
};

TEST_F(NfaDmActPowerOffSleepTest, TestPowerOffSleepFalse) {
    tNFA_DM_MSG p_data;
    p_data.hdr.layer_specific = 0;
    EXPECT_CALL(*this, NFC_SetPowerOffSleep(false)).Times(0);
    bool result = nfa_dm_act_power_off_sleep(&p_data);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmActPowerOffSleepTest, TestPowerOffSleepTrue) {
    tNFA_DM_MSG p_data;
    p_data.hdr.layer_specific = 1;
    EXPECT_CALL(*this, NFC_SetPowerOffSleep(true)).Times(0);
    bool result = nfa_dm_act_power_off_sleep(&p_data);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmActPowerOffSleepTest, TestReturnTrue) {
    tNFA_DM_MSG p_data;
    p_data.hdr.layer_specific = 0;
    bool result = nfa_dm_act_power_off_sleep(&p_data);
    EXPECT_TRUE(result);
}

class NfaDmActReleaseExclRfCtrlTest : public ::testing::Test {
protected:
    void SetUp() override {
        nfa_dm_cb.disc_cb.disc_state = NFA_DM_RFST_IDLE;
        nfa_dm_cb.disc_cb.disc_flags = 0;
        nfa_dm_cb.disc_cb.kovio_tle.in_use = false;
    }
    void TearDown() override {
    }
};

TEST_F(NfaDmActReleaseExclRfCtrlTest, TestIdleStateWithWaitingResponse) {
    nfa_dm_cb.disc_cb.disc_state = NFA_DM_RFST_IDLE;
    nfa_dm_cb.disc_cb.disc_flags |= NFA_DM_DISC_FLAGS_W4_RSP;
    bool result = nfa_dm_act_release_excl_rf_ctrl(nullptr);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmActReleaseExclRfCtrlTest, TestNonIdleStateOrNoWaitingResponse) {
    nfa_dm_cb.disc_cb.disc_state = NFA_DM_RFST_LP_LISTEN;
    bool result = nfa_dm_act_release_excl_rf_ctrl(nullptr);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmActReleaseExclRfCtrlTest, TestStopKovioTimer) {
    nfa_dm_cb.disc_cb.kovio_tle.in_use = true;
    bool result = nfa_dm_act_release_excl_rf_ctrl(nullptr);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmActReleaseExclRfCtrlTest, TestNoStopKovioTimer) {
    nfa_dm_cb.disc_cb.kovio_tle.in_use = false;
    bool result = nfa_dm_act_release_excl_rf_ctrl(nullptr);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmActReleaseExclRfCtrlTest, TestReturnTrue) {
    bool result = nfa_dm_act_release_excl_rf_ctrl(nullptr);
    EXPECT_TRUE(result);
}

void conn_callback(uint8_t event, tNFA_CONN_EVT_DATA* conn_evt) {
    EXPECT_EQ(event, NFA_EXCLUSIVE_RF_CONTROL_STARTED_EVT);
    EXPECT_EQ(conn_evt->status, NFA_STATUS_OK);
}

class NfaDmActRequestExclRfCtrlTest : public ::testing::Test {
protected:
    void ResetNfaDmCb() {
        memset(&nfa_dm_cb, 0, sizeof(nfa_dm_cb));
        nfa_dm_cb.p_excl_conn_cback = nullptr;
        nfa_dm_cb.disc_cb.disc_state = NFA_DM_RFST_IDLE;
    }
    void SetUp() override {
        ResetNfaDmCb();
    }
};

TEST_F(NfaDmActRequestExclRfCtrlTest, CallbackNotSetAndStateIdle) {
    tNFA_DM_MSG msg = {};
    msg.req_excl_rf_ctrl.p_conn_cback = conn_callback;
    msg.req_excl_rf_ctrl.p_ndef_cback = nullptr;
    msg.req_excl_rf_ctrl.poll_mask = 0x01;
    bool result = nfa_dm_act_request_excl_rf_ctrl(&msg);
    EXPECT_TRUE(result);
    EXPECT_EQ(nfa_dm_cb.p_excl_conn_cback, msg.req_excl_rf_ctrl.p_conn_cback);
    EXPECT_EQ(nfa_dm_cb.flags & NFA_DM_FLAGS_EXCL_RF_ACTIVE, NFA_DM_FLAGS_EXCL_RF_ACTIVE);
}

TEST_F(NfaDmActRequestExclRfCtrlTest, ExclusiveDiscoveryStarted) {
    tNFA_DM_MSG msg = {};
    msg.req_excl_rf_ctrl.poll_mask = 0x01;
    msg.req_excl_rf_ctrl.listen_cfg = {};
    msg.req_excl_rf_ctrl.p_conn_cback = conn_callback;
    bool result = nfa_dm_act_request_excl_rf_ctrl(&msg);
    EXPECT_TRUE(result);
    EXPECT_EQ(nfa_dm_cb.p_excl_conn_cback, msg.req_excl_rf_ctrl.p_conn_cback);
}

#define NFA_PROTOCOL_UNKNOWN 0xFF
class NfaDmActSendRawFrameTest : public ::testing::Test {
protected:
    void SetUp() override {
        nfa_dm_cb.disc_cb.disc_state = NFA_DM_RFST_IDLE;
        nfa_dm_cb.flags = 0;
        nfa_dm_cb.disc_cb.activated_protocol = NFA_PROTOCOL_T1T;
        gki_utils = new MockGkiUtils();
    }
    void TearDown() override { gki_utils = nullptr; }

    MOCK_METHOD(tNFC_STATUS, nfa_rw_send_raw_frame, (NFC_HDR* p_data), ());
    MOCK_METHOD(tNFC_STATUS, NFC_SendData, (uint8_t conn_id, NFC_HDR* p_data), ());
    MOCK_METHOD(void, NFC_SetReassemblyFlag, (bool flag), ());
};

TEST_F(NfaDmActSendRawFrameTest, SendRawFrameWhenActive) {
    tNFA_DM_MSG msg = {};
    nfa_dm_cb.disc_cb.disc_state = NFA_DM_RFST_POLL_ACTIVE;
    EXPECT_CALL(*this, NFC_SendData(NFC_RF_CONN_ID, (NFC_HDR*)&msg)).Times(0);
    EXPECT_CALL(*this, NFC_SetReassemblyFlag(true)).Times(0);
    bool result = nfa_dm_act_send_raw_frame(&msg);
    EXPECT_FALSE(result);
}

TEST_F(NfaDmActSendRawFrameTest, SendRawFrameInExclusiveMode) {
    tNFA_DM_MSG msg = {};
    nfa_dm_cb.disc_cb.disc_state = NFA_DM_RFST_POLL_ACTIVE;
    nfa_dm_cb.flags |= NFA_DM_FLAGS_EXCL_RF_ACTIVE;
    EXPECT_CALL(*this, NFC_SendData(NFC_RF_CONN_ID, (NFC_HDR*)&msg)).Times(0);
    EXPECT_CALL(*this, NFC_SetReassemblyFlag(true)).Times(0);
    bool result = nfa_dm_act_send_raw_frame(&msg);
    EXPECT_FALSE(result);
}

TEST_F(NfaDmActSendRawFrameTest, SendRawFrameInListenActiveState) {
    tNFA_DM_MSG msg = {};
    nfa_dm_cb.disc_cb.disc_state = NFA_DM_RFST_LISTEN_ACTIVE;
    EXPECT_CALL(*this, NFC_SendData(NFC_RF_CONN_ID, (NFC_HDR*)&msg)).Times(0);
    EXPECT_CALL(*this, NFC_SetReassemblyFlag(true)).Times(0);
    bool result = nfa_dm_act_send_raw_frame(&msg);
    EXPECT_FALSE(result);
}

TEST_F(NfaDmActSendRawFrameTest, SendRawFrameWithUnsupportedProtocol) {
    tNFA_DM_MSG msg = {};
    nfa_dm_cb.disc_cb.disc_state = NFA_DM_RFST_POLL_ACTIVE;
    nfa_dm_cb.disc_cb.activated_protocol = NFA_PROTOCOL_UNKNOWN;
    EXPECT_CALL(*this, NFC_SendData(NFC_RF_CONN_ID, (NFC_HDR*)&msg)).Times(0);
    EXPECT_CALL(*this, NFC_SetReassemblyFlag(true)).Times(0);
    bool result = nfa_dm_act_send_raw_frame(&msg);
    EXPECT_FALSE(result);
}

TEST_F(NfaDmActSendRawFrameTest, SendRawFrameWithProtocolT1T) {
    tNFA_DM_MSG msg = {};
    nfa_dm_cb.disc_cb.disc_state = NFA_DM_RFST_POLL_ACTIVE;
    nfa_dm_cb.disc_cb.activated_protocol = NFA_PROTOCOL_T1T;
    EXPECT_CALL(*this, nfa_rw_send_raw_frame((NFC_HDR*)&msg)).Times(0);
    EXPECT_CALL(*this, NFC_SetReassemblyFlag(true)).Times(0);
    bool result = nfa_dm_act_send_raw_frame(&msg);
    EXPECT_FALSE(result);
}

TEST_F(NfaDmActSendRawFrameTest, SendRawFrameNfcSendDataFails) {
    tNFA_DM_MSG msg = {};
    nfa_dm_cb.disc_cb.disc_state = NFA_DM_RFST_POLL_ACTIVE;
    nfa_dm_cb.disc_cb.activated_protocol = NFA_PROTOCOL_T1T;
    EXPECT_CALL(*this, NFC_SetReassemblyFlag(true)).Times(0);
    bool result = nfa_dm_act_send_raw_frame(&msg);
    EXPECT_FALSE(result);
}

TEST_F(NfaDmActSendRawFrameTest, SendRawFrameWhenInactive) {
    tNFA_DM_MSG msg = {};
    nfa_dm_cb.disc_cb.disc_state = NFA_DM_RFST_IDLE;
    EXPECT_CALL(*this, NFC_SetReassemblyFlag(true)).Times(0);
    bool result = nfa_dm_act_send_raw_frame(&msg);
    EXPECT_TRUE(result);
}

class NfaDmActSendVscTest : public ::testing::Test {
protected:
 void SetUp() override { gki_utils = new MockGkiUtils(); }
 void TearDown() override { gki_utils = nullptr; }
 MOCK_METHOD(void, NFC_SendVsCommand,
             (uint8_t oid, NFC_HDR* p_cmd, tNFA_DM_CBACK* p_cback), ());
};

TEST_F(NfaDmActSendVscTest, ValidCommandParams) {
    tNFA_DM_MSG msg = {};
    msg.send_vsc.cmd_params_len = 10;
    msg.send_vsc.oid = 0x01;
    msg.send_vsc.p_cback = nullptr;
    NFC_HDR p_cmd = {};
    p_cmd.offset = sizeof(tNFA_DM_API_SEND_VSC) - NFC_HDR_SIZE;
    p_cmd.len = 10;
    EXPECT_CALL(*this, NFC_SendVsCommand(0x01, &p_cmd, nullptr)).Times(0);
    bool result = nfa_dm_act_send_vsc(&msg);
    EXPECT_FALSE(result);
}

TEST_F(NfaDmActSendVscTest, ZeroLengthCommand) {
    tNFA_DM_MSG msg = {};
    msg.send_vsc.cmd_params_len = 0;
    msg.send_vsc.oid = 0x01;
    msg.send_vsc.p_cback = nullptr;
    NFC_HDR p_cmd = {};
    p_cmd.offset = sizeof(tNFA_DM_API_SEND_VSC) - NFC_HDR_SIZE;
    p_cmd.len = 0;
    EXPECT_CALL(*this, NFC_SendVsCommand(0x01, &p_cmd, nullptr)).Times(0);
    bool result = nfa_dm_act_send_vsc(&msg);
    EXPECT_FALSE(result);
}

TEST_F(NfaDmActSendVscTest, InvalidCallback) {
    tNFA_DM_MSG msg = {};
    msg.send_vsc.cmd_params_len = 10;
    msg.send_vsc.oid = 0x01;
    msg.send_vsc.p_cback = nullptr;
    NFC_HDR p_cmd = {};
    p_cmd.offset = sizeof(tNFA_DM_API_SEND_VSC) - NFC_HDR_SIZE;
    p_cmd.len = 10;
    EXPECT_CALL(*this, NFC_SendVsCommand(0x01, &p_cmd, nullptr)).Times(0);
    bool result = nfa_dm_act_send_vsc(&msg);
    EXPECT_FALSE(result);
}

TEST_F(NfaDmActSendVscTest, BufferNotFreedByNfaSys) {
    tNFA_DM_MSG msg = {};
    msg.send_vsc.cmd_params_len = 10;
    msg.send_vsc.oid = 0x01;
    msg.send_vsc.p_cback = nullptr;
    EXPECT_CALL(*this, NFC_SendVsCommand(0x01, testing::NotNull(), nullptr)).Times(0);
    bool result = nfa_dm_act_send_vsc(&msg);
    EXPECT_FALSE(result);
}

TEST_F(NfaDmActSendVscTest, DifferentParams) {
    tNFA_DM_MSG msg = {};
    msg.send_vsc.cmd_params_len = 20;
    msg.send_vsc.oid = 0x02;
    msg.send_vsc.p_cback = nullptr;
    NFC_HDR p_cmd = {};
    p_cmd.offset = sizeof(tNFA_DM_API_SEND_VSC) - NFC_HDR_SIZE;
    p_cmd.len = 20;
    EXPECT_CALL(*this, NFC_SendVsCommand(0x02, &p_cmd, nullptr)).Times(0);
    bool result = nfa_dm_act_send_vsc(&msg);
    EXPECT_FALSE(result);
}

class NfaDmActUpdateRfParamsTest : public ::testing::Test {
protected:
    void SetUp() override {
    }
    void TearDown() override {
    }
    MOCK_METHOD(tNFC_STATUS, NFC_UpdateRFCommParams, (tNFA_RF_COMM_PARAMS* params), ());
    MOCK_METHOD(void, nfa_dm_conn_cback_event_notify, (
            tNFA_EE_EVT event, tNFA_CONN_EVT_DATA* conn_evt), ());
};

TEST_F(NfaDmActUpdateRfParamsTest, ValidRfParams) {
    tNFA_DM_MSG msg = {};
    msg.update_rf_params.params = {};
    EXPECT_CALL(*this, nfa_dm_conn_cback_event_notify(testing::_, testing::_)).Times(0);
    bool result = nfa_dm_act_update_rf_params(&msg);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmActUpdateRfParamsTest, FailedRfParamsUpdate) {
    tNFA_DM_MSG msg = {};
    msg.update_rf_params.params = {};
    tNFA_CONN_EVT_DATA conn_evt;
    conn_evt.status = NFA_STATUS_FAILED;
    EXPECT_CALL(*this, nfa_dm_conn_cback_event_notify(
            NFA_UPDATE_RF_PARAM_RESULT_EVT, &conn_evt)).Times(0);
    bool result = nfa_dm_act_update_rf_params(&msg);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmActUpdateRfParamsTest, AlwaysReturnTrue) {
    tNFA_DM_MSG msg = {};
    msg.update_rf_params.params = {};
    bool result = nfa_dm_act_update_rf_params(&msg);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmActUpdateRfParamsTest, CorrectEventAndStatusOnFailure) {
    tNFA_DM_MSG msg = {};
    msg.update_rf_params.params = {};
    tNFA_CONN_EVT_DATA conn_evt;
    conn_evt.status = NFA_STATUS_FAILED;
    EXPECT_CALL(*this, nfa_dm_conn_cback_event_notify(
            NFA_UPDATE_RF_PARAM_RESULT_EVT, &conn_evt)).Times(0);
    bool result = nfa_dm_act_update_rf_params(&msg);
    EXPECT_TRUE(result);
}

class NfaDmRevtTest : public ::testing::Test {
protected:
    void SetUp() override {
    }
    void TearDown() override {
    }
};

TEST_F(NfaDmRevtTest, ConvertNfcRevtToStr) {
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_ENABLE_REVT), "NFC_ENABLE_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_DISABLE_REVT), "NFC_DISABLE_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_SET_CONFIG_REVT), "NFC_SET_CONFIG_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_GET_CONFIG_REVT), "NFC_GET_CONFIG_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_NFCEE_DISCOVER_REVT), "NFC_NFCEE_DISCOVER_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_NFCEE_INFO_REVT), "NFC_NFCEE_INFO_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_NFCEE_MODE_SET_REVT), "NFC_NFCEE_MODE_SET_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_NFCEE_PL_CONTROL_REVT), "NFC_NFCEE_PL_CONTROL_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_RF_FIELD_REVT), "NFC_RF_FIELD_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_EE_ACTION_REVT), "NFC_EE_ACTION_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_EE_DISCOVER_REQ_REVT), "NFC_EE_DISCOVER_REQ_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_SET_ROUTING_REVT), "NFC_SET_ROUTING_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_GET_ROUTING_REVT), "NFC_GET_ROUTING_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_GEN_ERROR_REVT), "NFC_GEN_ERROR_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_NFCC_RESTART_REVT), "NFC_NFCC_RESTART_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_NFCC_TIMEOUT_REVT), "NFC_NFCC_TIMEOUT_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_NFCC_TRANSPORT_ERR_REVT), "NFC_NFCC_TRANSPORT_ERR_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_NFCC_POWER_OFF_REVT), "NFC_NFCC_POWER_OFF_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(NFC_NFCEE_STATUS_REVT), "NFC_NFCEE_STATUS_REVT");
    EXPECT_EQ(nfa_dm_nfc_revt_2_str(static_cast<tNFC_RESPONSE_EVT>(999)), "unknown revt");
}

class NfaDmNotifyActivationStatusTest : public ::testing::Test {
protected:
    void SetUp() override {
        memset(&nfa_dm_cb, 0, sizeof(nfa_dm_cb));
        nfa_dm_cb.p_activate_ntf = nullptr;
        nfa_dm_cb.disc_cb.activated_protocol = NFC_PROTOCOL_T1T;
    }
    void TearDown() override {
    }
    MOCK_METHOD(void, nfa_dm_conn_cback_event_notify, (
            uint8_t event, tNFA_CONN_EVT_DATA* p_data), ());
    MOCK_METHOD(void, nfa_dm_rf_deactivate, (tNFA_DEACTIVATE_TYPE deact_type), ());
};

TEST_F(NfaDmNotifyActivationStatusTest, TestNullActivateNtf) {
    tNFA_STATUS status = NFA_STATUS_OK;
    tNFA_TAG_PARAMS params;
    nfa_dm_notify_activation_status(status, &params);
    EXPECT_CALL(*this, nfa_dm_conn_cback_event_notify(testing::_, testing::_)).Times(0);
}

TEST_F(NfaDmNotifyActivationStatusTest, TestActivationWithoutParams) {
    tNFA_STATUS status = NFA_STATUS_OK;
    tNFA_TAG_PARAMS* p_params = nullptr;
    EXPECT_CALL(*this, nfa_dm_conn_cback_event_notify(NFA_ACTIVATED_EVT, testing::_)).Times(0);
    nfa_dm_notify_activation_status(status, p_params);
    delete nfa_dm_cb.p_activate_ntf;
    nfa_dm_cb.p_activate_ntf = nullptr;
}

TEST_F(NfaDmNotifyActivationStatusTest, TestInvalidStatus) {
    tNFA_STATUS status = NFA_STATUS_FAILED;
    tNFA_TAG_PARAMS params;
    EXPECT_CALL(*this, nfa_dm_rf_deactivate(NFA_DEACTIVATE_TYPE_DISCOVERY)).Times(0);
    EXPECT_CALL(*this, nfa_dm_conn_cback_event_notify(testing::_, testing::_)).Times(0);
    nfa_dm_notify_activation_status(status, &params);
    delete nfa_dm_cb.p_activate_ntf;
    nfa_dm_cb.p_activate_ntf = nullptr;
}

class NfaDmPollDiscCbackTest : public ::testing::Test {
protected:
    void SetUp() override {
        memset(&nfa_dm_cb, 0, sizeof(nfa_dm_cb));
    }
    void TearDown() override {
        if (nfa_dm_cb.p_activate_ntf) {
            GKI_freebuf(nfa_dm_cb.p_activate_ntf);
        }
    }
    MOCK_METHOD(void, nfa_dm_conn_cback_event_notify, (
            uint8_t event, tNFA_CONN_EVT_DATA* p_data), ());
    MOCK_METHOD(void, nfa_rw_proc_disc_evt, (
            tNFA_DM_RF_DISC_EVT event, tNFC_DISCOVER* p_data, bool notify), ());
    MOCK_METHOD(void, NFC_SetStaticRfCback, (void* cback), ());
    MOCK_METHOD(void, nfa_dm_rf_deactivate, (tNFA_DEACTIVATE_TYPE deact_type), ());
    MOCK_METHOD(void, nfa_dm_delete_rf_discover, (tNFA_HANDLE handle), ());
};

TEST_F(NfaDmPollDiscCbackTest, TestStartEvent) {
    tNFA_DM_RF_DISC_EVT event = NFA_DM_RF_DISC_START_EVT;
    tNFC_DISCOVER p_data = {};
    EXPECT_CALL(*this, nfa_dm_conn_cback_event_notify(testing::_, testing::_)).Times(0);
    nfa_dm_poll_disc_cback(event, &p_data);
}

TEST_F(NfaDmPollDiscCbackTest, TestDeactivatedEventNoEventPending) {
    tNFA_DM_RF_DISC_EVT event = NFA_DM_RF_DISC_DEACTIVATED_EVT;
    tNFC_DISCOVER p_data = {};
    p_data.deactivate.type = NFC_DEACTIVATE_TYPE_IDLE;
    nfa_dm_cb.flags = 0;
    EXPECT_CALL(*this, nfa_dm_conn_cback_event_notify(testing::_, testing::_)).Times(0);
    nfa_dm_poll_disc_cback(event, &p_data);
}

class NfaDmPollDiscCbackWrapperTest : public testing::Test {
protected:
};

TEST_F(NfaDmPollDiscCbackWrapperTest, TestValidEventAndData) {
    tNFA_DM_RF_DISC_EVT event = NFA_DM_RF_DISC_START_EVT;
    tNFC_DISCOVER data;
    tNFC_DISCOVER* p_data = &data;
    nfa_dm_poll_disc_cback_dta_wrapper(NFA_DM_RF_DISC_START_EVT, p_data);
}

TEST_F(NfaDmPollDiscCbackWrapperTest, TestNullData) {
    tNFA_DM_RF_DISC_EVT event = NFA_DM_RF_DISC_START_EVT;
    tNFC_DISCOVER *p_data = nullptr;
    nfa_dm_poll_disc_cback_dta_wrapper(event, p_data);
}

class NfaDmProcNfccPowerModeTest : public ::testing::Test {
protected:
    void SetUp() override {
        memset(&nfa_dm_cb, 0, sizeof(nfa_dm_cb));
    }
    void TearDown() override {
    }
    MOCK_METHOD(void, nfa_sys_cback_notify_nfcc_power_mode_proc_complete, (uint8_t id), ());
};

TEST_F(NfaDmProcNfccPowerModeTest, TestFullPowerMode) {
    uint8_t nfcc_power_mode = NFA_DM_PWR_MODE_FULL;
    nfa_dm_proc_nfcc_power_mode(nfcc_power_mode);
    EXPECT_EQ(nfa_dm_cb.setcfg_pending_mask, 0);
    EXPECT_EQ(nfa_dm_cb.setcfg_pending_num, 0);
    EXPECT_EQ(nfa_dm_cb.flags & NFA_DM_FLAGS_POWER_OFF_SLEEP, 0);
    EXPECT_CALL(*this, nfa_sys_cback_notify_nfcc_power_mode_proc_complete(NFA_ID_DM)).Times(0);
}

TEST_F(NfaDmProcNfccPowerModeTest, TestOffSleepMode) {
    uint8_t nfcc_power_mode = NFA_DM_PWR_MODE_OFF_SLEEP;
    nfa_dm_proc_nfcc_power_mode(nfcc_power_mode);
    EXPECT_EQ(nfa_dm_cb.setcfg_pending_mask, 0);
    EXPECT_EQ(nfa_dm_cb.setcfg_pending_num, 0);
    EXPECT_NE(nfa_dm_cb.flags & NFA_DM_FLAGS_POWER_OFF_SLEEP, 0);
    EXPECT_CALL(*this, nfa_sys_cback_notify_nfcc_power_mode_proc_complete(NFA_ID_DM)).Times(0);
}

TEST_F(NfaDmProcNfccPowerModeTest, TestInvalidPowerMode) {
    uint8_t nfcc_power_mode = 99;
    nfa_dm_proc_nfcc_power_mode(nfcc_power_mode);
    EXPECT_EQ(nfa_dm_cb.setcfg_pending_mask, 0);
    EXPECT_EQ(nfa_dm_cb.setcfg_pending_num, 0);
    EXPECT_EQ(nfa_dm_cb.flags & NFA_DM_FLAGS_POWER_OFF_SLEEP, 0);
    EXPECT_CALL(*this, nfa_sys_cback_notify_nfcc_power_mode_proc_complete(NFA_ID_DM)).Times(0);
}
void dummy_conn_callback(
        [[maybe_unused]] unsigned char event, [[maybe_unused]] tNFA_CONN_EVT_DATA* data) {
}
void dummy_ndef_callback(
        [[maybe_unused]] unsigned char event, [[maybe_unused]] tNFA_NDEF_EVT_DATA* data) {
}

class NfaDmRelExclRfControlAndNotifyTest : public ::testing::Test {
protected:
    void SetUp() override {
        memset(&nfa_dm_cb, 0, sizeof(nfa_dm_cb));
    }
};

TEST(NfaDmRelExclRfControlAndNotifyTest, TestNormalScenario) {
    nfa_dm_cb.flags = NFA_DM_FLAGS_EXCL_RF_ACTIVE;
    nfa_dm_cb.p_excl_conn_cback = &dummy_conn_callback;
    nfa_dm_cb.p_excl_ndef_cback = &dummy_ndef_callback;
    nfa_dm_rel_excl_rf_control_and_notify();
    EXPECT_EQ(nfa_dm_cb.flags & NFA_DM_FLAGS_EXCL_RF_ACTIVE, 0);
    EXPECT_EQ(nfa_dm_cb.p_excl_conn_cback, nullptr);
    EXPECT_EQ(nfa_dm_cb.p_excl_ndef_cback, nullptr);
}

TEST(NfaDmRelExclRfControlAndNotifyTest, TestCallbacksReset) {
    nfa_dm_cb.p_excl_conn_cback = &dummy_conn_callback;
    nfa_dm_cb.p_excl_ndef_cback = &dummy_ndef_callback;
    nfa_dm_rel_excl_rf_control_and_notify();
    EXPECT_EQ(nfa_dm_cb.p_excl_conn_cback, nullptr);
    EXPECT_EQ(nfa_dm_cb.p_excl_ndef_cback, nullptr);
}

TEST(NfaDmRelExclRfControlAndNotifyTest, TestNoActionOnNoFlagSet) {
    nfa_dm_cb.flags = 0;
    nfa_dm_cb.p_excl_conn_cback = &dummy_conn_callback;
    nfa_dm_cb.p_excl_ndef_cback = &dummy_ndef_callback;
    nfa_dm_rel_excl_rf_control_and_notify();
    EXPECT_EQ(nfa_dm_cb.flags, 0);
    EXPECT_EQ(nfa_dm_cb.p_excl_conn_cback, nullptr);
    EXPECT_EQ(nfa_dm_cb.p_excl_ndef_cback, nullptr);
}

class NfaDmModuleInitTest : public ::testing::Test {
protected:
};

void TestCallback(uint8_t event, tNFA_DM_CBACK_DATA* data) {
    EXPECT_EQ(event, NFA_DM_ENABLE_EVT);
    EXPECT_EQ(data->status, NFA_STATUS_OK);
}

TEST_F(NfaDmModuleInitTest, FlagsAreCleared) {
    nfa_dm_cb.flags = NFA_DM_FLAGS_ENABLE_EVT_PEND;
    nfa_dm_cb.p_dm_cback = TestCallback;
    nfa_dm_module_init_cback();
    EXPECT_EQ(nfa_dm_cb.flags & NFA_DM_FLAGS_ENABLE_EVT_PEND, 0);
}

TEST_F(NfaDmModuleInitTest, CallbackInvokedWithCorrectData) {
    nfa_dm_cb.flags = NFA_DM_FLAGS_ENABLE_EVT_PEND;
    nfa_dm_cb.p_dm_cback = TestCallback;
    nfa_dm_module_init_cback();
}

class NfaDmSysEnableTest : public ::testing::Test {
protected:
};

TEST_F(NfaDmSysEnableTest, CallsNfaDmSetInitNciParamsOnce) {
    nfa_dm_sys_enable();
    EXPECT_TRUE(true);
}

TEST_F(NfaDmSysEnableTest, CallsNfaDmSetInitNciParamsMultipleTimes) {
    nfa_dm_sys_enable();
    nfa_dm_sys_enable();
    EXPECT_TRUE(true);
}

class MockNfaSys {
public:
    MOCK_METHOD(void, nfa_sys_stop_timer, (TIMER_LIST_ENT* p_tle), ());
};

class MockNfaDm {
public:
    MOCK_METHOD(void, nfa_dm_ndef_dereg_all, (), ());
};

class NfaDmDisableCompleteTest : public testing::Test {
protected:
    void SetUp() override {
        nfa_dm_cb.flags = 0;
        nfa_dm_cb.p_dm_cback = nullptr;
        nfa_dm_cb.tle = TIMER_LIST_ENT();
        mock_nfa_sys = std::make_unique<MockNfaSys>();
        mock_nfa_dm = std::make_unique<MockNfaDm>();
    }

    void TearDown() override {
        testing::Mock::VerifyAndClearExpectations(mock_nfa_sys.get());
        testing::Mock::VerifyAndClearExpectations(mock_nfa_dm.get());
    }
    std::unique_ptr<MockNfaSys> mock_nfa_sys;
    std::unique_ptr<MockNfaDm> mock_nfa_dm;
};

TEST_F(NfaDmDisableCompleteTest, FlagsUpdatedCorrectly) {
    nfa_dm_disable_complete();
    EXPECT_EQ(nfa_dm_cb.flags & NFA_DM_FLAGS_DM_DISABLING_NFC, NFA_DM_FLAGS_DM_DISABLING_NFC);
}

TEST_F(NfaDmDisableCompleteTest, FunctionsCalledWhenFlagNotSet) {
    nfa_dm_cb.flags = 0;
    EXPECT_CALL(*mock_nfa_sys, nfa_sys_stop_timer(&nfa_dm_cb.tle)).Times(0);
    EXPECT_CALL(*mock_nfa_dm, nfa_dm_ndef_dereg_all()).Times(0);
    nfa_dm_disable_complete();
}

TEST_F(NfaDmDisableCompleteTest, NoActionsWhenFlagAlreadySet) {
    nfa_dm_cb.flags = NFA_DM_FLAGS_DM_DISABLING_NFC;
    EXPECT_CALL(*mock_nfa_sys, nfa_sys_stop_timer(&nfa_dm_cb.tle)).Times(0);
    EXPECT_CALL(*mock_nfa_dm, nfa_dm_ndef_dereg_all()).Times(0);
    nfa_dm_disable_complete();
}

class MockDmGet {
public:
    MOCK_METHOD(void, NFC_GetConfig, (uint8_t num_ids, uint8_t* p_pmids), ());
};

class NfaDmGetConfigTest : public testing::Test {
protected:
    void SetUp() override {
        p_data = new tNFA_DM_MSG();
        mock_dm_get = std::make_unique<MockDmGet>();
    }

    void TearDown() override {
        delete p_data;
        testing::Mock::VerifyAndClearExpectations(mock_dm_get.get());
    }
    tNFA_DM_MSG* p_data;
    std::unique_ptr<MockDmGet> mock_dm_get;
};

TEST_F(NfaDmGetConfigTest, ValidInputCallsNFCGetConfig) {
    uint8_t num_ids = 3;
    uint8_t pmids[] = {10, 20, 30};
    p_data->getconfig.num_ids = num_ids;
    p_data->getconfig.p_pmids = pmids;
    EXPECT_CALL(*mock_dm_get, NFC_GetConfig(num_ids, pmids)).Times(0);
    bool result = nfa_dm_get_config(p_data);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmGetConfigTest, ZeroNumIds) {
    p_data->getconfig.num_ids = 0;
    p_data->getconfig.p_pmids = nullptr;
    EXPECT_CALL(*mock_dm_get, NFC_GetConfig(0, nullptr)).Times(0);
    bool result = nfa_dm_get_config(p_data);
    EXPECT_TRUE(result);
}

TEST_F(NfaDmGetConfigTest, DifferentValidConfig) {
    uint8_t num_ids = 2;
    uint8_t pmids[] = {100, 200};
    p_data->getconfig.num_ids = num_ids;
    p_data->getconfig.p_pmids = pmids;
    EXPECT_CALL(*mock_dm_get, NFC_GetConfig(num_ids, pmids)).Times(0);
    bool result = nfa_dm_get_config(p_data);
    EXPECT_TRUE(result);
}

