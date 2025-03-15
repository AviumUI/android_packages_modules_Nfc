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

package com.android.nfc.cardemulation.util;

import static android.nfc.cardemulation.PollingFrame.POLLING_LOOP_TYPE_UNKNOWN;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.verify;
import static com.android.nfc.NfcStatsLog.NFC_POLLING_LOOP_NOTIFICATION_REPORTED__PROPRIETARY_FRAME_TYPE__ECP_V1;
import static com.android.nfc.NfcStatsLog.NFC_POLLING_LOOP_NOTIFICATION_REPORTED__PROPRIETARY_FRAME_TYPE__ECP_V2;
import static com.android.nfc.NfcStatsLog.NFC_POLLING_LOOP_NOTIFICATION_REPORTED__PROPRIETARY_FRAME_TYPE__PROPRIETARY_FRAME_UNKNOWN;
import static com.android.nfc.cardemulation.util.StatsdUtils.CE_HCE_OTHER;
import static com.android.nfc.cardemulation.util.StatsdUtils.CE_HCE_PAYMENT;
import static com.android.nfc.cardemulation.util.StatsdUtils.CE_NO_ROUTING;
import static com.android.nfc.cardemulation.util.StatsdUtils.CE_OFFHOST;
import static com.android.nfc.cardemulation.util.StatsdUtils.CE_OFFHOST_OTHER;
import static com.android.nfc.cardemulation.util.StatsdUtils.CE_OFFHOST_PAYMENT;
import static com.android.nfc.cardemulation.util.StatsdUtils.CE_OTHER_DC_BOUND;
import static com.android.nfc.cardemulation.util.StatsdUtils.CE_OTHER_DC_RESPONSE;
import static com.android.nfc.cardemulation.util.StatsdUtils.CE_OTHER_WRONG_SETTING;
import static com.android.nfc.cardemulation.util.StatsdUtils.CE_PAYMENT_DC_BOUND;
import static com.android.nfc.cardemulation.util.StatsdUtils.CE_PAYMENT_DC_RESPONSE;
import static com.android.nfc.cardemulation.util.StatsdUtils.CE_PAYMENT_WRONG_SETTING;
import static com.android.nfc.cardemulation.util.StatsdUtils.CE_UNKNOWN;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.nfc.cardemulation.CardEmulation;
import android.nfc.cardemulation.PollingFrame;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.nfc.NfcStatsLog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoSession;

import java.lang.reflect.Field;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

@RunWith(AndroidJUnit4.class)
public class StatsdUtilsTest {
    private final StatsdUtils mStatsdUtils = spy(new StatsdUtils(new StatsdUtilsContext()));

    class CoverageSample extends StatsdUtils.PollingFrameLog {
        CoverageSample(byte[] data) {
            super(data);
        }
    }

    @Test
    public void testGetFrameType() {
        assertThat(StatsdUtils.getFrameType(ECP_V1_PAYMENT)).isEqualTo(
                NFC_POLLING_LOOP_NOTIFICATION_REPORTED__PROPRIETARY_FRAME_TYPE__ECP_V1);

        assertThat(StatsdUtils.getFrameType(ECP_V2_TRANSIT_MBTA)).isEqualTo(
                NFC_POLLING_LOOP_NOTIFICATION_REPORTED__PROPRIETARY_FRAME_TYPE__ECP_V2);

        assertThat(StatsdUtils.getFrameType(UNKNOWN_FRAME)).isEqualTo(
                NFC_POLLING_LOOP_NOTIFICATION_REPORTED__PROPRIETARY_FRAME_TYPE__PROPRIETARY_FRAME_UNKNOWN);
    }

    @Test
    public void testLogPollingFrame_ecp1Once() {
        PollingFrame frameData =
                new PollingFrame(POLLING_LOOP_TYPE_UNKNOWN, ECP_V1_PAYMENT, -1, 0, false);

        mStatsdUtils.tallyPollingFrame(ECP_V1_PAYMENT_KEY, frameData);
        mStatsdUtils.logPollingFrames();

        StatsdUtils.PollingFrameLog expectedFrame = new StatsdUtils.PollingFrameLog(ECP_V1_PAYMENT);
        expectedFrame.repeatCount = 1;

        verify(mStatsdUtils).writeToStatsd(expectedFrame);
    }

