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

package android.nfc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.os.Parcel;

import org.junit.Before;
import org.junit.Test;

public class EntryTest {
    private final String mSampleEntry = "SampleEntry";
    private final byte mType = 1;
    private final byte mNfceeId = 2;
    private final String mSampleRoutingType = "SampleRoutingType";
    private final byte mPowerState = 1;
    private Entry mEntry;

    @Before
    public void setUp() {
        mEntry = new Entry(mSampleEntry, mType, mNfceeId, mSampleRoutingType, mPowerState);
    }

    @Test
    public void testConstructorAndGetters() {
        assertEquals(mSampleEntry, mEntry.getEntry());
        assertEquals(mType, mEntry.getType());
        assertEquals(mNfceeId, mEntry.getNfceeId());
        assertEquals(mSampleRoutingType, mEntry.getRoutingType());
    }

    @Test
    public void testParcelableWriteAndRead() {
        Parcel parcel = Parcel.obtain();
        mEntry.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Entry createdFromParcel = Entry.CREATOR.createFromParcel(parcel);
        assertNotNull(createdFromParcel);
        assertEquals(mEntry.getEntry(), createdFromParcel.getEntry());
        assertEquals(mEntry.getType(), createdFromParcel.getType());
        assertEquals(mEntry.getNfceeId(), createdFromParcel.getNfceeId());
        assertEquals(mEntry.getRoutingType(), createdFromParcel.getRoutingType());
        parcel.recycle();
    }

    @Test
    public void testParcelableWithEmptyFields() {
        String entry = "";
        byte type = 0;
        byte nfceeId = 0;
        String routingType = "";
        byte powerState = 1;
        Entry original = new Entry(entry, type, nfceeId, routingType, powerState);
        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Entry createdFromParcel = Entry.CREATOR.createFromParcel(parcel);
        assertNotNull(createdFromParcel);
        assertEquals(original.getEntry(), createdFromParcel.getEntry());
        assertEquals(original.getType(), createdFromParcel.getType());
        assertEquals(original.getNfceeId(), createdFromParcel.getNfceeId());
        assertEquals(original.getRoutingType(), createdFromParcel.getRoutingType());
        parcel.recycle();
    }

    @Test
    public void testDescribeContents() {
        assertEquals(0, mEntry.describeContents());
    }

    @Test
    public void testParcelableCreatorArray() {
        Entry[] array = Entry.CREATOR.newArray(3);
        assertNotNull(array);
        assertEquals(3, array.length);
    }
}
