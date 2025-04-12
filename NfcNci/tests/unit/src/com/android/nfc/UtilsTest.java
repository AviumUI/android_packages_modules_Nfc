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

import static android.Manifest.permission.BIND_NFC_SERVICE;
import static android.Manifest.permission.NFC;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilterProto;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.PatternMatcher;
import android.os.UserHandle;
import android.util.proto.ProtoOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class UtilsTest {
    @Mock
    Context context;

    @Mock
    PackageManager packageManager;

    @Mock
    PackageInfo packageInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetPackageNameFromIntent() {
        // Prepare data for the Intent
        Intent intent = mock(Intent.class);
        Uri uri = mock(Uri.class);
        doReturn("package").when(uri).getScheme();
        doReturn("com.example.app").when(uri).getSchemeSpecificPart();
        doReturn(uri).when(intent).getData();

        // Call the method and check the result
        String packageName = Utils.getPackageNameFromIntent(intent);
        assertTrue(packageName.equals("com.example.app"));
    }

    @Test
    public void testHasCeServicesWithValidPermissions()
            throws PackageManager.NameNotFoundException {
        // Prepare data for the test
        Intent intent = mock(Intent.class);
        Uri uri = mock(Uri.class);
        doReturn("package").when(uri).getScheme();
        doReturn("com.example.app").when(uri).getSchemeSpecificPart();
        doReturn(uri).when(intent).getData();

        // Mock PackageManager behavior
        doReturn(context).when(context).createPackageContextAsUser(
                anyString(), anyInt(), any(UserHandle.class));
        doReturn(packageManager).when(context).getPackageManager();
        doReturn(PackageManager.PERMISSION_GRANTED).when(packageManager)
                .checkPermission(NFC, "com.example.app");
        doReturn(packageInfo).when(packageManager).getPackageInfo("com.example.app",
                PackageManager.GET_PERMISSIONS
                        | PackageManager.GET_SERVICES
                        | PackageManager.MATCH_DISABLED_COMPONENTS);
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.permission = BIND_NFC_SERVICE;
        packageInfo.services = new ServiceInfo[]{serviceInfo};

        // Call the method and check the result
        boolean result = Utils.hasCeServicesWithValidPermissions(context, intent, 123);
        assertTrue(result);
    }

    @Test
    public void testArrayContainsWithNullArray() {
        assertFalse(Utils.arrayContains(null, 1));
    }

    @Test
    public void testArrayContains() {
        Integer[] array = {1, 2, 3, 4, 5, 6};
        int elem = 5;
        assertTrue(Utils.arrayContains(array, elem));
    }

    @Test
    public void testArrayContainsWithoutMatch() {
        Integer[] array = {1, 2, 3, 4, 5, 6};
        int elem = -1;
        assertFalse(Utils.arrayContains(array, elem));
    }

    @Test
    public void testNullInput() {
        assertEquals("", Utils.maskSubstring(null, 2));
    }

    @Test
    public void testStartGreaterThanLength() {
        assertEquals("abc", Utils.maskSubstring("abc", 5));
    }

    @Test
    public void testStartEqualToLength() {
        assertEquals("abc", Utils.maskSubstring("abc", 3));
    }

    @Test
    public void testStartIsZero() {
        assertEquals("***", Utils.maskSubstring("abc", 0));
    }

    @Test
    public void testMiddleStart() {
        assertEquals("ab***", Utils.maskSubstring("abcde", 2));
    }

    @Test
    public void testStartAtLastChar() {
        assertEquals("abcd*", Utils.maskSubstring("abcde", 4));
    }

    @Test
    public void testEmptyString() {
        assertEquals("", Utils.maskSubstring("", 0));
    }

    @Test
    public void testStartNegative() {
        try {
            Utils.maskSubstring("test", -1);
            fail("Expected StringIndexOutOfBoundsException");
        } catch (StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDumpDebugPendingIntent() {
        PendingIntent pendingIntent = mock(PendingIntent.class);
        ProtoOutputStream proto = mock(ProtoOutputStream.class);
        long fieldId = 123L;
        when(proto.start(fieldId)).thenReturn(fieldId);
        when(pendingIntent.toString()).thenReturn("pendingIntent");

        Utils.dumpDebugPendingIntent(pendingIntent, proto, fieldId);
        verify(proto).start(fieldId);
    }

    @Test
    public void testDumpDebugIntentFilter() {
        IntentFilter intentFilter = mock(IntentFilter.class);
        ProtoOutputStream proto = mock(ProtoOutputStream.class);
        long fieldId = 123L;
        PatternMatcher patternMatcher = mock(PatternMatcher.class);
        IntentFilter.AuthorityEntry authorityEntry = mock(IntentFilter.AuthorityEntry.class);
        when(proto.start(anyInt())).thenReturn(fieldId);
        when(intentFilter.countActions()).thenReturn(1);
        when(intentFilter.getAction(0)).thenReturn("test.action");
        when(intentFilter.countCategories()).thenReturn(1);
        when(intentFilter.getCategory(0)).thenReturn("test.category");
        when(intentFilter.countDataSchemes()).thenReturn(1);
        when(intentFilter.getDataScheme(0)).thenReturn("test.data");
        when(intentFilter.countDataSchemeSpecificParts()).thenReturn(1);
        when(intentFilter.getDataSchemeSpecificPart(0)).thenReturn(patternMatcher);
        when(patternMatcher.getPath()).thenReturn("test.path");
        when(patternMatcher.getType()).thenReturn(1);
        when(intentFilter.countDataAuthorities()).thenReturn(1);
        when(intentFilter.getDataAuthority(0)).thenReturn(authorityEntry);
        when(authorityEntry.getHost()).thenReturn("*.test.host");
        when(authorityEntry.getPort()).thenReturn(1);
        when(intentFilter.countDataPaths()).thenReturn(1);
        when(intentFilter.getDataPath(0)).thenReturn(patternMatcher);
        when(intentFilter.countDataTypes()).thenReturn(1);
        when(intentFilter.getDataType(0)).thenReturn("test.datatype");
        when(intentFilter.getPriority()).thenReturn(1);

        Utils.dumpDebugIntentFilter(intentFilter, proto, fieldId);
        verify(proto).write(IntentFilterProto.ACTIONS, "test.action");
        verify(authorityEntry).getHost();
        verify(proto).write(IntentFilterProto.PRIORITY, 1);
        verify(intentFilter, times(2)).getPriority();
    }
}