    @Test
    public void testLogPollingFrame_ecp1TwiceInTwoWrites() {
        PollingFrame frameData =
                new PollingFrame(POLLING_LOOP_TYPE_UNKNOWN, ECP_V1_PAYMENT, -1, 0, false);

        mStatsdUtils.tallyPollingFrame(ECP_V1_PAYMENT_KEY, frameData);
        mStatsdUtils.logPollingFrames();
        mStatsdUtils.tallyPollingFrame(ECP_V1_PAYMENT_KEY, frameData);
        mStatsdUtils.logPollingFrames();

        StatsdUtils.PollingFrameLog expectedFrame = new StatsdUtils.PollingFrameLog(ECP_V1_PAYMENT);
        expectedFrame.repeatCount = 1;

        verify(mStatsdUtils, times(2)).tallyPollingFrame(any(), any());
        verify(mStatsdUtils, times(2)).logPollingFrames();
        verify(mStatsdUtils, times(2)).writeToStatsd(expectedFrame);
        verifyNoMoreInteractions(mStatsdUtils);
    }

    @Test
    public void testLogPollingFrame_ecp2Repeated() {
        PollingFrame frameData =
                new PollingFrame(POLLING_LOOP_TYPE_UNKNOWN, ECP_V2_TRANSIT_MBTA, -1, 0, false);

        mStatsdUtils.tallyPollingFrame(ECP_V2_TRANSIT_MBTA_KEY, frameData);
        mStatsdUtils.tallyPollingFrame(ECP_V2_TRANSIT_MBTA_KEY, frameData);
        mStatsdUtils.tallyPollingFrame(ECP_V2_TRANSIT_MBTA_KEY, frameData);

        mStatsdUtils.logPollingFrames();

        StatsdUtils.PollingFrameLog expectedFrame =
                new StatsdUtils.PollingFrameLog(ECP_V2_TRANSIT_MBTA);
        expectedFrame.repeatCount = 3;
        verify(mStatsdUtils).writeToStatsd(expectedFrame);
    }

    @Test
    public void testLogPollingFrame_ecp2RepeatedTwoTypes() {
        PollingFrame frame1Data =
                new PollingFrame(POLLING_LOOP_TYPE_UNKNOWN, UNKNOWN_FRAME, -1, 0, false);

        PollingFrame frame2Data =
                new PollingFrame(POLLING_LOOP_TYPE_UNKNOWN, ECP_V2_TRANSIT_MBTA, -1, 0, false);

        mStatsdUtils.tallyPollingFrame(UNKNOWN_FRAME_KEY, frame1Data);
        mStatsdUtils.tallyPollingFrame(ECP_V2_TRANSIT_MBTA_KEY, frame2Data);
        mStatsdUtils.tallyPollingFrame(UNKNOWN_FRAME_KEY, frame1Data);
        mStatsdUtils.logPollingFrames();

        StatsdUtils.PollingFrameLog expectedFrame = new StatsdUtils.PollingFrameLog(UNKNOWN_FRAME);
        expectedFrame.repeatCount = 2;
        verify(mStatsdUtils).writeToStatsd(expectedFrame);

        expectedFrame = new StatsdUtils.PollingFrameLog(ECP_V2_TRANSIT_MBTA);
        expectedFrame.repeatCount = 1;

        verify(mStatsdUtils, times(3)).tallyPollingFrame(any(), any());
        verify(mStatsdUtils).logPollingFrames();
        verify(mStatsdUtils).writeToStatsd(expectedFrame);
        verifyNoMoreInteractions(mStatsdUtils);
    }

