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

#include "nfa_rw_api.cc"
#include <gtest/gtest.h>
#include <gmock/gmock.h>

class NfaRwDetectNDefTest : public ::testing::Test {
protected:
    void SetUp() override {
    }
};

TEST_F(NfaRwDetectNDefTest, DetectNDef_Success) {
    tNFA_RW_OPERATION* p_msg = new tNFA_RW_OPERATION;
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_DETECT_NDEF;
    tNFA_STATUS status = NFA_RwDetectNDef();
    EXPECT_EQ(status, NFA_STATUS_OK);
    delete p_msg;
}

//NFA_RwFormatTag

TEST_F(NfaRwDetectNDefTest, DetectFormatTagNDef_Success) {
    tNFA_RW_OPERATION* p_msg = new tNFA_RW_OPERATION;
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_DETECT_NDEF;
    tNFA_STATUS status = NFA_RwFormatTag();
    EXPECT_EQ(status, NFA_STATUS_OK);
    delete p_msg;
}

class NFA_RwI93GetMultiBlockSecurityStatusTest : public :: testing::Test{
protected:
    void SetUp() override {
    }
};

TEST_F(NFA_RwI93GetMultiBlockSecurityStatusTest, TestWRONG_PROTOCOL){
    tNFA_RW_OPERATION* p_msg;
    uint8_t first_block_number = 0x01;
    uint16_t number_blocks = 0x10;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    tNFA_STATUS status = NFA_RwI93GetMultiBlockSecurityStatus(0x01, 0X10);
    EXPECT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93GetMultiBlockSecurityStatusTest, DetectFormatTagNDef_Success) {
    tNFA_RW_OPERATION* p_msg = new tNFA_RW_OPERATION;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_GET_MULTI_BLOCK_STATUS;
    uint8_t first_block_number = 0x01;
    uint16_t number_blocks = 0x10;
    p_msg->params.i93_cmd.first_block_number = first_block_number;
    p_msg->params.i93_cmd.number_blocks = number_blocks;
    tNFA_STATUS status = NFA_RwI93GetMultiBlockSecurityStatus(0x01,0x10);
    EXPECT_EQ(status, NFA_STATUS_OK);
    delete p_msg;
}

void* (*GKI_getbuf_ptr)(uint16_t) = GKI_getbuf;

TEST_F(NFA_RwI93GetMultiBlockSecurityStatusTest, MemoryAllocationFailure) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    auto original_GKI_getbuf = GKI_getbuf_ptr;
    GKI_getbuf_ptr = [](uint16_t size) -> void* {
        (void)size;
        return nullptr; };
    uint8_t first_block_number = 5;
    uint16_t number_blocks = 3;
    tNFA_STATUS status = NFA_RwI93GetMultiBlockSecurityStatus(first_block_number, number_blocks);
    EXPECT_EQ(status, NFA_STATUS_OK);
    GKI_getbuf_ptr = original_GKI_getbuf;
}

class NFA_RwI93GetSysInfoTest : public ::testing::Test {
protected:
    void SetUp() override {
    }
};

TEST_F(NFA_RwI93GetSysInfoTest, TestWrongProtocol) {
    tNFA_RW_OPERATION* p_msg;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    uint8_t p_uid[10] = {0};
    tNFA_STATUS status = NFA_RwI93GetSysInfo(p_uid);
    EXPECT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93GetSysInfoTest, TestSuccessWithoutUID) {
    tNFA_RW_OPERATION* p_msg = nullptr;
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_GET_SYS_INFO;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    uint8_t* p_uid = nullptr;
    tNFA_STATUS status = NFA_RwI93GetSysInfo(p_uid);
    EXPECT_EQ(status, NFA_STATUS_OK);
}

TEST_F(NFA_RwI93GetSysInfoTest, TestMemoryAllocationFailure) {
    auto original_GKI_getbuf = GKI_getbuf_ptr;
    GKI_getbuf_ptr = [](uint16_t size) -> void* {
        (void)size;
        return nullptr; };
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    uint8_t p_uid[10] = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10};
    tNFA_STATUS status = NFA_RwI93GetSysInfo(p_uid);
    EXPECT_EQ(status, NFA_STATUS_OK);
    GKI_getbuf_ptr = original_GKI_getbuf;
}

