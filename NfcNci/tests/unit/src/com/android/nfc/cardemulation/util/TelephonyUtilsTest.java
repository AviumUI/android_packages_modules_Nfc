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

import static com.android.nfc.cardemulation.util.TelephonyUtils.MEP_MODE_A1;
import static com.android.nfc.cardemulation.util.TelephonyUtils.SIM_TYPE_EUICC_1;
import static com.android.nfc.cardemulation.util.TelephonyUtils.SIM_TYPE_EUICC_2;
import static com.android.nfc.cardemulation.util.TelephonyUtils.SUBSCRIPTION_ID_UICC;
import static com.android.nfc.cardemulation.util.TelephonyUtils.SUBSCRIPTION_ID_UNKNOWN;
import static com.android.nfc.cardemulation.util.TelephonyUtils.SUBSCRIPTION_STATE_INACTIVATE;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelephonyUtilsTest {
    @Mock
    private Context mContext;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;

    private TelephonyUtils mTelephonyUtils;
    private MockitoSession mStaticMockSession;

    @Before
    public void setUp() {
        mStaticMockSession = ExtendedMockito.mockitoSession()
                .mockStatic(SubscriptionManager.class)
                .mockStatic(Executors.class)
                .strictness(Strictness.LENIENT)
                .startMocking();
        MockitoAnnotations.initMocks(this);
        when(SubscriptionManager.from(mContext)).thenReturn(mSubscriptionManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        mTelephonyUtils = new TelephonyUtils(mContext);
    }

    @After
    public void tearDown() throws Exception {
        mStaticMockSession.finishMocking();
    }

    @Test
    public void testIsUiccSubscription() {
        assertTrue(mTelephonyUtils.isUiccSubscription(SUBSCRIPTION_ID_UICC));
        assertFalse(mTelephonyUtils.isUiccSubscription(SUBSCRIPTION_ID_UNKNOWN));
    }

    @Test
    public void testGetActiveSubscription() {
        SubscriptionInfo subscriptionInfo = mock(SubscriptionInfo.class);
        List<SubscriptionInfo> list = new ArrayList<>();
        list.add(subscriptionInfo);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(list);

        mTelephonyUtils.getActiveSubscriptions();
        verify(mSubscriptionManager).getActiveSubscriptionInfoList();
    }

    @Test
    public void testIsEuiccSubscription() {
        assertTrue(mTelephonyUtils.isEuiccSubscription(SUBSCRIPTION_STATE_INACTIVATE));
        assertFalse(mTelephonyUtils.isEuiccSubscription(SUBSCRIPTION_ID_UICC));
    }

    @Test
    public void testGetActiveSubscriptionInfoByIdWhenIdUicc() {
        SubscriptionInfo subscriptionInfo = mock(SubscriptionInfo.class);
        List<SubscriptionInfo> list = new ArrayList<>();
        list.add(subscriptionInfo);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(list);

        mTelephonyUtils.getActiveSubscriptionInfoById(SUBSCRIPTION_ID_UICC);
        verify(mSubscriptionManager).getActiveSubscriptionInfoList();
    }

    @Test
    public void testGetActiveSubscriptionInfoByIdWhenEmbeddedUicc() {
        SubscriptionInfo subscriptionInfo = mock(SubscriptionInfo.class);
        when(mSubscriptionManager.getActiveSubscriptionInfo(SUBSCRIPTION_ID_UNKNOWN)).thenReturn(
                subscriptionInfo);

        mTelephonyUtils.getActiveSubscriptionInfoById(SUBSCRIPTION_ID_UNKNOWN);
        verify(mSubscriptionManager).getActiveSubscriptionInfo(SUBSCRIPTION_ID_UNKNOWN);
    }

    @Test
    public void testOnSubscriptionsChanged() {
        TelephonyUtils.Callback callback = mock(TelephonyUtils.Callback.class);
        SubscriptionInfo subscriptionInfo = mock(SubscriptionInfo.class);
        List<SubscriptionInfo> list = new ArrayList<>();
        list.add(subscriptionInfo);
        try {
            FieldSetter.setField(mTelephonyUtils,
                    mTelephonyUtils.getClass().getDeclaredField("mCallback"),
                    callback);

        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        }
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(list);

        mTelephonyUtils.onSubscriptionsChanged();
        mTelephonyUtils.onSubscriptionsChanged();
        verify(callback).onActiveSubscriptionsUpdated(list);
    }

    @Test
    public void testUpdateSwpStatusForEuiccWhenSubscriptionExist() {
        SubscriptionInfo subscriptionInfo = mock(SubscriptionInfo.class);
        TelephonyManager telephonyManager = mock(TelephonyManager.class);
        List<SubscriptionInfo> list = new ArrayList<>();
        list.add(subscriptionInfo);
        when(subscriptionInfo.isEmbedded()).thenReturn(true);
        when(subscriptionInfo.getSubscriptionId()).thenReturn(1);
        when(mTelephonyManager.createForSubscriptionId(1)).thenReturn(telephonyManager);
        when(telephonyManager.iccTransmitApduBasicChannel(0x80, 0x7C, 0x02, 0x00, 0x00,
                "")).thenReturn("");
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(list);

        mTelephonyUtils.updateSwpStatusForEuicc(SIM_TYPE_EUICC_1);
        verify(mTelephonyManager).createForSubscriptionId(1);
        verify(telephonyManager).iccTransmitApduBasicChannel(0x80, 0x7C, 0x02, 0x00, 0x00, "");
    }

    @Test
    public void testUpdateSwpStatusForEuiccWhenSubscriptionNotExist() {
        SubscriptionInfo subscriptionInfo = mock(SubscriptionInfo.class);
        TelephonyManager telephonyManager = mock(TelephonyManager.class);
        List<SubscriptionInfo> list = new ArrayList<>();
        list.add(subscriptionInfo);
        when(subscriptionInfo.isEmbedded()).thenReturn(false);
        when(subscriptionInfo.getSubscriptionId()).thenReturn(1);
        when(mTelephonyManager.createForSubscriptionId(1)).thenReturn(telephonyManager);
        when(telephonyManager.iccTransmitApduBasicChannel(0x80, 0x7C, 0x02, 0x02, 0x00,
                "")).thenReturn("");
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(list);
        mTelephonyUtils.setMepMode(MEP_MODE_A1);

        mTelephonyUtils.updateSwpStatusForEuicc(SIM_TYPE_EUICC_2);
        verify(mTelephonyManager, never()).createForSubscriptionId(1);
        verify(telephonyManager, never()).iccTransmitApduBasicChannel(0x80, 0x7C, 0x02, 0x02, 0x00,
                "");
    }

    @Test
    public void testGetInstance() {
        Context context = mock(Context.class);
        when(SubscriptionManager.from(context)).thenReturn(mSubscriptionManager);
        when(context.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);

        mTelephonyUtils = TelephonyUtils.getInstance(context);
        verify(context).getSystemService(TelephonyManager.class);
    }

    @Test
    public void testRegisterSubscriptionChangedCallback() {
        TelephonyUtils.Callback callback = mock(TelephonyUtils.Callback.class);
        ExecutorService executorService = mock(ExecutorService.class);
        when(Executors.newSingleThreadExecutor()).thenReturn(executorService);

        mTelephonyUtils.registerSubscriptionChangedCallback(callback);
        verify(mSubscriptionManager).addOnSubscriptionsChangedListener(eq(executorService),
                any(SubscriptionManager.OnSubscriptionsChangedListener.class));
    }

}
