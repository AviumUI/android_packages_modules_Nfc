/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.nfc.cardemulation;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.nfc.ComponentNameAndUser;
import android.nfc.INfcOemExtensionCallback;
import android.nfc.cardemulation.ApduServiceInfo;
import android.nfc.cardemulation.PollingFrame;
import android.util.proto.ProtoOutputStream;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

/** Base class to help refactor HEM implementation using flags. */
public interface HostEmulationManagerBase {

    /** */
    void setOemExtension(@Nullable INfcOemExtensionCallback nfcOemExtensionCallback);

    /** */
    void onBootCompleted();

    /** */
    void onPreferredPaymentServiceChanged(ComponentNameAndUser service);

    /** */
    void updateForShouldDefaultToObserveMode(boolean enabled);

    /** */
    void updatePollingLoopFilters(@UserIdInt int userId, List<ApduServiceInfo> services);

    /** */
    void onObserveModeStateChange(boolean enabled);

    /** */
    void onPollingLoopDetected(List<PollingFrame> pollingFrames);

    /** */
    void onObserveModeDisabledInFirmware(PollingFrame exitFrame);

    /** */
    void onPreferredForegroundServiceChanged(ComponentNameAndUser serviceAndUser);

    /** */
    void onFieldChangeDetected(boolean fieldOn);

    /** */
    void onHostEmulationActivated();

    /** */
    void onHostEmulationData(byte[] data);

    /** */
    void onHostEmulationDeactivated();

    /** */
    boolean isHostCardEmulationActivated();

    /** */
    void onOffHostAidSelected();

    /** */
    @FlaggedApi(android.nfc.Flags.FLAG_NFC_EVENT_LISTENER)
    interface NfcAidRoutingListener {
        /** */
        void onAidConflict(@NonNull String aid);

        /** */
        void onAidNotRouted(@NonNull String aid);
    }

    /** */
    void setAidRoutingListener(@Nullable NfcAidRoutingListener listener);

    /** */
    void dump(FileDescriptor fd, PrintWriter pw, String[] args);

    /** */
    void dumpDebug(ProtoOutputStream proto);
}