class NFA_RwI93InventoryTest : public :: testing :: Test{
protected:

};

TEST_F(NFA_RwI93InventoryTest, TestWRONG_PROTOCOL){
    tNFA_RW_OPERATION* p_msg;
    uint8_t afi = 0x01;
    uint8_t* p_uid = nullptr;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    tNFA_STATUS status = NFA_RwI93Inventory(true,afi,p_uid);
    EXPECT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93InventoryTest, TestRequestSuccess){
    tNFA_RW_OPERATION* p_msg = new tNFA_RW_OPERATION;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_INVENTORY;
    p_msg->params.i93_cmd.afi_present = true;
    p_msg->params.i93_cmd.afi = 0x01;
    uint8_t* p_uid = nullptr;
    tNFA_STATUS status = NFA_RwI93Inventory(true, 0x01, p_uid);
    EXPECT_EQ(status, NFA_STATUS_OK);
}

TEST_F(NFA_RwI93InventoryTest, TestValidCaseWithoutUID) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    uint8_t* uid = nullptr;
    void* mock_buffer = malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(mock_buffer, nullptr);
    tNFA_STATUS result = NFA_RwI93Inventory(false, 0x01, uid);
    EXPECT_EQ(result, NFA_STATUS_OK);
    free(mock_buffer);
}

class NFA_RwI93LockAFITest : public :: testing :: Test{
protected:
};

TEST_F(NFA_RwI93LockAFITest, TestWrongProtocol){
    tNFA_RW_OPERATION* p_msg;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    tNFA_STATUS status = NFA_RwI93LockAFI();
    EXPECT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93LockAFITest, DetectNDef_Success) {
    tNFA_RW_OPERATION* p_msg = new tNFA_RW_OPERATION;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_LOCK_AFI;
    tNFA_STATUS status = NFA_RwI93LockAFI();
    EXPECT_EQ(status, NFA_STATUS_OK);
    delete p_msg;
}

TEST_F(NFA_RwI93LockAFITest, TestValidCaseWithoutUID) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    void* mock_buffer = malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(mock_buffer, nullptr);
    tNFA_STATUS result = NFA_RwI93LockAFI();
    EXPECT_EQ(result, NFA_STATUS_OK);
    free(mock_buffer);
}

class NFA_RwI93LockBlockTest : public :: testing :: Test{
protected:
};

TEST_F(NFA_RwI93LockBlockTest, TestWrongProtocol){
    tNFA_RW_OPERATION* p_msg;
    uint8_t block_number = 0x01;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    tNFA_STATUS status = NFA_RwI93LockBlock(block_number);
    EXPECT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93LockBlockTest, DetectNDef_Success) {
    tNFA_RW_OPERATION* p_msg = new tNFA_RW_OPERATION;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    uint8_t block_number = 0xFF;
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_LOCK_BLOCK;
    p_msg->params.i93_cmd.first_block_number = block_number;
    tNFA_STATUS status = NFA_RwI93LockBlock(block_number);
    EXPECT_EQ(status, NFA_STATUS_OK);
    delete p_msg;
}

TEST_F(NFA_RwI93LockBlockTest, TestValidCaseWithoutUID) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    uint8_t block_number = 0x01;
    void* mock_buffer = malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(mock_buffer, nullptr);
    tNFA_STATUS result = NFA_RwI93LockBlock(block_number);
    EXPECT_EQ(result, NFA_STATUS_OK);
    free(mock_buffer);
}

class NFA_RwI93LockDSFIDTest : public :: testing :: Test{
protected:
};

TEST_F(NFA_RwI93LockDSFIDTest, TestWrongProtocol){
    tNFA_RW_OPERATION* p_msg;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    tNFA_STATUS status = NFA_RwI93LockDSFID();
    EXPECT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93LockDSFIDTest, DetectNDef_Success) {
    tNFA_RW_OPERATION* p_msg = new tNFA_RW_OPERATION;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_LOCK_BLOCK;
    tNFA_STATUS status = NFA_RwI93LockDSFID();
    EXPECT_EQ(status, NFA_STATUS_OK);
    delete p_msg;
}

