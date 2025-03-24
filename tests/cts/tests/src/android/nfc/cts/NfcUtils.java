/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.nfc.cts;

import static android.Manifest.permission.WRITE_SECURE_SETTINGS;

import static com.android.compatibility.common.util.PropertyUtil.getVsrApiLevel;

import static org.junit.Assume.assumeTrue;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class NfcUtils {
    private NfcUtils() {}

    private static final String TAG = "NfcUtils";

    static boolean enableNfc(@NonNull NfcAdapter nfcAdapter, @NonNull Context context) {
        return setNfcState(nfcAdapter, context, /* setEnabled= */ true, null);
    }

    static boolean disableNfc(@NonNull NfcAdapter nfcAdapter, @NonNull Context context) {
        return disableNfc(nfcAdapter, context, null);
    }

    static boolean disableNfc(@NonNull NfcAdapter nfcAdapter, @NonNull Context context,
        @Nullable Boolean persist) {
        return setNfcState(nfcAdapter, context, /* setEnabled= */ false, persist);
    }

    private static boolean setNfcState(
            @NonNull NfcAdapter nfcAdapter,
            @NonNull Context context,
            boolean setEnabled,
            @Nullable Boolean disablePersist) {
        if (setEnabled == nfcAdapter.isEnabled()) {
            return true;
        }
        HandlerThread handlerThread = new HandlerThread("nfc_cts_listener");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);

        NfcStateChangeListener nfcChangeListener = new NfcStateChangeListener(setEnabled);
        try {
            context.registerReceiver(nfcChangeListener, intentFilter, null, handler);
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(WRITE_SECURE_SETTINGS);

            if (setEnabled) {
                if (!nfcAdapter.enable()) {
                    Log.e(TAG, "Failed to enable NFC");
                    return false;
                }
            } else {
                if (disablePersist != null) {
                    if (!nfcAdapter.disable(disablePersist)) {
                        Log.e(TAG, "Failed to disable NFC");
                        return false;
                    }
                } else {
                    if (!nfcAdapter.disable()) {
                        Log.e(TAG, "Failed to disable NFC");
                        return false;
                    }
                }
            }

            return nfcChangeListener.awaitStateChange();
        } finally {
            context.unregisterReceiver(nfcChangeListener);
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }
    }

    private static class NfcStateChangeListener extends BroadcastReceiver {
        private final CountDownLatch mCountDownLatch;
        private final boolean mExpectingEnabled;

        NfcStateChangeListener(boolean expectingEnabled) {
            mCountDownLatch = new CountDownLatch(1);
            mExpectingEnabled = expectingEnabled;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int s = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, 0);

            if ((mExpectingEnabled && s == NfcAdapter.STATE_ON)
                    || (!mExpectingEnabled && s == NfcAdapter.STATE_OFF)) {
                mCountDownLatch.countDown();
            }
        }

        public boolean awaitStateChange() {
            try {
                boolean success = mCountDownLatch.await(20, TimeUnit.SECONDS);

                if (!success) {
                    Log.e( TAG, "Timeout waiting for NFC to be "
                                    + (mExpectingEnabled ? "enabled" : "disabled"));
                }

                return success;
            } catch (InterruptedException e) {
                Log.e( TAG, "Interrupted while waiting for NFC to be "
                                + (mExpectingEnabled ? "enabled" : "disabled"));
                return false;
            }
        }
    }

    public static void assumeObserveModeSupported(@NonNull NfcAdapter adapter) {
        assumeTrue("Observe mode must be supported", adapter.isObserveModeSupported());
    }

    public static void assumeVsrApiGreaterThanUdc() {
        assumeTrue("Device VSR API level must be greater than UDC",
            getVsrApiLevel() > Build.VERSION_CODES.UPSIDE_DOWN_CAKE);
    }
}
