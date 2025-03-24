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

package com.android.nfc.cardemulation;

import static com.android.nfc.cardemulation.PreferredSubscriptionService.PREF_PREFERRED_SUB_ID;
import static com.android.nfc.cardemulation.PreferredSubscriptionService.PREF_SUBSCRIPTION;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.telephony.SubscriptionInfo;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.nfc.cardemulation.util.TelephonyUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class PreferredSubscriptionServiceTest {
    @Mock
    private Context mContext;
    @Mock
    private PreferredSubscriptionService.Callback mCallback;
    @Mock
    private TelephonyUtils mTelephonyUtils;
    @Mock
    private SharedPreferences mSubscriptionPrefs;
    @Mock
    private SharedPreferences.Editor mEditor;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private Resources mResources;
    private MockitoSession mStaticMockSession;
    private PreferredSubscriptionService mPreferredSubscriptionService;

    @Before
    public void setUp() {
        mStaticMockSession =
                ExtendedMockito.mockitoSession()
                        .mockStatic(TelephonyUtils.class).strictness(
                                Strictness.LENIENT).startMocking();
        MockitoAnnotations.initMocks(this);
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(
                PackageManager.FEATURE_NFC_OFF_HOST_CARD_EMULATION_UICC)).thenReturn(true);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(anyInt())).thenReturn(true);
        when(TelephonyUtils.getInstance(mContext)).thenReturn(mTelephonyUtils);
        when(mContext.getSharedPreferences(PREF_SUBSCRIPTION, Context.MODE_PRIVATE)).thenReturn(
                mSubscriptionPrefs);
        when(mSubscriptionPrefs.getInt(PREF_PREFERRED_SUB_ID,
                TelephonyUtils.SUBSCRIPTION_ID_UNKNOWN)).thenReturn(
                TelephonyUtils.SUBSCRIPTION_ID_UNKNOWN);
        when(mSubscriptionPrefs.edit()).thenReturn(mEditor);
        when(mEditor.putInt(PREF_PREFERRED_SUB_ID, TelephonyUtils.SUBSCRIPTION_ID_UICC)).thenReturn(
                editor);
        when(editor.commit()).thenReturn(true);
        mPreferredSubscriptionService = new PreferredSubscriptionService(mContext, mCallback);
    }

    @After
    public void tearDown() {
        mStaticMockSession.finishMocking();
    }

    @Test
    public void testGetPreferredSubscriptionId() {
        when(mSubscriptionPrefs.getInt(PREF_PREFERRED_SUB_ID,
                TelephonyUtils.SUBSCRIPTION_ID_UNKNOWN)).thenReturn(
                TelephonyUtils.SUBSCRIPTION_ID_UICC);

        assertEquals(TelephonyUtils.SUBSCRIPTION_ID_UICC,
                mPreferredSubscriptionService.getPreferredSubscriptionId());
    }

    @Test
    public void testSetPreferredSubscriptionId() throws IllegalAccessException,
            NoSuchFieldException {
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        List<SubscriptionInfo> infos = new ArrayList<>();
        Field fieldForegroundComp = PreferredSubscriptionService.class.getDeclaredField(
                "mDefaultSubscriptionId");
        fieldForegroundComp.setAccessible(true);
        fieldForegroundComp.set(mPreferredSubscriptionService,
                TelephonyUtils.SUBSCRIPTION_ID_UNKNOWN);
        when(mSubscriptionInfo.isEmbedded()).thenReturn(false);
        when(mSubscriptionInfo.areUiccApplicationsEnabled()).thenReturn(true);
        infos.add(mSubscriptionInfo);
        when(mSubscriptionPrefs.edit()).thenReturn(mEditor);
        when(mEditor.putInt(PREF_PREFERRED_SUB_ID,
                TelephonyUtils.SUBSCRIPTION_ID_UICC)).thenReturn(
                editor);
        when(editor.commit()).thenReturn(true);
        when(mTelephonyUtils.getActiveSubscriptions()).thenReturn(infos);
        when(mTelephonyUtils.isEuiccSubscription(
                TelephonyUtils.SUBSCRIPTION_ID_UICC)).thenReturn(
                false);

        mPreferredSubscriptionService.setPreferredSubscriptionId(
                TelephonyUtils.SUBSCRIPTION_ID_UICC, true);
        verify(mTelephonyUtils).isEuiccSubscription(TelephonyUtils.SUBSCRIPTION_ID_UICC);
        verify(editor).commit();
    }

    @Test
    public void testOnActiveSubscriptionsUpdated()
            throws NoSuchFieldException, IllegalAccessException {
        List<SubscriptionInfo> infos = new ArrayList<>();
        Field fieldForegroundComp = PreferredSubscriptionService.class.getDeclaredField(
                "mDefaultSubscriptionId");
        fieldForegroundComp.setAccessible(true);
        fieldForegroundComp.set(mPreferredSubscriptionService,
                TelephonyUtils.SUBSCRIPTION_ID_UICC);
        when(mSubscriptionInfo.isEmbedded()).thenReturn(false);
        when(mSubscriptionInfo.areUiccApplicationsEnabled()).thenReturn(true);
        infos.add(mSubscriptionInfo);
        when(mTelephonyUtils.getActiveSubscriptions()).thenReturn(infos);
        when(mTelephonyUtils.isEuiccSubscription(TelephonyUtils.SUBSCRIPTION_ID_UICC)).thenReturn(
                false);

        mPreferredSubscriptionService.onActiveSubscriptionsUpdated(infos);
        verify(mTelephonyUtils).isEuiccSubscription(TelephonyUtils.SUBSCRIPTION_ID_UICC);
        verify(mCallback).onPreferredSubscriptionChanged(TelephonyUtils.SUBSCRIPTION_ID_UICC,
                false);
    }

    @Test
    public void testInitialize() throws NoSuchFieldException, IllegalAccessException {
        List<SubscriptionInfo> infos = new ArrayList<>();
        Field fieldForegroundComp = PreferredSubscriptionService.class.getDeclaredField(
                "mDefaultSubscriptionId");
        fieldForegroundComp.setAccessible(true);
        fieldForegroundComp.set(mPreferredSubscriptionService,
                TelephonyUtils.SUBSCRIPTION_ID_UNKNOWN);
        when(mSubscriptionInfo.isEmbedded()).thenReturn(false);
        when(mSubscriptionInfo.areUiccApplicationsEnabled()).thenReturn(true);
        infos.add(mSubscriptionInfo);
        when(mTelephonyUtils.getActiveSubscriptions()).thenReturn(infos);
        when(mTelephonyUtils.isEuiccSubscription(anyInt())).thenReturn(false);

        mPreferredSubscriptionService.initialize();
        verify(mTelephonyUtils).isEuiccSubscription(anyInt());
        verify(mTelephonyUtils).registerSubscriptionChangedCallback(
                any(TelephonyUtils.Callback.class));

    }

}
