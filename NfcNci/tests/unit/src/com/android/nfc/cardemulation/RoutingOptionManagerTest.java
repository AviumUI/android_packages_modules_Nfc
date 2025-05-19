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

import static com.android.nfc.cardemulation.RoutingOptionManager.KEY_AUTO_CHANGE_CAPABLE;
import static com.android.nfc.cardemulation.RoutingOptionManager.KEY_DEFAULT_ISO_DEP_ROUTE;
import static com.android.nfc.cardemulation.RoutingOptionManager.KEY_DEFAULT_FELICA_ROUTE;
import static com.android.nfc.cardemulation.RoutingOptionManager.KEY_DEFAULT_OFFHOST_ROUTE;
import static com.android.nfc.cardemulation.RoutingOptionManager.KEY_DEFAULT_ROUTE;
import static com.android.nfc.cardemulation.RoutingOptionManager.KEY_DEFAULT_SC_ROUTE;
import static com.android.nfc.cardemulation.RoutingOptionManager.ROUTE_DEFAULT;
import static com.android.nfc.cardemulation.RoutingOptionManager.ROUTE_UNKNOWN;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.test.runner.AndroidJUnit4;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.nfc.DeviceConfigFacade;
import com.android.nfc.NfcService;
import com.android.nfc.cardemulation.util.TelephonyUtils;
import com.android.nfc.dhimpl.NativeNfcManager;

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

import java.lang.reflect.Field;

@RunWith(AndroidJUnit4.class)
public class RoutingOptionManagerTest {
    @Mock
    private NfcService mNfcService;
    @Mock
    private NativeNfcManager mNativeNfcManager;
    @Captor
    private ArgumentCaptor<Integer> mRouteCaptor;
    private static final int DEFAULT_ROUTE = 0;
    private static final int DEFAULT_ISO_DEP_ROUTE = 1;
    private static final int NDEF_NFCEE_ROUTE = 4;
    private static final int OVERRIDDEN_ISO_DEP_ROUTE = 10;
    private static final int OVERRIDDEN_OFF_HOST_ROUTE = 20;
    private static final int DEFAULT_OFF_HOST_ROUTE = 2;
    private static final int DEFAULT_FELICA_ROUTE = 3;
    private static final int DEFAULT_SC_ROUTE = 2;
    private static final byte[] OFF_HOST_UICC = new byte[] {5, 6};
    private static final byte[] OFF_HOST_ESE = new byte[] {3, 4};
    private static final int AID_MATCHING_MODE = 3;
    private static final int DEFAULT_EUICC_MEP_MODE = 0;
    private RoutingOptionManager mRoutingOptionManager;

    private static class TestRoutingOptionManager extends RoutingOptionManager {
        @Override
        int doGetDefaultRouteDestination() {
            return DEFAULT_ROUTE;
        }

        @Override
        int doGetDefaultIsoDepRouteDestination() {
            return DEFAULT_ISO_DEP_ROUTE;
        }

        @Override
        int doGetDefaultOffHostRouteDestination() {
            return DEFAULT_OFF_HOST_ROUTE;
        }

        @Override
        int doGetDefaultFelicaRouteDestination() {
            return DEFAULT_FELICA_ROUTE;
        }

        @Override
        int doGetDefaultScRouteDestination() {
            return DEFAULT_SC_ROUTE;
        }

        @Override
        byte[] doGetOffHostUiccDestination() {
            return OFF_HOST_UICC;
        }

        @Override
        byte[] doGetOffHostEseDestination() {
            return OFF_HOST_ESE;
        }

        @Override
        int doGetAidMatchingMode() {
            return AID_MATCHING_MODE;
        }

        @Override
        int doGetEuiccMepMode() {
            return DEFAULT_EUICC_MEP_MODE;
        }
    }

    private TestRoutingOptionManager mManager;
    private MockitoSession mStaticMockSession;

