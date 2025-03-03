/*
 * Copyright 2021 The Android Open Source Project
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

import static com.android.dx.mockito.inline.extended.ExtendedMockito.doAnswer;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.doCallRealMethod;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.nfc.NfcAdapter.ControllerAlwaysOnListener;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.android.dx.mockito.inline.extended.MockedVoidMethod;
import com.android.modules.utils.testing.ExtendedMockitoRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Test of {@link NfcControllerAlwaysOnListener}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class NfcControllerAlwaysOnListenerTest {

    private INfcAdapter mNfcAdapter = mock(INfcAdapter.class);
    private Throwable mThrowRemoteException = new RemoteException("RemoteException");

    @Rule
    public final ExtendedMockitoRule mExtendedMockitoRule =
            new ExtendedMockitoRule.Builder(this)
                    .mockStatic(NfcAdapter.class)
                    .setStrictness(Strictness.LENIENT)
                    .build();

    private static Executor getExecutor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    private static void verifyListenerInvoked(ControllerAlwaysOnListener listener) {
        verify(listener, times(1)).onControllerAlwaysOnChanged(anyBoolean());
    }

    @Before
    public void setUp() {
        when(NfcAdapter.getService()).thenReturn(mNfcAdapter);
        when(mNfcAdapter.asBinder()).thenReturn(mock(IBinder.class));
        doCallRealMethod().when(
                (MockedVoidMethod)() -> NfcAdapter.callService(any(NfcAdapter.ServiceCall.class)));
        doCallRealMethod().when(
                () -> NfcAdapter.callServiceReturn(any(NfcAdapter.ServiceCallReturn.class), any()));
    }

    @Test
    public void testRegister_RegisterUnregisterWhenNotSupported() throws RemoteException {
        // isControllerAlwaysOnSupported() returns false, not supported.
        doReturn(false).when(mNfcAdapter).isControllerAlwaysOnSupported();
        NfcControllerAlwaysOnListener mListener =
                new NfcControllerAlwaysOnListener();
        ControllerAlwaysOnListener mockListener1 = mock(ControllerAlwaysOnListener.class);
        ControllerAlwaysOnListener mockListener2 = mock(ControllerAlwaysOnListener.class);

        // Verify that the state listener will not registered with the NFC Adapter
        mListener.register(getExecutor(), mockListener1);
        verify(mNfcAdapter, times(0)).registerControllerAlwaysOnListener(any());

        // Register a second client and no any call to NFC Adapter
        mListener.register(getExecutor(), mockListener2);
        verify(mNfcAdapter, times(0)).registerControllerAlwaysOnListener(any());

        // Unregister first listener, and no any call to NFC Adapter
        mListener.unregister(mockListener1);
        verify(mNfcAdapter, times(0)).registerControllerAlwaysOnListener(any());
        verify(mNfcAdapter, times(0)).unregisterControllerAlwaysOnListener(any());

        // Unregister second listener, and no any call to NFC Adapter
        mListener.unregister(mockListener2);
        verify(mNfcAdapter, times(0)).registerControllerAlwaysOnListener(any());
        verify(mNfcAdapter, times(0)).unregisterControllerAlwaysOnListener(any());
    }

    @Test
    public void testRegister_RegisterUnregister() throws RemoteException {
        doReturn(true).when(mNfcAdapter).isControllerAlwaysOnSupported();
        NfcControllerAlwaysOnListener mListener =
                new NfcControllerAlwaysOnListener();
        ControllerAlwaysOnListener mockListener1 = mock(ControllerAlwaysOnListener.class);
        ControllerAlwaysOnListener mockListener2 = mock(ControllerAlwaysOnListener.class);

        // Verify that the state listener registered with the NFC Adapter
        mListener.register(getExecutor(), mockListener1);
        verify(mNfcAdapter, times(1)).registerControllerAlwaysOnListener(any());

        // Register a second client and no new call to NFC Adapter
        mListener.register(getExecutor(), mockListener2);
        verify(mNfcAdapter, times(1)).registerControllerAlwaysOnListener(any());

        // Unregister first listener
        mListener.unregister(mockListener1);
        verify(mNfcAdapter, times(1)).registerControllerAlwaysOnListener(any());
        verify(mNfcAdapter, times(0)).unregisterControllerAlwaysOnListener(any());

        // Unregister second listener and the state listener registered with the NFC Adapter
        mListener.unregister(mockListener2);
        verify(mNfcAdapter, times(1)).registerControllerAlwaysOnListener(any());
        verify(mNfcAdapter, times(1)).unregisterControllerAlwaysOnListener(any());
    }

    @Test
    public void testRegister_FirstRegisterFails() throws RemoteException {
        doReturn(true).when(mNfcAdapter).isControllerAlwaysOnSupported();
        NfcControllerAlwaysOnListener mListener =
                new NfcControllerAlwaysOnListener();
        ControllerAlwaysOnListener mockListener1 = mock(ControllerAlwaysOnListener.class);
        ControllerAlwaysOnListener mockListener2 = mock(ControllerAlwaysOnListener.class);

        // Throw a remote exception whenever first registering
        doThrow(mThrowRemoteException).when(mNfcAdapter).registerControllerAlwaysOnListener(
                any());

        try {
            mListener.register(getExecutor(), mockListener1);
            fail();
        } catch (RuntimeException e) {
            // pass
        }
        // callServiceReturn retries on seeing remote exception.
        verify(mNfcAdapter, times(2)).registerControllerAlwaysOnListener(any());

        // No longer throw an exception, instead succeed
        doNothing().when(mNfcAdapter).registerControllerAlwaysOnListener(any());

        // Register a different listener
        mListener.register(getExecutor(), mockListener2);
        verify(mNfcAdapter, times(3)).registerControllerAlwaysOnListener(any());

        // Ensure first and second listener were invoked
        mListener.onControllerAlwaysOnChanged(true);
        verifyListenerInvoked(mockListener1);
        verifyListenerInvoked(mockListener2);
    }

    @Test
    public void testRegister_RegisterSameListenerTwice() throws RemoteException {
        doReturn(true).when(mNfcAdapter).isControllerAlwaysOnSupported();
        NfcControllerAlwaysOnListener mListener =
                new NfcControllerAlwaysOnListener();
        ControllerAlwaysOnListener mockListener = mock(ControllerAlwaysOnListener.class);

        // Register the same listener Twice
        mListener.register(getExecutor(), mockListener);
        mListener.register(getExecutor(), mockListener);
        verify(mNfcAdapter, times(1)).registerControllerAlwaysOnListener(any());

        // Invoke a state change and ensure the listener is only called once
        mListener.onControllerAlwaysOnChanged(true);
        verifyListenerInvoked(mockListener);
    }

    @Test
    public void testNotify_AllListenersNotified() throws RemoteException {
        doReturn(true).when(mNfcAdapter).isControllerAlwaysOnSupported();
        NfcControllerAlwaysOnListener listener = new NfcControllerAlwaysOnListener();
        List<ControllerAlwaysOnListener> mockListeners = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ControllerAlwaysOnListener mockListener = mock(ControllerAlwaysOnListener.class);
            listener.register(getExecutor(), mockListener);
            mockListeners.add(mockListener);
        }

        // Invoke a state change and ensure all listeners are invoked
        listener.onControllerAlwaysOnChanged(true);
        for (ControllerAlwaysOnListener mListener : mockListeners) {
            verifyListenerInvoked(mListener);
        }
    }

    @Test
    public void testStateChange_CorrectValue() throws RemoteException {
        doReturn(true).when(mNfcAdapter).isControllerAlwaysOnSupported();
        runStateChangeValue(true, true);
        runStateChangeValue(false, false);

    }

    private void runStateChangeValue(boolean isEnabledIn, boolean isEnabledOut) {
        NfcControllerAlwaysOnListener listener = new NfcControllerAlwaysOnListener();
        ControllerAlwaysOnListener mockListener = mock(ControllerAlwaysOnListener.class);
        listener.register(getExecutor(), mockListener);
        listener.onControllerAlwaysOnChanged(isEnabledIn);
        verify(mockListener, times(1)).onControllerAlwaysOnChanged(isEnabledOut);
        verify(mockListener, times(0)).onControllerAlwaysOnChanged(!isEnabledOut);
    }
}