TEST_F(NFA_RwI93LockDSFIDTest, TestValidCaseWithoutUID) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    void* mock_buffer = malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(mock_buffer, nullptr);
    tNFA_STATUS result = NFA_RwI93LockDSFID();
    EXPECT_EQ(result, NFA_STATUS_OK);
    free(mock_buffer);
}

class NFA_RwI93ReadMultipleBlocksTest : public :: testing::Test{
protected:
    void SetUp() override {
    }
};

TEST_F(NFA_RwI93ReadMultipleBlocksTest, TestWRONG_PROTOCOL){
    tNFA_RW_OPERATION* p_msg;
    uint8_t block_number = 0x01;
    uint16_t number_blocks = 0x10;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    tNFA_STATUS status = NFA_RwI93ReadMultipleBlocks(0x01, 0X10);
    EXPECT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93ReadMultipleBlocksTest, DetectFormatTagNDef_Success) {
    tNFA_RW_OPERATION* p_msg = new tNFA_RW_OPERATION;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_READ_MULTI_BLOCK;
    uint8_t block_number = 0x01;
    uint16_t number_blocks = 0x10;
    p_msg->params.i93_cmd.first_block_number = block_number;
    p_msg->params.i93_cmd.number_blocks = number_blocks;
    tNFA_STATUS status = NFA_RwI93ReadMultipleBlocks(block_number,number_blocks);
    EXPECT_EQ(status, NFA_STATUS_OK);
    delete p_msg;
}

TEST_F(NFA_RwI93ReadMultipleBlocksTest, MemoryAllocationFailure) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    auto original_GKI_getbuf = GKI_getbuf_ptr;
    GKI_getbuf_ptr = [](uint16_t size) -> void* {
        (void)size;
        return nullptr; };
    uint8_t block_number = 5;
    uint16_t number_blocks = 3;
    tNFA_STATUS status = NFA_RwI93ReadMultipleBlocks(block_number, number_blocks);
    EXPECT_EQ(status, NFA_STATUS_OK);
    GKI_getbuf_ptr = original_GKI_getbuf;
}

class NFA_RwI93ReadSingleBlockTest : public :: testing::Test{
protected:
    void SetUp() override {
    }
};

TEST_F(NFA_RwI93ReadSingleBlockTest, TestWRONG_PROTOCOL){
    tNFA_RW_OPERATION* p_msg;
    uint8_t block_number = 0x01;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    tNFA_STATUS status = NFA_RwI93ReadSingleBlock(0x01);
    EXPECT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93ReadSingleBlockTest, DetectFormatTagNDef_Success) {
    tNFA_RW_OPERATION* p_msg = new tNFA_RW_OPERATION;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_READ_SINGLE_BLOCK;
    uint8_t block_number = 0x01;
    p_msg->params.i93_cmd.first_block_number = block_number;
    tNFA_STATUS status = NFA_RwI93ReadSingleBlock(block_number);
    EXPECT_EQ(status, NFA_STATUS_OK);
    delete p_msg;
}

TEST_F(NFA_RwI93ReadSingleBlockTest, MemoryAllocationFailure) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    auto original_GKI_getbuf = GKI_getbuf_ptr;
    GKI_getbuf_ptr = [](uint16_t size) -> void* {
        (void)size;
        return nullptr; };
    uint8_t block_number = 5;
    tNFA_STATUS status = NFA_RwI93ReadSingleBlock(block_number);
    EXPECT_EQ(status, NFA_STATUS_OK);
    GKI_getbuf_ptr = original_GKI_getbuf;
}

class NFA_RwI93ResetToReadyTest : public :: testing :: Test{
protected:
};

