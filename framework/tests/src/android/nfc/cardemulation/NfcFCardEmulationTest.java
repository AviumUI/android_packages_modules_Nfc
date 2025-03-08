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

package android.nfc.cardemulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.INfcFCardEmulation;
import android.nfc.NfcAdapter;
import android.os.RemoteException;
import android.os.UserHandle;

import com.android.dx.mockito.inline.extended.ExtendedMockito;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

public class NfcFCardEmulationTest {
    @Mock
    private NfcAdapter mNfcAdapter;
    @Mock
    private Context mContext;
    @Mock
    private INfcFCardEmulation mService;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ComponentName mComponentName;
    @Mock
    private Activity mActivity;
    private MockitoSession mStaticMockSession;
    private NfcFCardEmulation mNfcFCardEmulation;

    @Before
    public void setUp() throws NoSuchFieldException {
        mStaticMockSession = ExtendedMockito.mockitoSession()
                .mockStatic(NfcAdapter.class)
                .strictness(Strictness.LENIENT)
                .startMocking();
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws NoSuchFieldException {
        mStaticMockSession.finishMocking();
    }

    private NfcFCardEmulation createEmulationInstance()
            throws NoSuchFieldException {
        when(mPackageManager.hasSystemFeature(
                PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF))
                .thenReturn(true);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mNfcAdapter.getContext()).thenReturn(mContext);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(mService);
        mNfcFCardEmulation = NfcFCardEmulation.getInstance(mNfcAdapter);
        FieldSetter.setField(
                mNfcFCardEmulation,
                mNfcFCardEmulation.getClass().getDeclaredField("sService"), mService);
        FieldSetter.setField(
                mNfcFCardEmulation,
                mNfcFCardEmulation.getClass().getDeclaredField("mContext"), mContext);
        return mNfcFCardEmulation;
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetInstanceUnsupportedOperationExceptionForContext() {
        when(mNfcAdapter.getContext()).thenReturn(null);
        NfcFCardEmulation.getInstance(mNfcAdapter);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetInstanceUnsupportedOperationExceptionForPackageManagers() {
        when(mContext.getPackageManager()).thenReturn(null);
        when(mNfcAdapter.getContext()).thenReturn(mContext);
        NfcFCardEmulation.getInstance(mNfcAdapter);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void
    testGetInstanceUnsupportedOperationExceptionForPackageManagersFeatureDisable() {
        when(mPackageManager.hasSystemFeature(
                PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF))
                .thenReturn(false);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mNfcAdapter.getContext()).thenReturn(mContext);
        NfcFCardEmulation.getInstance(mNfcAdapter);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetInstanceUnsupportedOperationExceptionForServices() {
        when(mPackageManager.hasSystemFeature(
                PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF))
                .thenReturn(true);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mNfcAdapter.getContext()).thenReturn(mContext);
        when(NfcAdapter.getNfcFCardEmulationService()).thenReturn(null);
        NfcFCardEmulation.getInstance(mNfcAdapter);
    }

    @Test
    public void testGetInstance() throws NoSuchFieldException {
        mNfcFCardEmulation = createEmulationInstance();
        assertNotNull(mNfcFCardEmulation);
    }

    @Test(expected = NullPointerException.class)
    public void testGetSystemCodeForServiceWithNullService()
            throws NoSuchFieldException {
        mNfcFCardEmulation = createEmulationInstance();
        mNfcFCardEmulation.getSystemCodeForService(null);
    }

    @Test(expected = RuntimeException.class)
    public void testGetSystemCodeForServiceRecoveryRemoteException()
            throws RemoteException {
        when(mService.getSystemCodeForService(anyInt(), eq(mComponentName)))
                .thenThrow(new RemoteException());
        INfcFCardEmulation recoveredService = mock(INfcFCardEmulation.class);
        when(mNfcAdapter.getNfcFCardEmulationService())
                .thenReturn(recoveredService);
        when(recoveredService.getSystemCodeForService(anyInt(), eq(mComponentName)))
                .thenThrow(new RemoteException());

        mNfcFCardEmulation.getSystemCodeForService(mComponentName);
    }

    @Test(expected = RuntimeException.class)
    public void testGetSystemCodeForServiceWithRecoveryFail()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandles = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandles);
        when(mockUserHandles.getIdentifier()).thenReturn(1);
        when(mService.getSystemCodeForService(1, mComponentName))
                .thenThrow(new RemoteException());
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(NfcAdapter.getNfcFCardEmulationService()).thenReturn(null);
        mNfcFCardEmulation.getSystemCodeForService(mComponentName);
    }

