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

package android.nfc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Application;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NfcApplicationStateTest {
    @Mock
    private NfcActivityManager mNfcActivityManager;
    @Mock
    private Application mApp;
    private NfcApplicationState mNfcApplicationState;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mNfcApplicationState = new NfcApplicationState(mApp, mNfcActivityManager);
    }

    @Test
    public void testRegister() {
        mNfcApplicationState.register();

        verify(mApp, times(1)).registerActivityLifecycleCallbacks(mNfcActivityManager);
    }

    @Test
    public void testMultipleRegister() {
        mNfcApplicationState.register();
        mNfcApplicationState.register();

        verify(mApp, times(1)).registerActivityLifecycleCallbacks(mNfcActivityManager);
    }

    @Test
    public void testUnregister() {
        mNfcApplicationState.register();
        mNfcApplicationState.unregister();

        verify(mApp, times(1)).unregisterActivityLifecycleCallbacks(mNfcActivityManager);
    }

    @Test
    public void testMultipleUnregister() {
        mNfcApplicationState.register();
        mNfcApplicationState.unregister();
        mNfcApplicationState.unregister();

        verify(mApp, times(1)).unregisterActivityLifecycleCallbacks(mNfcActivityManager);
    }

    @Test
    public void testUnregisterWithoutRegister() {
        mNfcApplicationState.unregister();

        verify(mApp, never()).unregisterActivityLifecycleCallbacks(mNfcActivityManager);
    }
}