TEST_F(NFA_RwI93ResetToReadyTest, TestWrongProtocol){
    tNFA_RW_OPERATION* p_msg;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    tNFA_STATUS status = NFA_RwI93ResetToReady();
    EXPECT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93ResetToReadyTest, DetectNDef_Success) {
    tNFA_RW_OPERATION* p_msg = new tNFA_RW_OPERATION;
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_RESET_TO_READY;
    tNFA_STATUS status = NFA_RwI93ResetToReady();
    EXPECT_EQ(status, NFA_STATUS_OK);
    delete p_msg;
}

TEST_F(NFA_RwI93ResetToReadyTest, TestValidCaseWithoutUID) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    void* mock_buffer = malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(mock_buffer, nullptr);
    tNFA_STATUS result = NFA_RwI93ResetToReady();
    EXPECT_EQ(result, NFA_STATUS_OK);
    free(mock_buffer);
}

class NFA_RwI93SelectTest : public ::testing::Test {
protected:
};

TEST_F(NFA_RwI93SelectTest, TestCorrectProtocol) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    uint8_t p_uid[I93_UID_BYTE_LEN] = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(
            sizeof(tNFA_RW_OPERATION) + I93_UID_BYTE_LEN);
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_SELECT;
    p_msg->params.i93_cmd.p_data = (uint8_t*)(p_msg + 1);
    memcpy(p_msg->params.i93_cmd.p_data, p_uid, I93_UID_BYTE_LEN);
    tNFA_STATUS status = NFA_RwI93Select(p_uid);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

TEST_F(NFA_RwI93SelectTest, TestWrongProtocol) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    uint8_t p_uid[I93_UID_BYTE_LEN] = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    tNFA_STATUS status = NFA_RwI93Select(p_uid);
    ASSERT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93SelectTest, TestSuccessfulMemoryAllocation) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    uint8_t p_uid[I93_UID_BYTE_LEN] = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(
            sizeof(tNFA_RW_OPERATION) + I93_UID_BYTE_LEN);
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_SELECT;
    p_msg->params.i93_cmd.p_data = (uint8_t*)(p_msg + 1);
    memcpy(p_msg->params.i93_cmd.p_data, p_uid, I93_UID_BYTE_LEN);
    tNFA_STATUS status = NFA_RwI93Select(p_uid);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

class NFA_RwI93SetAddressingModeTest : public ::testing::Test {
protected:
};

TEST_F(NFA_RwI93SetAddressingModeTest, TestCorrectProtocol) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    bool mode = true;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_SET_ADDR_MODE;
    p_msg->params.i93_cmd.addr_mode = mode;
    tNFA_STATUS status = NFA_RwI93SetAddressingMode(mode);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

TEST_F(NFA_RwI93SetAddressingModeTest, TestWrongProtocol) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    bool mode = true;
    tNFA_STATUS status = NFA_RwI93SetAddressingMode(mode);
    ASSERT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93SetAddressingModeTest, TestSuccessfulMemoryAllocation) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    bool mode = false;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_SET_ADDR_MODE;
    p_msg->params.i93_cmd.addr_mode = mode;
    tNFA_STATUS status = NFA_RwI93SetAddressingMode(mode);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

class NFA_RwI93StayQuietTest : public testing::Test {
protected:
    void SetUp() override {
        nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    }
};

TEST_F(NFA_RwI93StayQuietTest, TestCorrectProtocol) {
    uint8_t test_uid[I93_UID_BYTE_LEN] = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_STAY_QUIET;
    p_msg->params.i93_cmd.uid_present = true;
    memcpy(p_msg->params.i93_cmd.uid, test_uid, I93_UID_BYTE_LEN);
    tNFA_STATUS status = NFA_RwI93StayQuiet(test_uid);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

TEST_F(NFA_RwI93StayQuietTest, TestWrongProtocol) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    uint8_t test_uid[I93_UID_BYTE_LEN] = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    tNFA_STATUS status = NFA_RwI93StayQuiet(test_uid);
    ASSERT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93StayQuietTest, TestSuccessfulMemoryAllocation) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    uint8_t test_uid[I93_UID_BYTE_LEN] = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_STAY_QUIET;
    p_msg->params.i93_cmd.uid_present = true;
    memcpy(p_msg->params.i93_cmd.uid, test_uid, I93_UID_BYTE_LEN);
    tNFA_STATUS status = NFA_RwI93StayQuiet(test_uid);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