    @Test
    public void testIsValidSystemCodeValidCode() {
        assertTrue(NfcFCardEmulation.isValidSystemCode("4001"));
        assertTrue(NfcFCardEmulation.isValidSystemCode("4ABC"));
    }

    @Test
    public void testIsValidSystemCodeNullCode() {
        assertFalse(NfcFCardEmulation.isValidSystemCode(null));
    }

    @Test
    public void testIsValidSystemCodeInvalidLength() {
        assertFalse(NfcFCardEmulation.isValidSystemCode("40"));
        assertFalse(NfcFCardEmulation.isValidSystemCode("400123"));
    }

    @Test
    public void testIsValidSystemCodeInvalidRange() {
        assertFalse(NfcFCardEmulation.isValidSystemCode("3FFF"));
        assertFalse(NfcFCardEmulation.isValidSystemCode("4FFF"));
        assertFalse(NfcFCardEmulation.isValidSystemCode("4ffF"));
    }

    @Test
    public void testIsValidSystemCode_InvalidCharacters() {
        assertFalse(NfcFCardEmulation.isValidSystemCode("4GHI"));
    }

    @Test
    public void testIsValidNfcid2ValidNfcid2() {
        assertTrue(NfcFCardEmulation.isValidNfcid2("02FE1234567890AB"));
    }

    @Test
    public void testIsValidNfcid2NullNfcid2() {
        assertFalse(NfcFCardEmulation.isValidNfcid2(null));
    }

    @Test
    public void testIsValidNfcid2InvalidLength() {
        assertFalse(NfcFCardEmulation.isValidNfcid2("02FE1234"));
        assertFalse(NfcFCardEmulation.isValidNfcid2("02FE1234567890ABCDE"));
    }

    @Test
    public void testIsValidNfcid2InvalidPrefix() {
        assertFalse(NfcFCardEmulation.isValidNfcid2("01FE1234567890AB"));
        assertFalse(NfcFCardEmulation.isValidNfcid2("02ff1234567890AB"));
    }

    @Test
    public void testIsValidNfcid2InvalidCharacters() {
        assertFalse(NfcFCardEmulation.isValidNfcid2("02FE1234567890GZ"));
    }

    @Test
    public void testGetMaxNumOfRegisterableSystemCodesSuccess()
            throws RemoteException, NoSuchFieldException {
        mNfcFCardEmulation = createEmulationInstance();
        when(mService.getMaxNumOfRegisterableSystemCodes()).thenReturn(1);

        assertEquals(1, mNfcFCardEmulation.getMaxNumOfRegisterableSystemCodes());
    }

    @Test(expected = NullPointerException.class)
    public void testGetMaxNumOfRegisterableSystemCodesServiceIsNull()
            throws RemoteException, NoSuchFieldException {
        mNfcFCardEmulation = createEmulationInstance();
        when(mService.getMaxNumOfRegisterableSystemCodes())
                .thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(null);
        mNfcFCardEmulation.getMaxNumOfRegisterableSystemCodes();
    }

    @Test(expected = RemoteException.class)
    public void testGetMaxNumOfRegisterableSystemCodesWithException()
            throws RemoteException, NoSuchFieldException {
        mNfcFCardEmulation = createEmulationInstance();
        when(mService.getMaxNumOfRegisterableSystemCodes())
                .thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(mService);
        when(mService.getMaxNumOfRegisterableSystemCodes())
                .thenThrow(RemoteException.class);

        mNfcFCardEmulation.getMaxNumOfRegisterableSystemCodes();
    }

