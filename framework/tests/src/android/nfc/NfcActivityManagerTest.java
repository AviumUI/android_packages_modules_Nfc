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
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.Application;
import android.os.RemoteException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.FieldSetter;

import java.util.ArrayList;
import java.util.List;

public class NfcActivityManagerTest {
    private NfcActivityManager mNfcActivityManager;
    @Mock
    private NfcAdapter mNfcAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mNfcActivityManager = new NfcActivityManager(mNfcAdapter);
    }

    @Test
    public void testFindResumedActivityState() {
        NfcActivityState nfcActivityState = mock(NfcActivityState.class);
        List<NfcActivityState> mActivities = new ArrayList<>();
        mActivities.add(nfcActivityState);
        try {
            FieldSetter.setField(mNfcActivityManager,
                    mNfcActivityManager.getClass().getDeclaredField("mActivities"),
                    mActivities);
            FieldSetter.setField(nfcActivityState,
                    nfcActivityState.getClass().getDeclaredField("resumed"),
                    true);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        }

        assertEquals(nfcActivityState, mNfcActivityManager.findResumedActivityState());
    }

    @Test
    public void testFindResumedActivityStateWithNonResumeState() {
        NfcActivityState nfcActivityState = mock(NfcActivityState.class);
        List<NfcActivityState> mActivities = new ArrayList<>();
        mActivities.add(nfcActivityState);
        try {
            FieldSetter.setField(mNfcActivityManager,
                    mNfcActivityManager.getClass().getDeclaredField("mActivities"),
                    mActivities);
            FieldSetter.setField(nfcActivityState,
                    nfcActivityState.getClass().getDeclaredField("resumed"),
                    false);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        }

        assertNull(mNfcActivityManager.findResumedActivityState());
    }

    @Test
    public void testOnTagDiscovered() throws RemoteException {
        Tag tag = mock(Tag.class);
        NfcAdapter.ReaderCallback readerCallback = mock(NfcAdapter.ReaderCallback.class);
        NfcActivityState nfcActivityState = mock(NfcActivityState.class);
        List<NfcActivityState> mActivities = new ArrayList<>();
        mActivities.add(nfcActivityState);
        try {
            FieldSetter.setField(mNfcActivityManager,
                    mNfcActivityManager.getClass().getDeclaredField("mActivities"),
                    mActivities);
            FieldSetter.setField(nfcActivityState,
                    nfcActivityState.getClass().getDeclaredField("resumed"),
                    true);
            FieldSetter.setField(nfcActivityState,
                    nfcActivityState.getClass().getDeclaredField("readerCallback"),
                    readerCallback);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        }

        mNfcActivityManager.onTagDiscovered(tag);
        verify(readerCallback).onTagDiscovered(tag);
    }

    @Test
    public void testOnTagDiscoveredWithNullCallback() throws RemoteException {
        Tag tag = mock(Tag.class);
        NfcAdapter.ReaderCallback readerCallback = mock(NfcAdapter.ReaderCallback.class);
        NfcActivityState nfcActivityState = mock(NfcActivityState.class);
        List<NfcActivityState> mActivities = new ArrayList<>();
        mActivities.add(nfcActivityState);
        try {
            FieldSetter.setField(mNfcActivityManager,
                    mNfcActivityManager.getClass().getDeclaredField("mActivities"),
                    mActivities);
            FieldSetter.setField(nfcActivityState,
                    nfcActivityState.getClass().getDeclaredField("resumed"),
                    true);
            FieldSetter.setField(nfcActivityState,
                    nfcActivityState.getClass().getDeclaredField("readerCallback"),
                    null);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        }

        mNfcActivityManager.onTagDiscovered(tag);
        verify(readerCallback, never()).onTagDiscovered(tag);
    }

    @Test
    public void testUnregisterApplication() {
        Application app = mock(Application.class);
        NfcApplicationState nfcApplicationState = mock(NfcApplicationState.class);
        List<NfcApplicationState> mApps = new ArrayList<>();
        mApps.add(nfcApplicationState);
        try {
            FieldSetter.setField(mNfcActivityManager,
                    mNfcActivityManager.getClass().getDeclaredField("mApps"),
                    mApps);
            FieldSetter.setField(nfcApplicationState,
                    nfcApplicationState.getClass().getDeclaredField("app"),
                    app);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        }

        mNfcActivityManager.unregisterApplication(app);
        verify(nfcApplicationState).unregister();
    }

    @Test
    public void testUnregisterApplicationWithNoApp() {
        Application app = mock(Application.class);
        NfcApplicationState nfcApplicationState = mock(NfcApplicationState.class);
        List<NfcApplicationState> mApps = new ArrayList<>();
        mApps.add(nfcApplicationState);
        try {
            FieldSetter.setField(mNfcActivityManager,
                    mNfcActivityManager.getClass().getDeclaredField("mApps"),
                    mApps);
            FieldSetter.setField(nfcApplicationState,
                    nfcApplicationState.getClass().getDeclaredField("app"),
                    null);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        }

        mNfcActivityManager.unregisterApplication(app);
        verify(nfcApplicationState, never()).unregister();
    }

    @Test
    public void testRegisterApplication() {
        Application app = mock(Application.class);
        NfcApplicationState nfcApplicationState = mock(NfcApplicationState.class);
        List<NfcApplicationState> mApps = new ArrayList<>();
        mApps.add(nfcApplicationState);
        try {
            FieldSetter.setField(mNfcActivityManager,
                    mNfcActivityManager.getClass().getDeclaredField("mApps"),
                    mApps);
            FieldSetter.setField(nfcApplicationState,
                    nfcApplicationState.getClass().getDeclaredField("app"),
                    app);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        }

        mNfcActivityManager.registerApplication(app);
        verify(nfcApplicationState).register();
    }
}