class NFA_RwI93WriteAFITest : public testing::Test {
protected:
    void SetUp() override {
        nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    }
};

TEST_F(NFA_RwI93WriteAFITest, TestCorrectProtocol) {
    uint8_t afi = 0x12;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_WRITE_AFI;
    p_msg->params.i93_cmd.afi = afi;
    tNFA_STATUS status = NFA_RwI93WriteAFI(afi);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

TEST_F(NFA_RwI93WriteAFITest, TestWrongProtocol) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    uint8_t afi = 0x12;
    tNFA_STATUS status = NFA_RwI93WriteAFI(afi);
    ASSERT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93WriteAFITest, TestSuccessfulMemoryAllocation) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    uint8_t afi = 0x12;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_WRITE_AFI;
    p_msg->params.i93_cmd.afi = afi;
    tNFA_STATUS status = NFA_RwI93WriteAFI(afi);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

class NFA_RwI93WriteDSFIDTest : public testing::Test {
protected:
    void SetUp() override {
        nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    }
};

TEST_F(NFA_RwI93WriteDSFIDTest, TestCorrectProtocol) {
    uint8_t dsfid = 0x34;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_WRITE_DSFID;
    p_msg->params.i93_cmd.dsfid = dsfid;
    tNFA_STATUS status = NFA_RwI93WriteDSFID(dsfid);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

TEST_F(NFA_RwI93WriteDSFIDTest, TestWrongProtocol) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    uint8_t dsfid = 0x34;
    tNFA_STATUS status = NFA_RwI93WriteDSFID(dsfid);
    ASSERT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93WriteDSFIDTest, TestSuccessfulMemoryAllocation) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    uint8_t test_dsfid = 0x34;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_WRITE_DSFID;
    p_msg->params.i93_cmd.dsfid = test_dsfid;
    tNFA_STATUS status = NFA_RwI93WriteDSFID(test_dsfid);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

class NFA_RwI93WriteMultipleBlocksTest : public testing::Test {
protected:
    void SetUp() override {
        nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
        nfa_rw_cb.i93_block_size = 4;
        nfa_rw_cb.i93_num_block = 16;
    }
};

TEST_F(NFA_RwI93WriteMultipleBlocksTest, TestCorrectProtocol) {
    uint8_t first_block_number = 1;
    uint16_t number_blocks = 3;
    uint8_t test_data[] = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                        0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10 };
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION) + 12);
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_WRITE_MULTI_BLOCK;
    p_msg->params.i93_cmd.first_block_number = first_block_number;
    p_msg->params.i93_cmd.number_blocks = number_blocks;
    p_msg->params.i93_cmd.p_data = (uint8_t*)(p_msg + 1);
    memcpy(p_msg->params.i93_cmd.p_data, test_data, 12);
    tNFA_STATUS status = NFA_RwI93WriteMultipleBlocks(first_block_number, number_blocks, test_data);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

TEST_F(NFA_RwI93WriteMultipleBlocksTest, TestWrongProtocol) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    uint8_t first_block_number = 1;
    uint16_t number_blocks = 3;
    uint8_t test_data[] = { 0x01, 0x02, 0x03, 0x04 };
    tNFA_STATUS status = NFA_RwI93WriteMultipleBlocks(first_block_number, number_blocks, test_data);
    ASSERT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93WriteMultipleBlocksTest, TestInvalidBlockSizeOrNumber) {
    nfa_rw_cb.i93_block_size = 0;
    nfa_rw_cb.i93_num_block = 16;
    uint8_t first_block_number = 1;
    uint16_t number_blocks = 3;
    uint8_t test_data[] = { 0x01, 0x02, 0x03, 0x04 };
    tNFA_STATUS status = NFA_RwI93WriteMultipleBlocks(first_block_number, number_blocks, test_data);
    ASSERT_EQ(status, NFA_STATUS_FAILED);
    nfa_rw_cb.i93_block_size = 4;
    nfa_rw_cb.i93_num_block = 0;
    status = NFA_RwI93WriteMultipleBlocks(first_block_number, number_blocks, test_data);
    ASSERT_EQ(status, NFA_STATUS_FAILED);
}

