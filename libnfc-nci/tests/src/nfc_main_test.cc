//
// Copyright (C) 2024 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include "mock_gki_utils.h"
#include "nfc_int.h"

TEST(NfcGetStatusNameTest, TestGetStatusName) {
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_OK), "OK");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_REJECTED), "REJECTED");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_MSG_CORRUPTED), "CORRUPTED");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_BUFFER_FULL), "BUFFER_FULL");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_FAILED), "FAILED");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_NOT_INITIALIZED), "NOT_INITIALIZED");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_SYNTAX_ERROR), "SYNTAX_ERROR");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_SEMANTIC_ERROR), "SEMANTIC_ERROR");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_UNKNOWN_GID), "UNKNOWN_GID");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_UNKNOWN_OID), "UNKNOWN_OID");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_INVALID_PARAM), "INVALID_PARAM");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_MSG_SIZE_TOO_BIG), "MSG_SIZE_TOO_BIG");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_ALREADY_STARTED), "ALREADY_STARTED");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_ACTIVATION_FAILED),
            "ACTIVATION_FAILED");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_TEAR_DOWN), "TEAR_DOWN");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_RF_TRANSMISSION_ERR),
            "RF_TRANSMISSION_ERR");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_RF_PROTOCOL_ERR), "RF_PROTOCOL_ERR");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_TIMEOUT), "TIMEOUT");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_EE_INTF_ACTIVE_FAIL),
            "EE_INTF_ACTIVE_FAIL");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_EE_TRANSMISSION_ERR),
            "EE_TRANSMISSION_ERR");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_EE_PROTOCOL_ERR), "EE_PROTOCOL_ERR");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_EE_TIMEOUT), "EE_TIMEOUT");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_CMD_STARTED), "CMD_STARTED");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_HW_TIMEOUT), "HW_TIMEOUT");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_CONTINUE), "CONTINUE");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_REFUSED), "REFUSED");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_BAD_RESP), "BAD_RESP");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_CMD_NOT_CMPLTD), "CMD_NOT_CMPLTD");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_NO_BUFFERS), "NO_BUFFERS");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_WRONG_PROTOCOL), "WRONG_PROTOCOL");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_BUSY), "BUSY");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_LINK_LOSS), "LINK_LOSS");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_BAD_LENGTH), "BAD_LENGTH");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_BAD_HANDLE), "BAD_HANDLE");
  ASSERT_EQ(NFC_GetStatusName(NFC_STATUS_CONGESTED), "CONGESTED");

  // Unknown Status Value
  ASSERT_EQ(NFC_GetStatusName((tNFC_STATUS)100), "UNKNOWN");
}

class NfcConnCreateTest : public ::testing::Test {
 protected:
  void SetUp() override { gki_utils = new MockGkiUtils(); }

  void TearDown() override { gki_utils = nullptr; }
};

static void mock_callback(__attribute__((unused)) uint8_t,
                          __attribute__((unused)) tNFC_CONN_EVT,
                          __attribute__((unused)) tNFC_CONN*) {}

TEST_F(NfcConnCreateTest, NfceeSuccess) {
  uint8_t dest_type = NCI_DEST_TYPE_NFCEE;
  uint8_t id = 0x01;
  uint8_t protocol = 0x02;
  tNFC_CONN_CBACK* p_cback = mock_callback;
  nfc_cb.conn_cb[0].conn_id = NFC_ILLEGAL_CONN_ID;
  nfc_cb.max_conn = 1;
  EXPECT_CALL(*((MockGkiUtils*)gki_utils), getpoolbuf(::testing::_))
      .WillOnce(testing::Return(new NFC_HDR()));

  tNFC_STATUS status = NFC_ConnCreate(dest_type, id, protocol, p_cback);

  ASSERT_EQ(status, NFC_STATUS_OK);
}

TEST_F(NfcConnCreateTest, RemoteSuccess) {
  uint8_t dest_type = NCI_DEST_TYPE_REMOTE;
  uint8_t id = 0x03;
  uint8_t protocol = 0x04;
  nfc_cb.conn_cb[0].conn_id = NFC_ILLEGAL_CONN_ID;
  tNFC_CONN_CBACK* p_cback = mock_callback;
  nfc_cb.max_conn = 1;
  EXPECT_CALL(*((MockGkiUtils*)gki_utils), getpoolbuf(::testing::_))
      .WillOnce(testing::Return(new NFC_HDR()));

  tNFC_STATUS status = NFC_ConnCreate(dest_type, id, protocol, p_cback);

  ASSERT_EQ(status, NFC_STATUS_OK);
}

TEST_F(NfcConnCreateTest, NfccSuccess) {
  uint8_t dest_type = NCI_DEST_TYPE_NFCC;
  uint8_t id = 0x05;
  uint8_t protocol = 0x06;
  tNFC_CONN_CBACK* p_cback = mock_callback;
  nfc_cb.conn_cb[0].conn_id = NFC_ILLEGAL_CONN_ID;
  nfc_cb.max_conn = 1;
  EXPECT_CALL(*((MockGkiUtils*)gki_utils), getpoolbuf(::testing::_))
      .WillOnce(testing::Return(new NFC_HDR()));

  tNFC_STATUS status = NFC_ConnCreate(dest_type, id, protocol, p_cback);

  ASSERT_EQ(status, NFC_STATUS_OK);
}

class NfcMainTest : public ::testing::Test {
 protected:
  void SetUp() override { gki_utils = new MockGkiUtils(); }

  void TearDown() override { gki_utils = nullptr; }
};

TEST_F(NfcMainTest, FlushData) {
  uint8_t conn_id = NFC_PEND_CONN_ID;
  uint8_t bytes1[] = {0x01};
  uint8_t bytes2[] = {0x02};
  void* buf1 = &bytes1;
  void* buf2 = &bytes2;
  nfc_cb.conn_cb[0].conn_id = NFC_PEND_CONN_ID;

  EXPECT_CALL(*((MockGkiUtils*)gki_utils), dequeue(testing::_))
      .WillOnce(testing::Return(buf1))
      .WillOnce(testing::Return(buf2))
      .WillOnce(testing::Return(nullptr));

  EXPECT_CALL(*((MockGkiUtils*)gki_utils), freebuf(buf1));
  EXPECT_CALL(*((MockGkiUtils*)gki_utils), freebuf(buf2));

  tNFC_STATUS status = NFC_FlushData(conn_id);

  ASSERT_EQ(status, NFC_STATUS_OK);
}

TEST_F(NfcMainTest, PowerCycleInvalidStateFailure) {
  nfc_cb.nfc_state = NFC_STATE_NONE;
  tNFC_STATUS status = NFC_PowerCycleNFCC();
  ASSERT_EQ(status, NFC_STATUS_FAILED);
}