    @Before
    public void setUp() throws Exception {
        mStaticMockSession = ExtendedMockito.mockitoSession()
                .mockStatic(NfcService.class)
                .mockStatic(NativeNfcManager.class)
                .strictness(Strictness.LENIENT)
                .startMocking();
        MockitoAnnotations.initMocks(this);

        when(mNativeNfcManager.getNdefNfceeRouteId()).thenReturn(NDEF_NFCEE_ROUTE);
        when(NfcService.getInstance()).thenReturn(mNfcService);
        when(NativeNfcManager.getInstance()).thenReturn(mNativeNfcManager);
        mRoutingOptionManager = new RoutingOptionManager() {
            @Override
            int doGetDefaultRouteDestination() {
                return DEFAULT_ROUTE;
            }

            @Override
            int doGetDefaultIsoDepRouteDestination() {
                return DEFAULT_ISO_DEP_ROUTE;
            }

            @Override
            int doGetDefaultOffHostRouteDestination() {
                return DEFAULT_OFF_HOST_ROUTE;
            }

            @Override
            int doGetDefaultFelicaRouteDestination() {
                return DEFAULT_FELICA_ROUTE;
            }

            @Override
            int doGetDefaultScRouteDestination() {
                return DEFAULT_SC_ROUTE;
            }

            @Override
            byte[] doGetOffHostUiccDestination() {
                return OFF_HOST_UICC;
            }

            @Override
            byte[] doGetOffHostEseDestination() {
                return OFF_HOST_ESE;
            }

            @Override
            int doGetAidMatchingMode() {
                return AID_MATCHING_MODE;
            }

            @Override
            int doGetEuiccMepMode() {
                return DEFAULT_EUICC_MEP_MODE;
            }
        };

    }

    @After
    public void tearDown() {
        mStaticMockSession.finishMocking();
    }

    @Test
    public void testConstructor() {
        mManager = new TestRoutingOptionManager();

        assertEquals(DEFAULT_ROUTE, mManager.mDefaultRoute);
        assertEquals(DEFAULT_ISO_DEP_ROUTE, mManager.mDefaultIsoDepRoute);
        assertEquals(DEFAULT_OFF_HOST_ROUTE, mManager.mDefaultOffHostRoute);
        assertEquals(DEFAULT_FELICA_ROUTE, mManager.mDefaultFelicaRoute);
        assertEquals(OFF_HOST_UICC, mManager.mOffHostRouteUicc);
        assertEquals(OFF_HOST_ESE, mManager.mOffHostRouteEse);
        assertEquals(AID_MATCHING_MODE, mManager.mAidMatchingSupport);
    }

    @Test
    public void testOverrideDefaultIsoDepRoute() {
        mManager = new TestRoutingOptionManager();

        mManager.overrideDefaultIsoDepRoute(OVERRIDDEN_ISO_DEP_ROUTE);
        assertEquals(OVERRIDDEN_ISO_DEP_ROUTE, mManager.getOverrideDefaultIsoDepRoute());
        verify(mNfcService).setIsoDepProtocolRoute(mRouteCaptor.capture());
        assertEquals(Integer.valueOf(OVERRIDDEN_ISO_DEP_ROUTE), mRouteCaptor.getValue());
    }

    @Test
    public void testOverrideDefaultOffHostRoute() {
        mManager = new TestRoutingOptionManager();

        mManager.overrideDefaultOffHostRoute(OVERRIDDEN_OFF_HOST_ROUTE);
        assertEquals(OVERRIDDEN_OFF_HOST_ROUTE, mManager.getOverrideDefaultOffHostRoute());
        verify(mNfcService).setTechnologyABFRoute(mRouteCaptor.capture(), mRouteCaptor.capture());
        assertEquals(Integer.valueOf(OVERRIDDEN_OFF_HOST_ROUTE), mRouteCaptor.getValue());
    }

    @Test
    public void testOverrideDefaulttRoute() {
        mManager = new TestRoutingOptionManager();

        mManager.overrideDefaultRoute(OVERRIDDEN_OFF_HOST_ROUTE);
        assertEquals(OVERRIDDEN_OFF_HOST_ROUTE, mManager.getOverrideDefaultRoute());
    }

    @Test
    public void testRecoverOverridedRoutingTable() {
        mManager = new TestRoutingOptionManager();

        mManager.recoverOverridedRoutingTable();

        verify(mNfcService).setIsoDepProtocolRoute(anyInt());
        verify(mNfcService).setTechnologyABFRoute(anyInt(), anyInt());
        assertEquals(ROUTE_UNKNOWN, mManager.mOverrideDefaultRoute);
        assertEquals(ROUTE_UNKNOWN, mManager.mOverrideDefaultIsoDepRoute);
        assertEquals(ROUTE_UNKNOWN, mManager.mOverrideDefaultOffHostRoute);
    }

