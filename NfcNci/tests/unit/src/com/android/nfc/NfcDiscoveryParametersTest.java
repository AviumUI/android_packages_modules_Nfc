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

import static com.android.nfc.NfcDiscoveryParameters.NFC_POLL_DEFAULT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.util.proto.ProtoOutputStream;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.dx.mockito.inline.extended.ExtendedMockito;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.util.Objects;

@RunWith(AndroidJUnit4.class)
public class NfcDiscoveryParametersTest {

    private static final String TAG = NfcDiscoveryParametersTest.class.getSimpleName();
    private MockitoSession mStaticMockSession;

    @Before
    public void setUp() throws Exception {
        mStaticMockSession = ExtendedMockito.mockitoSession()
                .strictness(Strictness.LENIENT)
                .startMocking();
    }

    @After
    public void tearDown() throws Exception {
        mStaticMockSession.finishMocking();
    }


    private NfcDiscoveryParameters computeDiscoveryParameters() {
        // Recompute discovery parameters based on screen state
        NfcDiscoveryParameters.Builder paramsBuilder = NfcDiscoveryParameters.newBuilder();
        paramsBuilder.setTechMask(1);
        paramsBuilder.setEnableLowPowerDiscovery(true);
        paramsBuilder.setEnableHostRouting(true);
        paramsBuilder.setEnableReaderMode(true);
        return paramsBuilder.build();
    }

    @Test
    public void testGetTechMask() {
        NfcDiscoveryParameters nfcDiscoveryParameters = computeDiscoveryParameters();
        int techMask = nfcDiscoveryParameters.getTechMask();
        Assert.assertEquals(1, techMask);
    }

    @Test
    public void testDiscoveryParameters() {
        NfcDiscoveryParameters.Builder paramsBuilder = NfcDiscoveryParameters.newBuilder();
        NfcDiscoveryParameters nfcDiscoveryParameters = paramsBuilder.build();
        boolean shouldEnableDiscovery = nfcDiscoveryParameters.shouldEnableDiscovery();
        boolean shouldEnableLowPowerDiscovery =
                nfcDiscoveryParameters.shouldEnableLowPowerDiscovery();
        boolean shouldEnableReaderMode = nfcDiscoveryParameters.shouldEnableReaderMode();
        boolean shouldEnableHostRouting = nfcDiscoveryParameters.shouldEnableHostRouting();

        assertFalse(shouldEnableDiscovery);
        assertTrue(shouldEnableLowPowerDiscovery);
        assertFalse(shouldEnableReaderMode);
        assertFalse(shouldEnableHostRouting);

        nfcDiscoveryParameters = computeDiscoveryParameters();
        shouldEnableDiscovery = nfcDiscoveryParameters.shouldEnableDiscovery();
        shouldEnableLowPowerDiscovery = nfcDiscoveryParameters.shouldEnableLowPowerDiscovery();
        shouldEnableReaderMode = nfcDiscoveryParameters.shouldEnableReaderMode();
        shouldEnableHostRouting = nfcDiscoveryParameters.shouldEnableHostRouting();

        assertTrue(shouldEnableDiscovery);
        assertFalse(shouldEnableLowPowerDiscovery);
        assertTrue(shouldEnableReaderMode);
        assertTrue(shouldEnableHostRouting);

    }

    @Test
    public void testDefaultInstance() {
        ProtoOutputStream proto = mock(ProtoOutputStream.class);
        NfcDiscoveryParameters nfcDiscoveryParameters = NfcDiscoveryParameters.getDefaultInstance();
        nfcDiscoveryParameters.dumpDebug(proto);

        assertNotNull(nfcDiscoveryParameters);
    }

    @Test
    public void testToStringDefault() {
        NfcDiscoveryParameters.Builder builder = NfcDiscoveryParameters.newBuilder();
        builder.setTechMask(NFC_POLL_DEFAULT);
        builder.setEnableLowPowerDiscovery(true);
        builder.setEnableReaderMode(true);
        builder.setEnableHostRouting(true);
        NfcDiscoveryParameters nfcDiscoveryParameters = builder.build();
        String stringBuilder = """
                mTechMask: default
                mEnableLPD: false
                mEnableReader: true
                mEnableHostRouting: true
                """;

        assertEquals(stringBuilder, nfcDiscoveryParameters.toString());
    }

    @Test
    public void testToString() {
        NfcDiscoveryParameters.Builder builder = NfcDiscoveryParameters.newBuilder();
        builder.setTechMask(1);
        builder.setEnableLowPowerDiscovery(true);
        builder.setEnableReaderMode(true);
        builder.setEnableHostRouting(true);
        NfcDiscoveryParameters nfcDiscoveryParameters = builder.build();

        String stringBuilder = """
                mTechMask: 1
                mEnableLPD: false
                mEnableReader: true
                mEnableHostRouting: true
                """;

        assertEquals(stringBuilder, nfcDiscoveryParameters.toString());
    }

    @Test
    public void testHashCode() {
        NfcDiscoveryParameters.Builder builder = NfcDiscoveryParameters.newBuilder();
        builder.setTechMask(NFC_POLL_DEFAULT);
        builder.setEnableLowPowerDiscovery(true);
        builder.setEnableReaderMode(true);
        builder.setEnableHostRouting(true);
        NfcDiscoveryParameters nfcDiscoveryParameters = builder.build();
        int expectedCode = Objects.hash(NFC_POLL_DEFAULT, false, true, true);

        assertEquals(expectedCode, nfcDiscoveryParameters.hashCode());
    }

    @Test
    public void testEquals() {
        NfcDiscoveryParameters.Builder builder = NfcDiscoveryParameters.newBuilder();
        builder.setTechMask(NFC_POLL_DEFAULT);
        builder.setEnableLowPowerDiscovery(true);
        builder.setEnableReaderMode(true);
        builder.setEnableHostRouting(true);
        NfcDiscoveryParameters nfcDiscoveryParameters = builder.build();
        NfcDiscoveryParameters obj = builder.build();

        assertTrue(nfcDiscoveryParameters.equals(obj));
    }

    @Test
    public void testEqualsWithNullObject() {
        NfcDiscoveryParameters.Builder builder = NfcDiscoveryParameters.newBuilder();
        builder.setTechMask(NFC_POLL_DEFAULT);
        builder.setEnableLowPowerDiscovery(true);
        builder.setEnableReaderMode(true);
        builder.setEnableHostRouting(true);
        NfcDiscoveryParameters nfcDiscoveryParameters = builder.build();

        assertFalse(nfcDiscoveryParameters.equals(null));
    }
}