    @Test
    public void testGetNfcFServicesSuccess()
            throws NoSuchFieldException, RemoteException {
        List<NfcFServiceInfo> nfcFServiceInfoList = new ArrayList<>();
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandles = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandles);
        when(mockUserHandles.getIdentifier()).thenReturn(1);
        when(mService.getNfcFServices(1)).thenReturn(nfcFServiceInfoList);

        assertEquals(nfcFServiceInfoList, mNfcFCardEmulation.getNfcFServices());
    }

    @Test(expected = NullPointerException.class)
    public void testGetNfcFServiceWithNullServices()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandles = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandles);
        when(mockUserHandles.getIdentifier()).thenReturn(1);
        when(mService.getNfcFServices(1)).thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(null);

        mNfcFCardEmulation.getNfcFServices();
    }

    @Test
    public void testGetNfcFServiceWithSecondTry()
            throws NoSuchFieldException, RemoteException {
        List<NfcFServiceInfo> nfcFServiceInfoList = new ArrayList<>();
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandles = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandles);
        when(mockUserHandles.getIdentifier()).thenReturn(1);
        when(mService.getNfcFServices(1))
                .thenThrow(RemoteException.class)
                .thenReturn(nfcFServiceInfoList);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);

        assertEquals(nfcFServiceInfoList, mNfcFCardEmulation.getNfcFServices());
    }

    @Test
    public void testGetNfcFServiceWithSecondTryFailure()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandles = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandles);
        when(mockUserHandles.getIdentifier()).thenReturn(1);
        when(mService.getNfcFServices(1))
                .thenThrow(RemoteException.class)
                .thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);

        assertNull(mNfcFCardEmulation.getNfcFServices());
    }

    @Test
    public void testDisableServiceSuccess()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        when(mActivity.isResumed()).thenReturn(true);
        when(mService.disableNfcFForegroundService()).thenReturn(true);

        assertTrue(mNfcFCardEmulation.disableService(mActivity));
    }

    @Test(expected = NullPointerException.class)
    public void testDisableServiceWithNoActivity()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();

        assertTrue(mNfcFCardEmulation.disableService(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDisableServiceWithActivityNotResumedState()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        when(mActivity.isResumed()).thenReturn(false);

        assertTrue(mNfcFCardEmulation.disableService(mActivity));
    }

    @Test(expected = RuntimeException.class)
    public void testDisableServiceWithNoService()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        when(mActivity.isResumed()).thenReturn(true);
        when(mService.disableNfcFForegroundService())
                .thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(null);

        mNfcFCardEmulation.disableService(mActivity);
    }

    @Test
    public void testDisableServiceWithSecondTry()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        when(mActivity.isResumed()).thenReturn(true);
        when(mService.disableNfcFForegroundService())
                .thenThrow(RemoteException.class)
                .thenReturn(true);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(mService);

        assertTrue(mNfcFCardEmulation.disableService(mActivity));
    }

    @Test(expected = RuntimeException.class)
    public void testDisableServiceWithSecondTryFail()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        when(mActivity.isResumed()).thenReturn(true);
        when(mService.disableNfcFForegroundService())
                .thenThrow(RemoteException.class)
                .thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(mService);

        mNfcFCardEmulation.disableService(mActivity);
    }

    @Test
    public void testEnableServiceSuccess()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        when(mActivity.isResumed()).thenReturn(true);
        when(mService.enableNfcFForegroundService(mComponentName)).thenReturn(true);

        assertTrue(mNfcFCardEmulation.enableService(mActivity, mComponentName));
    }

    @Test(expected = NullPointerException.class)
    public void testEnableServiceWithNullActivityAndService()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();

        mNfcFCardEmulation.enableService(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnableServiceWithActivityNonResumeState()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        when(mActivity.isResumed()).thenReturn(false);

        mNfcFCardEmulation.enableService(mActivity, mComponentName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnableServiceWithException()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        when(mActivity.isResumed()).thenReturn(false);
        when(mService.enableNfcFForegroundService(mComponentName))
                .thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(null);

        assertFalse(mNfcFCardEmulation.enableService(mActivity, mComponentName));
    }

    @Test
    public void testEnableServiceWithSecondTry()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        when(mActivity.isResumed()).thenReturn(true);
        when(mService.enableNfcFForegroundService(mComponentName))
                .thenThrow(RemoteException.class)
                .thenReturn(true);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(mService);

        assertTrue(mNfcFCardEmulation.enableService(mActivity, mComponentName));
    }

    @Test(expected = RuntimeException.class)
    public void testEnableServiceWithSecondTryFail()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        when(mActivity.isResumed()).thenReturn(false);
        when(mService.enableNfcFForegroundService(mComponentName))
                .thenThrow(RemoteException.class)
                .thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(mService);

        mNfcFCardEmulation.enableService(mActivity, mComponentName);
    }

    @Test
    public void testSetNfcid2ForServiceSuccess()
            throws NoSuchFieldException, RemoteException {
        String nfcid2 = "nfcid2";
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.setNfcid2ForService(1, mComponentName, nfcid2))
                .thenReturn(true);

        assertTrue(mNfcFCardEmulation.setNfcid2ForService(mComponentName, nfcid2));
    }

    @Test(expected = NullPointerException.class)
    public void testSetNfcid2ForServiceWithNoService()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();

        mNfcFCardEmulation.setNfcid2ForService(null, null);
    }

    @Test(expected = RuntimeException.class)
    public void testSetNfcid2ForServiceWithException()
            throws NoSuchFieldException, RemoteException {
        String nfcid2 = "nfcid2";
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.setNfcid2ForService(1, mComponentName, nfcid2))
                .thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(null);

        mNfcFCardEmulation.setNfcid2ForService(mComponentName, nfcid2);
    }

    @Test
    public void testSetNfcid2ForServiceWithSecondTry()
            throws NoSuchFieldException, RemoteException {
        String nfcid2 = "nfcid2";
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.setNfcid2ForService(1, mComponentName, nfcid2))
                .thenThrow(RemoteException.class)
                .thenReturn(true);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(mService);

        assertTrue(mNfcFCardEmulation.setNfcid2ForService(mComponentName, nfcid2));
    }

    @Test(expected = RuntimeException.class)
    public void testSetNfcid2ForServiceWithSecondTryFail()
            throws NoSuchFieldException, RemoteException {
        String nfcid2 = "nfcid2";
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.setNfcid2ForService(1, mComponentName, nfcid2))
                .thenThrow(RemoteException.class)
                .thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(mService);

        mNfcFCardEmulation.setNfcid2ForService(mComponentName, nfcid2);
    }

    @Test
    public void testGetNfcid2ForServiceSuccess()
            throws NoSuchFieldException, RemoteException {
        String currentNfcId2 = "currentNFCID2";
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.getNfcid2ForService(1, mComponentName))
                .thenReturn(currentNfcId2);

        assertEquals(currentNfcId2,
                mNfcFCardEmulation.getNfcid2ForService(mComponentName));
    }

    @Test(expected = NullPointerException.class)
    public void testGetNfcid2ForServiceWithNoService()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();

        mNfcFCardEmulation.getNfcid2ForService(null);
    }

    @Test(expected = RuntimeException.class)
    public void testGetNfcid2ForServiceWithException()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.getNfcid2ForService(1, mComponentName))
                .thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(null);

        mNfcFCardEmulation.getNfcid2ForService(mComponentName);
    }

    @Test
    public void testGetNfcid2ForServiceWithSecondTry()
            throws NoSuchFieldException, RemoteException {
        String currentNfcId2 = "currentNFCID2";
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.getNfcid2ForService(1, mComponentName))
                .thenThrow(RemoteException.class)
                .thenReturn(currentNfcId2);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(mService);

        assertEquals(currentNfcId2,
                mNfcFCardEmulation.getNfcid2ForService(mComponentName));
    }

    @Test(expected = RuntimeException.class)
    public void testGetNfcid2ForServiceWithSecondTryFail()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.getNfcid2ForService(1, mComponentName))
                .thenThrow(RemoteException.class)
                .thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(mService);

        mNfcFCardEmulation.getNfcid2ForService(mComponentName);
    }

    @Test
    public void testUnregisterSystemCodeForServiceSuccess()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.removeSystemCodeForService(1, mComponentName))
                .thenReturn(true);

        assertTrue(
                mNfcFCardEmulation.unregisterSystemCodeForService(mComponentName));
    }

    @Test(expected = NullPointerException.class)
    public void testUnregisterSystemCodeForServiceWithNoService()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();

        mNfcFCardEmulation.unregisterSystemCodeForService(null);
    }

    @Test(expected = RuntimeException.class)
    public void testUnregisterSystemCodeForServiceWithException()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.removeSystemCodeForService(1, mComponentName))
                .thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(null);

        mNfcFCardEmulation.unregisterSystemCodeForService(mComponentName);
    }

    @Test
    public void testUnregisterSystemCodeForServiceWithSecondTry()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.removeSystemCodeForService(1, mComponentName))
                .thenThrow(RemoteException.class)
                .thenReturn(true);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(mService);

        assertTrue(
                mNfcFCardEmulation.unregisterSystemCodeForService(mComponentName));
    }

    @Test(expected = RuntimeException.class)
    public void testUnregisterSystemCodeForServiceWithSecondTryFail()
            throws NoSuchFieldException, RemoteException {
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.removeSystemCodeForService(1, mComponentName))
                .thenThrow(RemoteException.class)
                .thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(mService);

        mNfcFCardEmulation.unregisterSystemCodeForService(mComponentName);
    }

    @Test
    public void testRegisterSystemCodeForServiceSuccess()
            throws NoSuchFieldException, RemoteException {
        String systemCode = "systemCode";
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.registerSystemCodeForService(1, mComponentName, systemCode))
                .thenReturn(true);

        assertTrue(mNfcFCardEmulation.registerSystemCodeForService(mComponentName,
                systemCode));
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterSystemCodeForServiceWithNoService() {
        mNfcFCardEmulation.registerSystemCodeForService(null, null);
    }

    @Test(expected = RuntimeException.class)
    public void testRegisterSystemCodeForServiceWithException()
            throws NoSuchFieldException, RemoteException {
        String systemCode = "systemCode";
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.registerSystemCodeForService(1, mComponentName, systemCode))
                .thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(null);

        mNfcFCardEmulation.registerSystemCodeForService(mComponentName, systemCode);
    }

    @Test
    public void testRegisterSystemCodeForServiceWithSecondTry()
            throws NoSuchFieldException, RemoteException {
        String systemCode = "systemCode";
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.registerSystemCodeForService(1, mComponentName, systemCode))
                .thenThrow(RemoteException.class)
                .thenReturn(true);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(mService);

        assertTrue(mNfcFCardEmulation.registerSystemCodeForService(mComponentName,
                systemCode));
    }

    @Test(expected = RuntimeException.class)
    public void testRegisterSystemCodeForServiceWithSecondTryFail()
            throws NoSuchFieldException, RemoteException {
        String systemCode = "systemCode";
        mNfcFCardEmulation = createEmulationInstance();
        UserHandle mockUserHandle = mock(UserHandle.class);
        when(mContext.getUser()).thenReturn(mockUserHandle);
        when(mockUserHandle.getIdentifier()).thenReturn(1);
        when(mService.registerSystemCodeForService(1, mComponentName, systemCode))
                .thenThrow(RemoteException.class)
                .thenThrow(RemoteException.class);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);
        when(mNfcAdapter.getNfcFCardEmulationService()).thenReturn(mService);

        mNfcFCardEmulation.registerSystemCodeForService(mComponentName, systemCode);
    }
}