    @Test
    public void testGetters() {
        mManager = new TestRoutingOptionManager();

        int overrideDefaultRoute = mManager.getOverrideDefaultRoute();
        int defaultRoute = mManager.getDefaultRoute();
        int defaultIsoDepRoute = mManager.getDefaultIsoDepRoute();
        int defaultOffHostRoute = mManager.getDefaultOffHostRoute();
        int defaultFelicaRoute = mManager.getDefaultFelicaRoute();
        byte[] offHostRouteUicc = mManager.getOffHostRouteUicc();
        byte[] offHostRouteEse = mManager.getOffHostRouteEse();
        int aidMatchingSupport = mManager.getAidMatchingSupport();

        assertEquals(-1, overrideDefaultRoute);
        assertEquals(DEFAULT_ROUTE, defaultRoute);
        assertEquals(DEFAULT_ISO_DEP_ROUTE, defaultIsoDepRoute);
        assertEquals(DEFAULT_OFF_HOST_ROUTE, defaultOffHostRoute);
        assertEquals(DEFAULT_FELICA_ROUTE, defaultFelicaRoute);
        assertEquals(OFF_HOST_UICC, offHostRouteUicc);
        assertEquals(OFF_HOST_ESE, offHostRouteEse);
        assertEquals(AID_MATCHING_MODE, aidMatchingSupport);
        assertEquals(DEFAULT_SC_ROUTE, mManager.getDefaultScRoute());
    }

    @Test
    public void testIsRoutingTableOverrided() {
        mManager = new TestRoutingOptionManager();

        boolean result = mManager.isRoutingTableOverrided();
        assertFalse(result);
    }

    @Test
    public void testDefaultScRoute() {
        mManager = new TestRoutingOptionManager();
        assertEquals(ROUTE_UNKNOWN, mManager.getOverrideDefaultScRoute());

        mManager.overrideDefaultScRoute(DEFAULT_SC_ROUTE);
        assertEquals(DEFAULT_SC_ROUTE, mManager.getOverrideDefaultScRoute());
    }

    @Test
    public void testPreferredSim() {
        mManager = new TestRoutingOptionManager();
        assertEquals("SIM1", mManager.getPreferredSim());
    }

    @Test
    public void testAutoChangeStatus() throws NoSuchFieldException, IllegalAccessException {
        SharedPreferences mPrefs = mock(SharedPreferences.class);
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        Field field = RoutingOptionManager.class.getDeclaredField("mPrefs");
        field.setAccessible(true);
        field.set(mRoutingOptionManager, mPrefs);
        when(mPrefs.edit()).thenReturn(editor);
        when(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor);

        assertTrue(mRoutingOptionManager.isAutoChangeEnabled());

        mRoutingOptionManager.setAutoChangeStatus(false);
        assertFalse(mRoutingOptionManager.isAutoChangeEnabled());
    }

    @Test
    public void testOverwriteRoutingTableWithDefaultRoute()
            throws NoSuchFieldException, IllegalAccessException {

        SharedPreferences mPrefs = mock(SharedPreferences.class);
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        mRoutingOptionManager.overrideDefaultRoute(ROUTE_DEFAULT);
        mRoutingOptionManager.overrideDefaultIsoDepRoute(ROUTE_DEFAULT);
        mRoutingOptionManager.overrideDefaultOffHostRoute(ROUTE_DEFAULT);
        mRoutingOptionManager.overrideDefaultScRoute(ROUTE_DEFAULT);
        Field field = RoutingOptionManager.class.getDeclaredField("mPrefs");
        field.setAccessible(true);
        field.set(mRoutingOptionManager, mPrefs);
        when(mPrefs.edit()).thenReturn(editor);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);

