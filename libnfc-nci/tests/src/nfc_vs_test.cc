//
// Copyright (C) 2025 The Android Open Source Project
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

#include "nfc_vs.h"

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include "mock_gki_utils.h"
#include "nfc_api.h"
#include "nfc_int.h"

class NfcVsTest : public ::testing::Test {
 protected:
  tNFC_CB mock_nfc_cb;
  void SetUp() override {
    memset(&mock_nfc_cb, 0, sizeof(mock_nfc_cb));
    gki_utils = new MockGkiUtils();
  }
  void TearDown() override { gki_utils = nullptr; }
};

TEST_F(NfcVsTest, SuccessfulRegistration) {
  //    nfc_cb = mock_nfc_cb;
  tNFC_VS_CBACK* mock_callback = [](tNFC_VS_EVT event, uint16_t /*data_len*/,
                                    uint8_t* /*p_data*/) { (void)event; };

  tNFC_STATUS status = NFC_RegVSCback(true, mock_callback);

  ASSERT_EQ(NFC_STATUS_OK, status);
  ASSERT_NE(nullptr, nfc_cb.p_vs_cb[0]);
  ASSERT_EQ(mock_callback, nfc_cb.p_vs_cb[0]);
}

TEST_F(NfcVsTest, FailedRegistrationWhenFull) {
  // Fill all slots
  for (auto& i : mock_nfc_cb.p_vs_cb) {
    i = [](tNFC_VS_EVT /*event*/, uint16_t /*data_len*/, uint8_t* /*p_data*/) {
    };
  }
  nfc_cb = mock_nfc_cb;

  tNFC_VS_CBACK* mock_callback =
      [](tNFC_VS_EVT /*event*/, uint16_t /*data_len*/, uint8_t* /*p_data*/) {};

  tNFC_STATUS status = NFC_RegVSCback(true, mock_callback);

  ASSERT_EQ(NFC_STATUS_FAILED, status);
}

TEST_F(NfcVsTest, SuccessfulDeregistration) {
  tNFC_VS_CBACK* mock_callback =
      [](tNFC_VS_EVT /*event*/, uint16_t /*data_len*/, uint8_t* /*p_data*/) {};
  mock_nfc_cb.p_vs_cb[0] = nullptr;
  mock_nfc_cb.p_vs_cb[1] = nullptr;
  mock_nfc_cb.p_vs_cb[2] = mock_callback;
  nfc_cb = mock_nfc_cb;
  tNFC_STATUS status = NFC_RegVSCback(false, mock_callback);

  ASSERT_EQ(NFC_STATUS_OK, status);
  ASSERT_EQ(nullptr, nfc_cb.p_vs_cb[2]);
}

TEST_F(NfcVsTest, FailedDeregistrationWhenNotFound) {
  tNFC_VS_CBACK* mock_callback1 =
      [](tNFC_VS_EVT /*event*/, uint16_t /*data_len*/, uint8_t* /*p_data*/) {};
  tNFC_VS_CBACK* mock_callback2 =
      [](tNFC_VS_EVT /*event*/, uint16_t /*data_len*/, uint8_t* /*p_data*/) {};
  mock_nfc_cb.p_vs_cb[0] = mock_callback1;
  nfc_cb = mock_nfc_cb;

  tNFC_STATUS status = NFC_RegVSCback(false, mock_callback2);

  ASSERT_EQ(NFC_STATUS_FAILED, status);
  ASSERT_EQ(mock_callback1, mock_nfc_cb.p_vs_cb[0]);
}

TEST_F(NfcVsTest, MultipleRegistrationsAndDeregistrations) {
  tNFC_VS_CBACK* mock_callback1 =
      [](tNFC_VS_EVT /*event*/, uint16_t /*data_len*/, uint8_t* /*p_data*/) {};
  tNFC_VS_CBACK* mock_callback2 =
      [](tNFC_VS_EVT /*event*/, uint16_t /*data_len*/, uint8_t* /*p_data*/) {};

  mock_nfc_cb.p_vs_cb[0] = nullptr;
  mock_nfc_cb.p_vs_cb[1] = nullptr;
  mock_nfc_cb.p_vs_cb[2] = nullptr;
  nfc_cb = mock_nfc_cb;
  // Register both callbacks
  ASSERT_EQ(NFC_STATUS_OK, NFC_RegVSCback(true, mock_callback1));
  ASSERT_EQ(NFC_STATUS_OK, NFC_RegVSCback(true, mock_callback2));

  // Deregister one callback
  ASSERT_EQ(NFC_STATUS_OK, NFC_RegVSCback(false, mock_callback1));
  ASSERT_NE(nullptr, nfc_cb.p_vs_cb[1]);
  ASSERT_EQ(mock_callback2, nfc_cb.p_vs_cb[1]);

  // Deregister the remaining callback
  ASSERT_EQ(NFC_STATUS_OK, NFC_RegVSCback(false, mock_callback2));
  ASSERT_EQ(nullptr, nfc_cb.p_vs_cb[1]);
}

TEST_F(NfcVsTest, NullPointerInput) {
  tNFC_STATUS status = NFC_SendRawVsCommand(nullptr, nullptr);
  ASSERT_EQ(NFC_STATUS_INVALID_PARAM, status);
}

TEST_F(NfcVsTest, InvalidLengthInput) {
  NFC_HDR p_data = {0, 0, 0, 0};
  p_data.len = NCI_MAX_VSC_SIZE + 1;

  tNFC_STATUS status = NFC_SendRawVsCommand(&p_data, nullptr);
  ASSERT_EQ(NFC_STATUS_INVALID_PARAM, status);
}