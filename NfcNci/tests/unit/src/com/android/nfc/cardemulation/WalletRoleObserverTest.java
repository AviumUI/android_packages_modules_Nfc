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

package com.android.nfc.cardemulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.app.role.RoleManager;
import android.content.Context;
import android.nfc.PackageAndUser;
import android.os.UserHandle;
import android.os.UserManager;
import android.permission.flags.Flags;
import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.collect.ImmutableList;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.nfc.NfcEventLog;
import com.android.nfc.NfcInjector;
import com.android.nfc.NfcService;
import com.android.nfc.NfcStatsLog;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;


import java.util.List;
import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
public class WalletRoleObserverTest {

    private static final int USER_ID = 1;
    private static final UserHandle USER_HANDLE = UserHandle.of(USER_ID);

    private static final String WALLET_ROLE_HOLDER = "gms.wallet.stuff";

    private MockitoSession mStaticMockSession;

    WalletRoleObserver mWalletRoleObserver;
    @Mock
    Context mContext;
    @Mock
    RoleManager mRoleManager;
    @Mock
    WalletRoleObserver.Callback mCallback;
    @Mock
    Executor mExecutor;
    @Mock
    NfcInjector mNfcInjector;
    @Mock
    NfcEventLog mNfcEventLog;
    @Mock
    UserManager mUserManager;
    @Captor
    ArgumentCaptor<String> mRoleNameCaptor;
    @Captor
    ArgumentCaptor<UserHandle> mUserHandlerCaptor;
    @Captor
    ArgumentCaptor<Executor> mExecutorCaptor;
    @Captor
    ArgumentCaptor<String> mRoleHolderCaptor;
    @Before
    public void setUp() {
        mStaticMockSession = ExtendedMockito.mockitoSession()
                .mockStatic(ActivityManager.class)
                .mockStatic(Flags.class)
                .strictness(Strictness.LENIENT)
                .startMocking();
        MockitoAnnotations.initMocks(this);
        when(mContext.getMainExecutor()).thenReturn(mExecutor);
        when(mNfcInjector.getNfcEventLog()).thenReturn(mNfcEventLog);
        when(ActivityManager.getCurrentUser()).thenReturn(USER_ID);
        when(mRoleManager.getActiveUserForRole(RoleManager.ROLE_WALLET)).thenReturn(USER_HANDLE);
        when(mContext.createContextAsUser(any(), anyInt())).thenReturn(mContext);
        when(mContext.getSystemService(eq(RoleManager.class))).thenReturn(mRoleManager);
        when(mContext.getSystemService(eq(UserManager.class))).thenReturn(mUserManager);
        when(Flags.walletRoleCrossUserEnabled()).thenReturn(false);
        mWalletRoleObserver =
            new WalletRoleObserver(mContext, mRoleManager, mCallback, mNfcInjector);
    }

    @After
    public void tearDown() {
        mStaticMockSession.finishMocking();
    }

    @Test
    public void testConstructor() {
        verify(mContext).getMainExecutor();
        verify(mRoleManager).addOnRoleHoldersChangedListenerAsUser(mExecutorCaptor.capture(),any(),
                mUserHandlerCaptor.capture());
        assertEquals(mExecutor, mExecutorCaptor.getValue());
        assertEquals(UserHandle.ALL, mUserHandlerCaptor.getValue());
    }

    @Test
    public void testGetDefaultWalletRoleHolder_roleAvailable_returnsTheHolder() {
        List<String> roleHolders = ImmutableList.of(WALLET_ROLE_HOLDER);
        when(mRoleManager.isRoleAvailable(eq(RoleManager.ROLE_WALLET))).thenReturn(true);
        when(mRoleManager.getRoleHoldersAsUser(eq(RoleManager.ROLE_WALLET),
                eq(USER_HANDLE))).thenReturn(roleHolders);

        PackageAndUser roleHolder = mWalletRoleObserver.getDefaultWalletRoleHolder(USER_ID);

        verify(mRoleManager).isRoleAvailable(mRoleNameCaptor.capture());
        verify(mRoleManager).getRoleHoldersAsUser(mRoleNameCaptor.capture(),
                mUserHandlerCaptor.capture());
        assertEquals(WALLET_ROLE_HOLDER, roleHolder.getPackage());
        assertEquals(USER_ID, roleHolder.getUserId());
        assertEquals(RoleManager.ROLE_WALLET, mRoleNameCaptor.getAllValues().get(0));
        assertEquals(RoleManager.ROLE_WALLET, mRoleNameCaptor.getAllValues().get(1));
    }

    @Test
    public void testGetDefaultWalletRoleHolder_roleAvailable_returnsTheHolder_crossProfile() {
        when(Flags.walletRoleCrossUserEnabled()).thenReturn(true);

        List<String> roleHolders = ImmutableList.of(WALLET_ROLE_HOLDER);
        when(mRoleManager.getRoleHoldersAsUser(eq(RoleManager.ROLE_WALLET),
                eq(USER_HANDLE))).thenReturn(roleHolders);

        PackageAndUser roleHolder = mWalletRoleObserver.getDefaultWalletRoleHolder(USER_ID);

        verify(mRoleManager).getActiveUserForRole(mRoleNameCaptor.capture());
        verify(mRoleManager).getRoleHoldersAsUser(mRoleNameCaptor.capture(),
                mUserHandlerCaptor.capture());
        assertEquals(WALLET_ROLE_HOLDER, roleHolder.getPackage());
        assertEquals(USER_ID, roleHolder.getUserId());
        assertEquals(RoleManager.ROLE_WALLET, mRoleNameCaptor.getAllValues().get(0));
        assertEquals(RoleManager.ROLE_WALLET, mRoleNameCaptor.getAllValues().get(1));
    }