        mRoutingOptionManager.overwriteRoutingTable();
        assertEquals(DEFAULT_ROUTE, mRoutingOptionManager.getDefaultRoute());
        assertEquals(DEFAULT_ISO_DEP_ROUTE, mRoutingOptionManager.getDefaultIsoDepRoute());
        assertEquals(DEFAULT_OFF_HOST_ROUTE, mRoutingOptionManager.getDefaultOffHostRoute());
        assertEquals(DEFAULT_SC_ROUTE, mRoutingOptionManager.getDefaultScRoute());
    }

    @Test
    public void testOverwriteRoutingTable()
            throws NoSuchFieldException, IllegalAccessException {
        SharedPreferences mPrefs = mock(SharedPreferences.class);
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        mRoutingOptionManager.overrideDefaultRoute(NDEF_NFCEE_ROUTE);
        mRoutingOptionManager.overrideDefaultIsoDepRoute(OVERRIDDEN_ISO_DEP_ROUTE);
        mRoutingOptionManager.overrideDefaultOffHostRoute(OVERRIDDEN_OFF_HOST_ROUTE);
        mRoutingOptionManager.overrideDefaultScRoute(DEFAULT_SC_ROUTE);
        Field field = RoutingOptionManager.class.getDeclaredField("mPrefs");
        field.setAccessible(true);
        field.set(mRoutingOptionManager, mPrefs);
        when(mPrefs.edit()).thenReturn(editor);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);

        mRoutingOptionManager.overwriteRoutingTable();
        assertEquals(NDEF_NFCEE_ROUTE, mRoutingOptionManager.getDefaultRoute());
        assertEquals(OVERRIDDEN_ISO_DEP_ROUTE, mRoutingOptionManager.getDefaultIsoDepRoute());
        assertEquals(OVERRIDDEN_OFF_HOST_ROUTE, mRoutingOptionManager.getDefaultOffHostRoute());
        assertEquals(DEFAULT_SC_ROUTE, mRoutingOptionManager.getDefaultScRoute());
        assertEquals(ROUTE_UNKNOWN, mRoutingOptionManager.getOverrideDefaultFelicaRoute());
    }

    @Test
    public void testReadRoutingOptionsFromPrefs()
            throws NoSuchFieldException, IllegalAccessException {
        String defaultRoute = "DefaultRoute";
        Context context = mock(Context.class);
        DeviceConfigFacade deviceConfigFacade = mock(DeviceConfigFacade.class);
        PackageManager packageManager = mock(PackageManager.class);
        SharedPreferences mPrefs = mock(SharedPreferences.class);
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        mRoutingOptionManager.overrideDefaultRoute(NDEF_NFCEE_ROUTE);
        Field field = RoutingOptionManager.class.getDeclaredField("mPrefs");
        field.setAccessible(true);
        field.set(mRoutingOptionManager, null);
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(mPrefs);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.hasSystemFeature(anyString())).thenReturn(true);
        when(mPrefs.edit()).thenReturn(editor);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);
        when(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor);
        when(deviceConfigFacade.getDefaultRoute()).thenReturn(defaultRoute);
        when(deviceConfigFacade.getDefaultIsoDepRoute()).thenReturn(defaultRoute);
        when(deviceConfigFacade.getDefaultOffHostRoute()).thenReturn(defaultRoute);
        when(deviceConfigFacade.getDefaultFelicaRoute()).thenReturn(defaultRoute);
        when(deviceConfigFacade.getDefaultScRoute()).thenReturn(defaultRoute);
        when(mPrefs.contains(KEY_DEFAULT_ROUTE)).thenReturn(false);
        when(mPrefs.contains(KEY_DEFAULT_ISO_DEP_ROUTE)).thenReturn(false);
        when(mPrefs.contains(KEY_DEFAULT_OFFHOST_ROUTE)).thenReturn(false);
        when(mPrefs.contains(KEY_DEFAULT_FELICA_ROUTE)).thenReturn(false);
        when(mPrefs.contains(KEY_DEFAULT_SC_ROUTE)).thenReturn(false);
        when(mPrefs.contains(KEY_AUTO_CHANGE_CAPABLE)).thenReturn(false);
        when(mPrefs.getString(KEY_DEFAULT_ROUTE, null)).thenReturn(defaultRoute);
        when(mPrefs.getString(KEY_DEFAULT_ISO_DEP_ROUTE, null)).thenReturn(defaultRoute);
        when(mPrefs.getString(KEY_DEFAULT_OFFHOST_ROUTE, null)).thenReturn(defaultRoute);
        when(mPrefs.getString(KEY_DEFAULT_FELICA_ROUTE, null)).thenReturn(defaultRoute);
        when(mPrefs.getString(KEY_DEFAULT_SC_ROUTE, null)).thenReturn(defaultRoute);
        when(mPrefs.getBoolean(KEY_AUTO_CHANGE_CAPABLE, true)).thenReturn(true);

        mRoutingOptionManager.readRoutingOptionsFromPrefs(context, deviceConfigFacade);
        assertTrue(mRoutingOptionManager.isAutoChangeEnabled());
        verify(mPrefs).contains(KEY_AUTO_CHANGE_CAPABLE);
    }

    @Test
    public void testSimSettings() {
        RoutingOptionManager.SimSettings simSettings = new RoutingOptionManager.SimSettings(2, 1);
        assertEquals("SIM1", simSettings.getName());

        simSettings.setType(TelephonyUtils.SIM_TYPE_UICC);
        assertEquals("SIM1", simSettings.getName());

        simSettings.setType(TelephonyUtils.SIM_TYPE_EUICC_1);
        assertEquals("SIM2", simSettings.getName());

        simSettings.setType(TelephonyUtils.SIM_TYPE_EUICC_2);
        assertEquals("SIM2", simSettings.getName());
    }
}