    @Test
    public void testFieldGain() {
        PollingFrame frame1Data =
                new PollingFrame(POLLING_LOOP_TYPE_UNKNOWN, UNKNOWN_FRAME, GAIN_1, 0, false);

        PollingFrame frame2Data =
                new PollingFrame(POLLING_LOOP_TYPE_UNKNOWN, UNKNOWN_FRAME, GAIN_1, 0, false);

        PollingFrame frame3Data =
                new PollingFrame(POLLING_LOOP_TYPE_UNKNOWN, UNKNOWN_FRAME, GAIN_2, 0, false);

        mStatsdUtils.tallyPollingFrame(UNKNOWN_FRAME_KEY, frame1Data);
        mStatsdUtils.tallyPollingFrame(UNKNOWN_FRAME_KEY, frame2Data);
        mStatsdUtils.tallyPollingFrame(UNKNOWN_FRAME_KEY, frame3Data);
        mStatsdUtils.logPollingFrames();

        verify(mStatsdUtils, times(3)).tallyPollingFrame(any(), any());
        verify(mStatsdUtils).logPollingFrames();
        verify(mStatsdUtils, times(1)).logFieldChanged(true, GAIN_1);
        verify(mStatsdUtils, times(1)).logFieldChanged(true, GAIN_2);
        verify(mStatsdUtils).writeToStatsd(any());
        verifyNoMoreInteractions(mStatsdUtils);
    }

    @Test
    public void testLogObserveModeStateChanged_noOverride() {
        MockitoSession session = mockitoSession().spyStatic(NfcStatsLog.class).startMocking();

        try {
            mStatsdUtils.logObserveModeStateChanged(true, StatsdUtils.TRIGGER_SOURCE_FOREGROUND_APP,
                    LATENCY_MS);

            verify(() -> NfcStatsLog.write(NfcStatsLog.NFC_OBSERVE_MODE_STATE_CHANGED,
                    NfcStatsLog.NFC_OBSERVE_MODE_STATE_CHANGED__STATE__OBSERVE_MODE_ENABLED,
                    NfcStatsLog.NFC_OBSERVE_MODE_STATE_CHANGED__TRIGGER_SOURCE__FOREGROUND_APP,
                    LATENCY_MS));
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void testLogObserveModeStateChanged_override() {
        MockitoSession session = mockitoSession().spyStatic(NfcStatsLog.class).startMocking();

        try {
            mStatsdUtils.setNextObserveModeTriggerSource(StatsdUtils.TRIGGER_SOURCE_AUTO_TRANSACT);
            mStatsdUtils.logObserveModeStateChanged(true, StatsdUtils.TRIGGER_SOURCE_FOREGROUND_APP,
                    LATENCY_MS);

            verify(() -> NfcStatsLog.write(NfcStatsLog.NFC_OBSERVE_MODE_STATE_CHANGED,
                    NfcStatsLog.NFC_OBSERVE_MODE_STATE_CHANGED__STATE__OBSERVE_MODE_ENABLED,
                    NfcStatsLog.NFC_OBSERVE_MODE_STATE_CHANGED__TRIGGER_SOURCE__AUTO_TRANSACT,
                    LATENCY_MS));
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void testGetCardEmulationStatsdCategoryForSuccessResultPayment()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = StatsdUtils.class.getDeclaredField("mBindingStartTimeMillis");
        field.setAccessible(true);
        field.set(mStatsdUtils, -100);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        mStatsdUtils.logErrorEvent(NfcStatsLog.NFC_ERROR_OCCURRED__TYPE__HCE_LATE_BINDING);
        mStatsdUtils.notifyCardEmulationEventResponseReceived();

        mStatsdUtils.setCardEmulationEventCategory(CardEmulation.CATEGORY_PAYMENT);
        mStatsdUtils.logCardEmulationDeactivatedEvent();
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_HCE_PAYMENT, captor.getValue().intValue());
    }

    @Test
    public void testGetCardEmulationStatsdCategoryForSuccessWithOther()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = StatsdUtils.class.getDeclaredField("mBindingStartTimeMillis");
        field.setAccessible(true);
        field.set(mStatsdUtils, -100);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

        mStatsdUtils.setCardEmulationEventCategory(CardEmulation.CATEGORY_OTHER);
        mStatsdUtils.logCardEmulationDeactivatedEvent();
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_HCE_OTHER, captor.getValue().intValue());
    }

    @Test
    public void testGetCardEmulationStatsdCategoryForSuccessWithUnknown()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = StatsdUtils.class.getDeclaredField("mBindingStartTimeMillis");
        field.setAccessible(true);
        field.set(mStatsdUtils, -100);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

