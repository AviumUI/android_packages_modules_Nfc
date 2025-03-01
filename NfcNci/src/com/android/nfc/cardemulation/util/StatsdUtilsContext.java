/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.nfc.cardemulation.util;

import android.annotation.FlaggedApi;
import android.annotation.Nullable;
import com.android.nfc.flags.Flags;

/** Shared context for all instances of StatsdUtils. */
@FlaggedApi(Flags.FLAG_STATSD_CE_EVENTS_FLAG)
public class StatsdUtilsContext {
    public static final String TAG = "StatsdUtilsContext";

    /** The trigger source of the next observe mode state change, if known */
    @Nullable
    private Integer mObserveModeTriggerSource = null;

    public StatsdUtilsContext() {}

    public void setObserveModeTriggerSource(@Nullable Integer triggerSource) {
        mObserveModeTriggerSource = triggerSource;
    }

    @Nullable
    public Integer getObserveModeTriggerSource() {
        return mObserveModeTriggerSource;
    }
}
