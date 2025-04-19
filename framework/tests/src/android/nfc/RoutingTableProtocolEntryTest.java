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

import static android.nfc.cardemulation.CardEmulation.PROTOCOL_AND_TECHNOLOGY_ROUTE_DH;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RoutingTableProtocolEntryTest {
    @Test
    public void testConstructorAndGetProtocol() {
        int nfceeId = 1;
        int protocolValue = RoutingTableProtocolEntry.PROTOCOL_ISO_DEP;
        int routeType = PROTOCOL_AND_TECHNOLOGY_ROUTE_DH;
        int powerState = 1;

        RoutingTableProtocolEntry entry = new RoutingTableProtocolEntry(nfceeId, protocolValue,
                routeType, powerState);
        assertEquals(protocolValue, entry.getProtocol());
        assertEquals(nfceeId, entry.getNfceeId());
        assertEquals(routeType, entry.getRouteType());
    }

    @Test
    public void testProtocolStringToInt() {
        assertEquals(RoutingTableProtocolEntry.PROTOCOL_T1T,
                RoutingTableProtocolEntry.protocolStringToInt("PROTOCOL_T1T"));
        assertEquals(RoutingTableProtocolEntry.PROTOCOL_T2T,
                RoutingTableProtocolEntry.protocolStringToInt("PROTOCOL_T2T"));
        assertEquals(RoutingTableProtocolEntry.PROTOCOL_T3T,
                RoutingTableProtocolEntry.protocolStringToInt("PROTOCOL_T3T"));
        assertEquals(RoutingTableProtocolEntry.PROTOCOL_ISO_DEP,
                RoutingTableProtocolEntry.protocolStringToInt("PROTOCOL_ISO_DEP"));
        assertEquals(RoutingTableProtocolEntry.PROTOCOL_NFC_DEP,
                RoutingTableProtocolEntry.protocolStringToInt("PROTOCOL_NFC_DEP"));
        assertEquals(RoutingTableProtocolEntry.PROTOCOL_T5T,
                RoutingTableProtocolEntry.protocolStringToInt("PROTOCOL_T5T"));
        assertEquals(RoutingTableProtocolEntry.PROTOCOL_NDEF,
                RoutingTableProtocolEntry.protocolStringToInt("PROTOCOL_NDEF"));
        assertEquals(RoutingTableProtocolEntry.PROTOCOL_UNDETERMINED,
                RoutingTableProtocolEntry.protocolStringToInt("PROTOCOL_UNDETERMINED"));
        assertEquals(RoutingTableProtocolEntry.PROTOCOL_UNSUPPORTED,
                RoutingTableProtocolEntry.protocolStringToInt("INVALID_PROTOCOL"));
    }
}