        mStatsdUtils.setCardEmulationEventCategory("unknown");
        mStatsdUtils.logCardEmulationDeactivatedEvent();
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_UNKNOWN, captor.getValue().intValue());
    }

    @Test
    public void testGetCardEmulationStatsdCategoryForDcBefrBoundWithPayment()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = StatsdUtils.class.getDeclaredField("mBindingStartTimeMillis");
        field.setAccessible(true);
        field.set(mStatsdUtils, 100);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

        mStatsdUtils.setCardEmulationEventCategory(CardEmulation.CATEGORY_PAYMENT);
        mStatsdUtils.logCardEmulationDeactivatedEvent();
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_PAYMENT_DC_BOUND, captor.getValue().intValue());
    }

    @Test
    public void testGetCardEmulationStatsdCategoryForDcBefrBoundWithOther()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = StatsdUtils.class.getDeclaredField("mBindingStartTimeMillis");
        field.setAccessible(true);
        field.set(mStatsdUtils, 100);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

        mStatsdUtils.setCardEmulationEventCategory(CardEmulation.CATEGORY_OTHER);
        mStatsdUtils.logCardEmulationDeactivatedEvent();
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_OTHER_DC_BOUND, captor.getValue().intValue());
    }

    @Test
    public void testGetCardEmulationStatsdCategoryForcBefrBoundWithUnknown()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = StatsdUtils.class.getDeclaredField("mBindingStartTimeMillis");
        field.setAccessible(true);
        field.set(mStatsdUtils, 100);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

        mStatsdUtils.setCardEmulationEventCategory("unknown");
        mStatsdUtils.logCardEmulationDeactivatedEvent();
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_UNKNOWN, captor.getValue().intValue());
    }

    @Test
    public void testGetCardEmulationStatsdCategoryForDcBefrResponseWithPayment()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = StatsdUtils.class.getDeclaredField("mBindingStartTimeMillis");
        field.setAccessible(true);
        field.set(mStatsdUtils, -100);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

        mStatsdUtils.notifyCardEmulationEventWaitingForResponse();
        mStatsdUtils.setCardEmulationEventCategory(CardEmulation.CATEGORY_PAYMENT);
        mStatsdUtils.logCardEmulationDeactivatedEvent();
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_PAYMENT_DC_RESPONSE, captor.getValue().intValue());
    }

    @Test
    public void testGetCardEmulationStatsdCategoryForDcBefrResponseWithOther()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = StatsdUtils.class.getDeclaredField("mBindingStartTimeMillis");
        field.setAccessible(true);
        field.set(mStatsdUtils, -100);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

        mStatsdUtils.notifyCardEmulationEventWaitingForResponse();
        mStatsdUtils.setCardEmulationEventCategory(CardEmulation.CATEGORY_OTHER);
        mStatsdUtils.logCardEmulationDeactivatedEvent();
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_OTHER_DC_RESPONSE, captor.getValue().intValue());
    }

    @Test
    public void testGetCardEmulationStatsdCategoryForDcBefrResponseWithUnknown()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = StatsdUtils.class.getDeclaredField("mBindingStartTimeMillis");
        field.setAccessible(true);
        field.set(mStatsdUtils, -100);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

        mStatsdUtils.notifyCardEmulationEventWaitingForResponse();
        mStatsdUtils.setCardEmulationEventCategory("unknown");
        mStatsdUtils.logCardEmulationDeactivatedEvent();
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_UNKNOWN, captor.getValue().intValue());
    }

    @Test
    public void testGetCardEmulationStatsdCategoryForWrongAppWithPayment() {
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        mStatsdUtils.setCardEmulationEventCategory(CardEmulation.CATEGORY_PAYMENT);

        mStatsdUtils.logCardEmulationWrongSettingEvent();
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_PAYMENT_WRONG_SETTING, captor.getValue().intValue());
    }

    @Test
    public void testGetCardEmulationStatsdCategoryForWrongAppWithOther() {
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        mStatsdUtils.setCardEmulationEventCategory(CardEmulation.CATEGORY_OTHER);

        mStatsdUtils.logCardEmulationWrongSettingEvent();
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_OTHER_WRONG_SETTING, captor.getValue().intValue());
    }

    @Test
    public void testGetCardEmulationStatsdCategoryForWrongAppWithUnknown() {
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        mStatsdUtils.setCardEmulationEventCategory("unknown");

        mStatsdUtils.logCardEmulationWrongSettingEvent();
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_UNKNOWN, captor.getValue().intValue());
    }

    @Test
    public void testGetCardEmulationStatsdCategoryForNoRoutingForAid() {
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        mStatsdUtils.setCardEmulationEventCategory("unknown");

        mStatsdUtils.logCardEmulationNoRoutingEvent();
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_NO_ROUTING, captor.getValue().intValue());
    }


    @Test
    public void testPollingFrameLogToString() {
        CoverageSample coverageSample = new CoverageSample(ECP_V1_PAYMENT);
        String expected = "PollingFrameLog{repeatCount="
                + 1
                + ", frameType="
                + NFC_POLLING_LOOP_NOTIFICATION_REPORTED__PROPRIETARY_FRAME_TYPE__ECP_V1
                + "}";

        assertEquals(expected, coverageSample.toString());
    }

    @Test
    public void testPollingFrameLogHashCode() {
        CoverageSample coverageSample = new CoverageSample(ECP_V1_PAYMENT);

        assertEquals(Objects.hash(1,
                        NFC_POLLING_LOOP_NOTIFICATION_REPORTED__PROPRIETARY_FRAME_TYPE__ECP_V1),
                coverageSample.hashCode());
    }

    @Test
    public void testLogCardEmulationNoRoutingEvent()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = StatsdUtils.class.getDeclaredField("mTransactionCategory");
        field.setAccessible(true);
        field.set(mStatsdUtils, CardEmulation.EXTRA_CATEGORY);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

        mStatsdUtils.logCardEmulationNoRoutingEvent();
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_NO_ROUTING, captor.getValue().intValue());
    }

    @Test
    public void testLogCardEmulationOffhostEventForOffHostPayment()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = StatsdUtils.class.getDeclaredField("mTransactionCategory");
        field.setAccessible(true);
        field.set(mStatsdUtils, CardEmulation.CATEGORY_PAYMENT);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

        mStatsdUtils.logCardEmulationOffhostEvent("");
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_OFFHOST_PAYMENT, captor.getValue().intValue());
    }

    @Test
    public void testLogCardEmulationOffhostEventForOffHostOthers()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = StatsdUtils.class.getDeclaredField("mTransactionCategory");
        field.setAccessible(true);
        field.set(mStatsdUtils, CardEmulation.CATEGORY_OTHER);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

        mStatsdUtils.logCardEmulationOffhostEvent("");
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_OFFHOST_OTHER, captor.getValue().intValue());
    }

    @Test
    public void testLogCardEmulationOffhostEvent() {
        mStatsdUtils.setCardEmulationEventCategory(CardEmulation.EXTRA_CATEGORY);
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

        mStatsdUtils.logCardEmulationOffhostEvent("");
        verify(mStatsdUtils).logCardEmulationEvent(captor.capture());
        assertEquals(CE_OFFHOST, captor.getValue().intValue());
    }

    private static final int GAIN_1 = 42;
    private static final int GAIN_2 = 25;
    private static final byte[] ECP_V1_PAYMENT = new byte[]{0x6a, 0x01, 0x00, 0x00, 0x00};
    private static final String ECP_V1_PAYMENT_KEY =
            HexFormat.of().formatHex(ECP_V1_PAYMENT).toUpperCase(Locale.ROOT);
    private static final byte[] ECP_V2_TRANSIT_MBTA =
            new byte[]{0x6a, 0x02, (byte) 0xc8, 0x01, 0x00, 0x03, 0x00, 0x03, 0x7f, 0x00, 0x00,
                    0x00, 0x00, 0x71, (byte) 0xe7};
    private static final String ECP_V2_TRANSIT_MBTA_KEY =
            HexFormat.of().formatHex(ECP_V2_TRANSIT_MBTA).toUpperCase(Locale.ROOT);

    private static final byte[] UNKNOWN_FRAME =
            new byte[]{0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x20, 0x41, 0x6e, 0x64, 0x72, 0x6f, 0x69, 0x64,
                    0x21};
    private static final String UNKNOWN_FRAME_KEY =
            HexFormat.of().formatHex(UNKNOWN_FRAME).toUpperCase(Locale.ROOT);
    private static final int LATENCY_MS = 100;
}
