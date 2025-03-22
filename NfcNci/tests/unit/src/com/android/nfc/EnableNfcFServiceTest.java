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

package com.android.nfc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.nfc.cardemulation.NfcFServiceInfo;
import android.os.UserHandle;
import android.util.proto.ProtoOutputStream;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.nfc.cardemulation.EnabledNfcFServices;
import com.android.nfc.cardemulation.EnabledNfcFServicesProto;
import com.android.nfc.cardemulation.RegisteredNfcFServicesCache;
import com.android.nfc.cardemulation.RegisteredT3tIdentifiersCache;
import com.android.nfc.cardemulation.RoutingOptionManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Field;

@RunWith(AndroidJUnit4.class)
public class EnableNfcFServiceTest {

    private static final String TAG = EnableNfcFServiceTest.class.getSimpleName();
    private MockitoSession mStaticMockSession;
    private ComponentName mComponentName;
    private NfcFServiceInfo mNfcFServiceInfo;
    private EnabledNfcFServices mEnabledNfcFServices;
    private ForegroundUtils mForegroundUtils;
    private EnabledNfcFServices.Callback mCallback;

    @Before
    public void setUp() throws Exception {
        mStaticMockSession = ExtendedMockito.mockitoSession()
                .mockStatic(RoutingOptionManager.class)
                .mockStatic(NfcService.class)
                .mockStatic(NfcStatsLog.class)
                .mockStatic(UserHandle.class)
                .mockStatic(ForegroundUtils.class)
                .strictness(Strictness.LENIENT)
                .startMocking();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context mockContext = new ContextWrapper(context) {
        };

        mForegroundUtils = mock(ForegroundUtils.class);
        when(ForegroundUtils.getInstance(
                mockContext.getSystemService(ActivityManager.class))).thenReturn(mForegroundUtils);
        RegisteredNfcFServicesCache registeredNfcFServicesCache = mock(
                RegisteredNfcFServicesCache.class);
        mComponentName = mock(ComponentName.class);
        mNfcFServiceInfo = mock(NfcFServiceInfo.class);
        when(registeredNfcFServicesCache.getService(1, mComponentName)).thenReturn(
                mNfcFServiceInfo);
        RegisteredT3tIdentifiersCache registeredT3tIdentifiersCache = mock(
                RegisteredT3tIdentifiersCache.class);

        RoutingOptionManager routingOptionManager = mock(RoutingOptionManager.class);
        when(RoutingOptionManager.getInstance()).thenReturn(routingOptionManager);

        mCallback = mock(EnabledNfcFServices.Callback.class);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> mEnabledNfcFServices = new EnabledNfcFServices(mockContext,
                        registeredNfcFServicesCache, registeredT3tIdentifiersCache,
                        mCallback));
        Assert.assertNotNull(mEnabledNfcFServices);
    }

    @After
    public void tearDown() throws Exception {
        mStaticMockSession.finishMocking();
    }

    @Test
    public void testOnHostEmulationActivated() {
        boolean isActivated = mEnabledNfcFServices.isActivated();
        Assert.assertFalse(isActivated);
        mEnabledNfcFServices.onHostEmulationActivated();
        isActivated = mEnabledNfcFServices.isActivated();
        assertTrue(isActivated);
    }

    @Test
    public void testOnHostEmulationDeactivated() {
        mEnabledNfcFServices.onHostEmulationActivated();
        boolean isActivated = mEnabledNfcFServices.isActivated();
        assertTrue(isActivated);
        mEnabledNfcFServices.onHostEmulationDeactivated();
        isActivated = mEnabledNfcFServices.isActivated();
        Assert.assertFalse(isActivated);
    }

    @Test
    public void testRegisterEnabledForegroundService() {
        UserHandle userHandle = mock(UserHandle.class);
        when(userHandle.getIdentifier()).thenReturn(1);
        when(UserHandle.getUserHandleForUid(1)).thenReturn(userHandle);
        when(mNfcFServiceInfo.getSystemCode()).thenReturn("Nfc");
        when(mNfcFServiceInfo.getNfcid2()).thenReturn("NfcId");
        when(mNfcFServiceInfo.getT3tPmm()).thenReturn("T3");
        when(mForegroundUtils.registerUidToBackgroundCallback(mEnabledNfcFServices, 1)).thenReturn(
                true);
        boolean isRegistered = mEnabledNfcFServices.registerEnabledForegroundService(mComponentName,
                1);
        assertTrue(isRegistered);
    }


    @Test
    public void testOnNfcDisabled() {
        mEnabledNfcFServices.onNfcDisabled();
        boolean isNfcDisabled = mEnabledNfcFServices.isNfcDisabled();
        assertTrue(isNfcDisabled);
    }

    @Test
    public void testOnUserSwitched() {
        mEnabledNfcFServices.onUserSwitched(0);
        boolean isUserSwitched = mEnabledNfcFServices.isUserSwitched();
        assertTrue(isUserSwitched);

    }

    @Test
    public void testDumpDebug() throws NoSuchFieldException, IllegalAccessException {
        ProtoOutputStream proto = mock(ProtoOutputStream.class);
        ComponentName mForegroundComponent = mock(ComponentName.class);
        ComponentName mForegroundRequested = mock(ComponentName.class);
        Field fieldForegroundComp = EnabledNfcFServices.class.getDeclaredField(
                "mForegroundComponent");
        fieldForegroundComp.setAccessible(true);
        fieldForegroundComp.set(mEnabledNfcFServices, mForegroundComponent);
        Field fieldForegroundReq = EnabledNfcFServices.class.getDeclaredField(
                "mForegroundRequested");
        fieldForegroundReq.setAccessible(true);
        fieldForegroundReq.set(mEnabledNfcFServices, mForegroundRequested);
        mEnabledNfcFServices.dump(mock(FileDescriptor.class), mock(PrintWriter.class),
                new String[]{});

        mEnabledNfcFServices.dumpDebug(proto);
        verify(proto).write(eq(EnabledNfcFServicesProto.ACTIVATED), anyBoolean());
    }

    @Test
    public void testUnRegisterForegroundService()
            throws NoSuchFieldException, IllegalAccessException {
        int mForegroundUid = 1;
        int userId = 1;
        ComponentName mForegroundComponent = mock(ComponentName.class);
        ComponentName mForegroundRequested = mock(ComponentName.class);
        UserHandle userHandle = mock(UserHandle.class);
        Field fieldForegroundUid = EnabledNfcFServices.class.getDeclaredField(
                "mForegroundUid");
        fieldForegroundUid.setAccessible(true);
        fieldForegroundUid.set(mEnabledNfcFServices, mForegroundUid);
        Field fieldForegroundComp = EnabledNfcFServices.class.getDeclaredField(
                "mForegroundComponent");
        fieldForegroundComp.setAccessible(true);
        fieldForegroundComp.set(mEnabledNfcFServices, mForegroundComponent);
        Field fieldForegroundReq = EnabledNfcFServices.class.getDeclaredField(
                "mForegroundRequested");
        fieldForegroundReq.setAccessible(true);
        fieldForegroundReq.set(mEnabledNfcFServices, mForegroundRequested);
        when(UserHandle.getUserHandleForUid(anyInt())).thenReturn(userHandle);
        when(userHandle.getIdentifier()).thenReturn(userId);

        mEnabledNfcFServices.onUidToBackground(mForegroundUid);
        verify(mCallback).onEnabledForegroundNfcFServiceChanged(userId, null);
        verify(userHandle).getIdentifier();
    }

    @Test
    public void testUnregisteredEnabledForegroundService()
            throws NoSuchFieldException, IllegalAccessException {
        int callingId = 1;
        int mForegroundUid = 1;
        int userId = 1;
        ComponentName mForegroundComponent = mock(ComponentName.class);
        ComponentName mForegroundRequested = mock(ComponentName.class);
        UserHandle userHandle = mock(UserHandle.class);
        Field fieldForegroundUid = EnabledNfcFServices.class.getDeclaredField(
                "mForegroundUid");
        fieldForegroundUid.setAccessible(true);
        fieldForegroundUid.set(mEnabledNfcFServices, mForegroundUid);
        Field fieldForegroundComp = EnabledNfcFServices.class.getDeclaredField(
                "mForegroundComponent");
        fieldForegroundComp.setAccessible(true);
        fieldForegroundComp.set(mEnabledNfcFServices, mForegroundComponent);
        Field fieldForegroundReq = EnabledNfcFServices.class.getDeclaredField(
                "mForegroundRequested");
        fieldForegroundReq.setAccessible(true);
        fieldForegroundReq.set(mEnabledNfcFServices, mForegroundRequested);
        when(UserHandle.getUserHandleForUid(anyInt())).thenReturn(userHandle);
        when(userHandle.getIdentifier()).thenReturn(userId);
        when(mForegroundUtils.isInForeground(callingId)).thenReturn(true);

        assertTrue(mEnabledNfcFServices.unregisteredEnabledForegroundService(callingId));
        verify(mCallback).onEnabledForegroundNfcFServiceChanged(userId, null);
        verify(mForegroundUtils).isInForeground(callingId);
    }

    @Test
    public void testUnregisteredEnabledForegroundServiceNonForegroundUid() {
        int callingId = 1;
        when(mForegroundUtils.isInForeground(callingId)).thenReturn(false);

        assertFalse(mEnabledNfcFServices.unregisteredEnabledForegroundService(callingId));
        verify(mForegroundUtils).isInForeground(callingId);
    }

    @Test
    public void testOnServiceUpdate() throws NoSuchFieldException, IllegalAccessException {
        int userId = 1;
        ComponentName mForegroundComponent = mock(ComponentName.class);
        ComponentName mForegroundRequested = mock(ComponentName.class);
        UserHandle userHandle = mock(UserHandle.class);
        Field fieldForegroundComp = EnabledNfcFServices.class.getDeclaredField(
                "mForegroundComponent");
        fieldForegroundComp.setAccessible(true);
        fieldForegroundComp.set(mEnabledNfcFServices, mForegroundComponent);
        Field fieldForegroundReq = EnabledNfcFServices.class.getDeclaredField(
                "mForegroundRequested");
        fieldForegroundReq.setAccessible(true);
        fieldForegroundReq.set(mEnabledNfcFServices, mForegroundRequested);
        when(UserHandle.getUserHandleForUid(anyInt())).thenReturn(userHandle);
        when(userHandle.getIdentifier()).thenReturn(userId);

        mEnabledNfcFServices.onServicesUpdated();
        verify(mCallback).onEnabledForegroundNfcFServiceChanged(userId, null);
        verify(userHandle).getIdentifier();
    }

    @Test
    public void testOnHostEmulationDeactivatedWithPostponedConfiguration()
            throws NoSuchFieldException, IllegalAccessException {
        boolean mComputeFgRequested = true;
        int userId = 1;
        ComponentName mForegroundRequested = mock(ComponentName.class);
        UserHandle userHandle = mock(UserHandle.class);
        Field fieldForegroundUid = EnabledNfcFServices.class.getDeclaredField(
                "mComputeFgRequested");
        fieldForegroundUid.setAccessible(true);
        fieldForegroundUid.set(mEnabledNfcFServices, mComputeFgRequested);
        Field fieldForegroundComp = EnabledNfcFServices.class.getDeclaredField(
                "mForegroundComponent");
        fieldForegroundComp.setAccessible(true);
        fieldForegroundComp.set(mEnabledNfcFServices, null);
        Field fieldForegroundReq = EnabledNfcFServices.class.getDeclaredField(
                "mForegroundRequested");
        fieldForegroundReq.setAccessible(true);
        fieldForegroundReq.set(mEnabledNfcFServices, mForegroundRequested);
        when(UserHandle.getUserHandleForUid(anyInt())).thenReturn(userHandle);
        when(userHandle.getIdentifier()).thenReturn(userId);

        mEnabledNfcFServices.onHostEmulationDeactivated();
        verify(mCallback).onEnabledForegroundNfcFServiceChanged(userId, mForegroundRequested);
        verify(userHandle).getIdentifier();
        verify(mForegroundRequested).equals(null);
    }

}