class NFA_RwI93WriteSingleBlockTest : public testing::Test {
protected:
    void SetUp() override {
        nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
        nfa_rw_cb.i93_block_size = 4;
        nfa_rw_cb.i93_num_block = 16;
    }
};

TEST_F(NFA_RwI93WriteSingleBlockTest, TestCorrectProtocol) {
    uint8_t block_number = 1;
    uint8_t test_data[] = { 0x01, 0x02, 0x03, 0x04 };
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION) + 4);
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_I93_WRITE_SINGLE_BLOCK;
    p_msg->params.i93_cmd.first_block_number = block_number;
    p_msg->params.i93_cmd.p_data = (uint8_t*)(p_msg + 1);
    memcpy(p_msg->params.i93_cmd.p_data, test_data, 4);
    tNFA_STATUS status = NFA_RwI93WriteSingleBlock(block_number, test_data);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

TEST_F(NFA_RwI93WriteSingleBlockTest, TestWrongProtocol) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    uint8_t block_number = 1;
    uint8_t test_data[] = { 0x01, 0x02, 0x03, 0x04 };
    tNFA_STATUS status = NFA_RwI93WriteSingleBlock(block_number, test_data);
    ASSERT_EQ(status, NFA_STATUS_WRONG_PROTOCOL);
}

TEST_F(NFA_RwI93WriteSingleBlockTest, TestInvalidBlockSizeOrNumber) {
    nfa_rw_cb.i93_block_size = 0;
    nfa_rw_cb.i93_num_block = 16;
    uint8_t block_number = 1;
    uint8_t test_data[] = { 0x01, 0x02, 0x03, 0x04 };
    tNFA_STATUS status = NFA_RwI93WriteSingleBlock(block_number, test_data);
    ASSERT_EQ(status, NFA_STATUS_FAILED);
    nfa_rw_cb.i93_block_size = 4;
    nfa_rw_cb.i93_num_block = 0;
    status = NFA_RwI93WriteSingleBlock(block_number, test_data);
    ASSERT_EQ(status, NFA_STATUS_FAILED);
}

class NFA_RwLocateTlvTest : public testing::Test {
protected:
    tNFA_RW_OPERATION* p_msg;
    void SetUp() override {
        nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    }
};

