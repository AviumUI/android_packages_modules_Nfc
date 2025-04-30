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
package com.android.nfc.emulator;

import android.content.ComponentName;
import android.content.Intent;
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;

import com.android.nfc.service.OffHostService;
import com.android.nfc.service.PollingLoopService;

public class OffHostEmulatorActivity extends BaseEmulatorActivity {
    public static final String EXTRA_ENABLE_OBSERVE_MODE = "EXTRA_ENABLE_OBSERVE_MODE";

    private CardEmulation.NfcEventCallback mEventListener = new CardEmulation.NfcEventCallback() {
        @Override
        public void onOffHostAidSelected(String aid, String offHostSe) {
            Intent intent = new Intent(BaseEmulatorActivity.ACTION_OFFHOST_AID_SELECTED);
            intent.putExtra(EXTRA_OFFHOST_AID_SELECTED_AID, aid);
            intent.putExtra(EXTRA_OFFHOST_AID_SELECTED_SE, offHostSe);
            sendBroadcast(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupServices(OffHostService.COMPONENT, PollingLoopService.COMPONENT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerEventListener(mEventListener);
        if (getIntent().getBooleanExtra(EXTRA_ENABLE_OBSERVE_MODE, false)) {
            // Still need to set a preferred service to be able to set observe mode.
            mCardEmulation.setPreferredService(
                    this, PollingLoopService.COMPONENT);
            mAdapter.setObserveModeEnabled(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mCardEmulation.unregisterNfcEventCallback(mEventListener);
        if (getIntent().getBooleanExtra(EXTRA_ENABLE_OBSERVE_MODE, false)) {
            mCardEmulation.unsetPreferredService(this);
            mAdapter.setObserveModeEnabled(false);
        }
    }

    @Override
    public ComponentName getPreferredServiceComponent() {
        return PollingLoopService.COMPONENT;
    }
}