    @Test
    public void testGetDefaultWalletRoleHolder_roleAvailable_returnsTheHolder_differentProfile() {
        int otherUserId = 11;
        UserHandle otherUser = UserHandle.of(otherUserId);

        when(Flags.walletRoleCrossUserEnabled()).thenReturn(true);
        when(mUserManager.getProfileParent(otherUser)).thenReturn(USER_HANDLE);
        when(mRoleManager.getActiveUserForRole(RoleManager.ROLE_WALLET)).thenReturn(otherUser);

        List<String> roleHolders = ImmutableList.of(WALLET_ROLE_HOLDER);
        when(mRoleManager.getRoleHoldersAsUser(eq(RoleManager.ROLE_WALLET),
                eq(otherUser))).thenReturn(roleHolders);

        PackageAndUser roleHolder = mWalletRoleObserver.getDefaultWalletRoleHolder(USER_ID);

        verify(mRoleManager).getActiveUserForRole(mRoleNameCaptor.capture());
        verify(mRoleManager).getRoleHoldersAsUser(mRoleNameCaptor.capture(),
                mUserHandlerCaptor.capture());
        assertEquals(WALLET_ROLE_HOLDER, roleHolder.getPackage());
        assertEquals(otherUserId, roleHolder.getUserId());
        assertEquals(RoleManager.ROLE_WALLET, mRoleNameCaptor.getAllValues().get(0));
        assertEquals(RoleManager.ROLE_WALLET, mRoleNameCaptor.getAllValues().get(1));
    }

    @Test
    public void testGetDefaultWalletRoleHolder_roleNotAvailable_returnsNull() {
        PackageAndUser roleHolder = mWalletRoleObserver.getDefaultWalletRoleHolder(USER_ID);

        assertNull(roleHolder.getPackage());
    }

    @Test
    public void testCallbackFiringOnRoleChange_roleWallet() {
        List<String> roleHolders = ImmutableList.of(WALLET_ROLE_HOLDER);
        when(mRoleManager.getRoleHoldersAsUser(eq(RoleManager.ROLE_WALLET), eq(USER_HANDLE)))
                .thenReturn(roleHolders);
        mWalletRoleObserver.mOnRoleHoldersChangedListener
                .onRoleHoldersChanged(RoleManager.ROLE_WALLET, USER_HANDLE);

        verify(mRoleManager).getRoleHoldersAsUser(mRoleNameCaptor.capture(), eq(USER_HANDLE));
        verify(mCallback).onWalletRoleHolderChanged(mRoleHolderCaptor.capture(), eq(USER_ID));
        assertEquals(RoleManager.ROLE_WALLET, mRoleNameCaptor.getValue());
        assertEquals(WALLET_ROLE_HOLDER, mRoleHolderCaptor.getValue());
    }

    @Test
    public void testCallbackFiringOnRoleChange_roleWallet_crossProfile() {
        when(Flags.walletRoleCrossUserEnabled()).thenReturn(true);

        List<String> roleHolders = ImmutableList.of(WALLET_ROLE_HOLDER);
        when(mRoleManager.getActiveUserForRole(RoleManager.ROLE_WALLET))
                .thenReturn(USER_HANDLE);
        when(mRoleManager.getRoleHoldersAsUser(eq(RoleManager.ROLE_WALLET), eq(USER_HANDLE)))
                .thenReturn(roleHolders);
        mWalletRoleObserver.mOnRoleHoldersChangedListener
                .onRoleHoldersChanged(RoleManager.ROLE_WALLET, USER_HANDLE);

        verify(mRoleManager).getRoleHoldersAsUser(mRoleNameCaptor.capture(), eq(USER_HANDLE));
        verify(mCallback).onWalletRoleHolderChanged(mRoleHolderCaptor.capture(), eq(USER_ID));
        assertEquals(RoleManager.ROLE_WALLET, mRoleNameCaptor.getValue());
        assertEquals(WALLET_ROLE_HOLDER, mRoleHolderCaptor.getValue());
    }

    @Test
    public void testCallbackNotFiringOnRoleChange_roleNonWallet() {
        mWalletRoleObserver.mOnRoleHoldersChangedListener
                .onRoleHoldersChanged(RoleManager.ROLE_ASSISTANT, USER_HANDLE);

        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void testOnUserSwitched_callsCallback() {
        List<String> roleHolders = ImmutableList.of(WALLET_ROLE_HOLDER);
        when(mRoleManager.isRoleAvailable(eq(RoleManager.ROLE_WALLET))).thenReturn(true);
        when(mRoleManager.getRoleHoldersAsUser(eq(RoleManager.ROLE_WALLET),
                eq(USER_HANDLE))).thenReturn(roleHolders);

        mWalletRoleObserver.onUserSwitched(USER_ID);
        verify(mRoleManager).isRoleAvailable(mRoleNameCaptor.capture());
        verify(mRoleManager).getRoleHoldersAsUser(mRoleNameCaptor.capture(),
                mUserHandlerCaptor.capture());
        verify(mCallback).onWalletRoleHolderChanged(mRoleHolderCaptor.capture(), eq(USER_ID));
        assertEquals(WALLET_ROLE_HOLDER, mRoleHolderCaptor.getValue());
        assertEquals(RoleManager.ROLE_WALLET, mRoleNameCaptor.getAllValues().get(0));
        assertEquals(RoleManager.ROLE_WALLET, mRoleNameCaptor.getAllValues().get(1));
    }

}