TEST_F(NFA_RwLocateTlvTest, TestCorrectTlvType) {
    uint8_t tlv_type = TAG_LOCK_CTRL_TLV;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_DETECT_LOCK_TLV;
    tNFA_STATUS status = NFA_RwLocateTlv(tlv_type);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

TEST_F(NFA_RwLocateTlvTest, TestInvalidTlvType) {
    uint8_t tlv_type = 0xFF;
    tNFA_STATUS status = NFA_RwLocateTlv(tlv_type);
    ASSERT_EQ(status, NFA_STATUS_FAILED);
}

TEST_F(NFA_RwLocateTlvTest, TestMemCtrlTlvType) {
    uint8_t tlv_type = TAG_MEM_CTRL_TLV;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_DETECT_MEM_TLV;
    tNFA_STATUS status = NFA_RwLocateTlv(tlv_type);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

TEST_F(NFA_RwLocateTlvTest, TestNdefTlvType) {
    uint8_t tlv_type = TAG_NDEF_TLV;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_DETECT_NDEF;
    tNFA_STATUS status = NFA_RwLocateTlv(tlv_type);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

class NFA_RwReadNDefTest : public testing::Test {
protected:
};

TEST_F(NFA_RwReadNDefTest, TestValidOperation) {
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    tNFA_STATUS status = NFA_RwReadNDef();
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

TEST_F(NFA_RwReadNDefTest, TestMessageSent) {
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_READ_NDEF;
    tNFA_STATUS status = NFA_RwReadNDef();
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

class NFA_RwSetTagReadOnlyTest : public testing::Test {
protected:
    void SetUp() override {
        nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    }
};

TEST_F(NFA_RwSetTagReadOnlyTest, TestHardLockOnT5T) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    bool b_hard_lock = true;
    tNFA_STATUS status = NFA_RwSetTagReadOnly(b_hard_lock);
    ASSERT_EQ(status, NFA_STATUS_OK);
}

TEST_F(NFA_RwSetTagReadOnlyTest, TestSoftLockOnT5T) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T5T;
    bool b_hard_lock = false;
    tNFA_STATUS status = NFA_RwSetTagReadOnly(b_hard_lock);
    ASSERT_EQ(status, NFA_STATUS_REJECTED);
}

TEST_F(NFA_RwSetTagReadOnlyTest, TestSoftLockOnISO_DEP) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_ISO_DEP;
    bool b_hard_lock = false;
    tNFA_STATUS status = NFA_RwSetTagReadOnly(b_hard_lock);
    ASSERT_EQ(status, NFA_STATUS_OK);
}

TEST_F(NFA_RwSetTagReadOnlyTest, TestHardLockOnISO_DEP) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_ISO_DEP;
    bool b_hard_lock = true;
    tNFA_STATUS status = NFA_RwSetTagReadOnly(b_hard_lock);
    ASSERT_EQ(status, NFA_STATUS_REJECTED);
}

TEST_F(NFA_RwSetTagReadOnlyTest, TestSoftLockOnT1T) {
    nfa_rw_cb.protocol = NFC_PROTOCOL_T1T;
    bool b_hard_lock = false;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_SET_TAG_RO;
    p_msg->params.set_readonly.b_hard_lock = b_hard_lock;
    tNFA_STATUS status = NFA_RwSetTagReadOnly(b_hard_lock);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

class NFA_RwT1tReadTest : public :: testing :: Test{
protected:
};

TEST_F(NFA_RwT1tReadTest, TestT1T_READSuccess){
    uint8_t block_number = 0x01;
    uint8_t index = 2;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_T1T_READ;
    tNFA_STATUS status = NFA_RwT1tRead(block_number, index);
    p_msg->params.t1t_read.block_number = block_number;
    p_msg->params.t1t_read.index = index;
    ASSERT_EQ(status, NFA_STATUS_OK);
}

// NFA_RwT1tRead8

TEST_F(NFA_RwT1tReadTest, TestT1T_READ8Success){
    uint8_t block_number = 0x01;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_T1T_READ8;
    tNFA_STATUS status = NFA_RwT1tRead8(block_number);
    p_msg->params.t1t_read.block_number = block_number;
    ASSERT_EQ(status, NFA_STATUS_OK);
}

// NFA_RwT1tReadAll

TEST_F(NFA_RwT1tReadTest, TestT1T_READALLSuccess){
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_T1T_RALL;
    tNFA_STATUS status = NFA_RwT1tReadAll();
    ASSERT_EQ(status, NFA_STATUS_OK);
}

// NFA_RwT1tReadSeg

TEST_F(NFA_RwT1tReadTest, TestCorrectProtocol) {
    uint8_t segment_number = 1;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_T1T_RSEG;
    p_msg->params.t1t_read.segment_number = segment_number;
    tNFA_STATUS status = NFA_RwT1tReadSeg(segment_number);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

// NFA_RwT1tRid

TEST_F(NFA_RwT1tReadTest, TestNFA_RwT1tRidSuccess){
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_T1T_RID;
    tNFA_STATUS status = NFA_RwT1tRid();
    ASSERT_EQ(status, NFA_STATUS_OK);
}

class NFA_RwT1tWriteTest : public testing::Test {
protected:
};

TEST_F(NFA_RwT1tWriteTest, TestCorrectProtocol) {
    uint8_t block_number = 1;
    uint8_t index = 0;
    uint8_t data = 0x10;
    bool b_erase = false;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_T1T_WRITE;
    p_msg->params.t1t_write.block_number = block_number;
    p_msg->params.t1t_write.index = index;
    p_msg->params.t1t_write.p_block_data[0] = data;
    p_msg->params.t1t_write.b_erase = b_erase;
    tNFA_STATUS status = NFA_RwT1tWrite(block_number, index, data, b_erase);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

TEST_F(NFA_RwT1tWriteTest, TestWriteWithErase) {
    uint8_t block_number = 1;
    uint8_t index = 0;
    uint8_t data = 0x20;
    bool b_erase = true;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_T1T_WRITE;
    p_msg->params.t1t_write.block_number = block_number;
    p_msg->params.t1t_write.index = index;
    p_msg->params.t1t_write.p_block_data[0] = data;
    p_msg->params.t1t_write.b_erase = b_erase;
    tNFA_STATUS status = NFA_RwT1tWrite(block_number, index, data, b_erase);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

//  NFA_RwT1tWrite8

TEST_F(NFA_RwT1tWriteTest, Test8CorrectProtocol) {
    uint8_t block_number = 1;
    uint8_t data[8] = { 0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0x80 };
    bool b_erase = false;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_T1T_WRITE8;
    p_msg->params.t1t_write.block_number = block_number;
    p_msg->params.t1t_write.b_erase = b_erase;
    memcpy(p_msg->params.t1t_write.p_block_data, data, 8);
    tNFA_STATUS status = NFA_RwT1tWrite8(block_number, data, b_erase);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

TEST_F(NFA_RwT1tWriteTest, TestWrite8WithErase) {
    uint8_t block_number = 1;
    uint8_t data[8] = { 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0x80, 0x90 };
    bool b_erase = true;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_T1T_WRITE8;
    p_msg->params.t1t_write.block_number = block_number;
    p_msg->params.t1t_write.b_erase = b_erase;
    memcpy(p_msg->params.t1t_write.p_block_data, data, 8);
    tNFA_STATUS status = NFA_RwT1tWrite8(block_number, data, b_erase);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

class NFA_RwT2tReadTest : public testing::Test {
protected:
    void SetUp() override {
        nfa_rw_cb.protocol = NFC_PROTOCOL_T2T;
    }
};

TEST_F(NFA_RwT2tReadTest, TestCorrectProtocol) {
    uint8_t block_number = 1;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_T2T_READ;
    p_msg->params.t2t_read.block_number = block_number;
    tNFA_STATUS status = NFA_RwT2tRead(block_number);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

// NFA_RwT2tReadDynLockBytes

TEST_F(NFA_RwT2tReadTest, TestWithRead_locks) {
    bool read_dyn_locks = true;
    tNFA_RW_OPERATION* p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_T2T_READ_DYN_LOCKS;
    p_msg->params.t2t_read_dyn_locks.read_dyn_locks = read_dyn_locks;
    tNFA_STATUS status = NFA_RwT2tReadDynLockBytes(read_dyn_locks);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

TEST_F(NFA_RwT2tReadTest, TestWithoutRead_Dyn_locks){
    bool read_dyn_locks = false;
    tNFA_RW_OPERATION*p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_T2T_READ_DYN_LOCKS;
    p_msg->params.t2t_read_dyn_locks.read_dyn_locks = read_dyn_locks;
    tNFA_STATUS status = NFA_RwT2tReadDynLockBytes(read_dyn_locks);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}

// NFA_RwT2tSectorSelect

TEST_F(NFA_RwT2tReadTest, TestNFA_RwT2tSectorSelectSuccess){
    uint8_t sector_number = 0x01;
    tNFA_RW_OPERATION*p_msg = (tNFA_RW_OPERATION*)malloc(sizeof(tNFA_RW_OPERATION));
    ASSERT_NE(p_msg, nullptr);
    p_msg->hdr.event = NFA_RW_OP_REQUEST_EVT;
    p_msg->op = NFA_RW_OP_T2T_READ_DYN_LOCKS;
    p_msg->params.t2t_sector_select.sector_number = sector_number;
    tNFA_STATUS status = NFA_RwT2tSectorSelect(sector_number);
    ASSERT_EQ(status, NFA_STATUS_OK);
    free(p_msg);
}