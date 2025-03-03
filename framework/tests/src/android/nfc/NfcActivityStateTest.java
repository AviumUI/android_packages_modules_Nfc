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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.FieldSetter;

public class NfcActivityStateTest {
    @Mock
    private Activity mActivity;
    @Mock
    private NfcActivityManager mNfcActivityManager;
    @Mock
    private Application mApp;
    @Mock
    private NfcAdapter.ReaderCallback mReaderCallback;
    private NfcActivityState mNfcActivityState;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mActivity.isDestroyed()).thenReturn(false);
        when(mActivity.isResumed()).thenReturn(true);
        when(mActivity.getApplication()).thenReturn(mApp);

        mNfcActivityState = new NfcActivityState(mActivity, mNfcActivityManager);
    }

    @Test(expected = IllegalStateException.class)
    public void testNfcActivityStateWithActivityDestroyed() {
        when(mActivity.isDestroyed()).thenReturn(true);

        mNfcActivityState = new NfcActivityState(mActivity, mNfcActivityManager);
    }

    @Test
    public void testToString() {
        try {
            FieldSetter.setField(mNfcActivityState,
                    mNfcActivityState.getClass().getDeclaredField("readerCallback"),
                    mReaderCallback);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        }

        assertEquals("[" + mReaderCallback + "]", mNfcActivityState.toString());
    }
}
