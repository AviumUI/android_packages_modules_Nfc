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

#include "nfa_hci_main.cc"

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include "mock_gki_utils.h"
#include "nfa_sys_main.cc"
#include "nfc_main.cc"

void ResetNfaHciCb() {
    nfa_hci_cb.msg_len = 0;
    nfa_hci_cb.assembly_failed = false;
    memset(nfa_hci_cb.p_msg_data, 0, nfa_hci_cb.max_msg_len);
}

class NfaHciAssembleMsgTest : public testing::Test {
protected:
    void SetUp() override {
        nfa_hci_cb.max_msg_len = 1024;
        nfa_hci_cb.p_msg_data = new uint8_t[nfa_hci_cb.max_msg_len];
        ResetNfaHciCb();
    }
    void TearDown() override {
        delete[] nfa_hci_cb.p_msg_data;
    }
};

TEST_F(NfaHciAssembleMsgTest, NormalReassembly) {
    uint8_t test_data[] = {0x01, 0x02, 0x03};
    uint16_t data_len = sizeof(test_data);
    nfa_hci_assemble_msg(test_data, data_len);
    EXPECT_EQ(nfa_hci_cb.msg_len, data_len);
    EXPECT_FALSE(nfa_hci_cb.assembly_failed);
    EXPECT_EQ(memcmp(nfa_hci_cb.p_msg_data, test_data, data_len), 0);
}

TEST_F(NfaHciAssembleMsgTest, BufferOverflow) {
    uint8_t test_data[] = {0xFF, 0xEE, 0xDD, 0xCC};
    uint16_t data_len = nfa_hci_cb.max_msg_len + 10;
    nfa_hci_assemble_msg(test_data, data_len);
    EXPECT_EQ(nfa_hci_cb.msg_len, nfa_hci_cb.max_msg_len);
    EXPECT_TRUE(nfa_hci_cb.assembly_failed);
}

TEST_F(NfaHciAssembleMsgTest, PartialReassembly) {
    uint8_t test_data[] = {0xAA, 0xBB, 0xCC};
    nfa_hci_cb.msg_len = nfa_hci_cb.max_msg_len - 1;
    nfa_hci_assemble_msg(test_data, sizeof(test_data));
    EXPECT_EQ(nfa_hci_cb.msg_len, nfa_hci_cb.max_msg_len);
    EXPECT_TRUE(nfa_hci_cb.assembly_failed);
    EXPECT_EQ(nfa_hci_cb.p_msg_data[nfa_hci_cb.max_msg_len - 1], 0xAA);
}

TEST_F(NfaHciAssembleMsgTest, EmptyData) {
    uint8_t* test_data = nullptr;
    uint16_t data_len = 0;
    nfa_hci_assemble_msg(test_data, data_len);
    EXPECT_EQ(nfa_hci_cb.msg_len, 0);
    EXPECT_FALSE(nfa_hci_cb.assembly_failed);
}

TEST_F(NfaHciAssembleMsgTest, AppendToExistingData) {
    uint8_t initial_data[] = {0x11, 0x22};
    uint8_t new_data[] = {0x33, 0x44};
    memcpy(nfa_hci_cb.p_msg_data, initial_data, sizeof(initial_data));
    nfa_hci_cb.msg_len = sizeof(initial_data);
    nfa_hci_assemble_msg(new_data, sizeof(new_data));
    EXPECT_EQ(nfa_hci_cb.msg_len, sizeof(initial_data) + sizeof(new_data));
    EXPECT_FALSE(nfa_hci_cb.assembly_failed);
    EXPECT_EQ(memcmp(nfa_hci_cb.p_msg_data, initial_data, sizeof(initial_data)), 0);
    EXPECT_EQ(memcmp(nfa_hci_cb.p_msg_data + sizeof(initial_data), new_data, sizeof(new_data)), 0);
}

class NfaHciIsValidCfgTest : public testing::Test {
protected:
    void SetUp() override {
        memset(&nfa_hci_cb, 0, sizeof(nfa_hci_cb));
    }
};

TEST_F(NfaHciIsValidCfgTest, ValidConfiguration) {
    strncpy(nfa_hci_cb.cfg.reg_app_names[0], "App1", NFA_MAX_HCI_APP_NAME_LEN);
    nfa_hci_cb.cfg.b_send_conn_evts[0] = true;
    nfa_hci_cb.cfg.dyn_gates[0].gate_id = NFA_HCI_LOOP_BACK_GATE;
    nfa_hci_cb.cfg.dyn_gates[0].pipe_inx_mask = 0x01;
    nfa_hci_cb.cfg.dyn_gates[0].gate_owner = 0;
    nfa_hci_cb.cfg.dyn_pipes[0].pipe_id = NFA_HCI_FIRST_DYNAMIC_PIPE;
    nfa_hci_cb.cfg.dyn_pipes[0].pipe_state = NFA_HCI_PIPE_OPENED;
    nfa_hci_cb.cfg.dyn_pipes[0].local_gate = NFA_HCI_LOOP_BACK_GATE;
    nfa_hci_cb.cfg.dyn_pipes[0].dest_gate = NFA_HCI_LOOP_BACK_GATE;
    nfa_hci_cb.cfg.admin_gate.pipe01_state = NFA_HCI_PIPE_OPENED;
    nfa_hci_cb.cfg.link_mgmt_gate.pipe00_state = NFA_HCI_PIPE_OPENED;
    nfa_hci_cb.cfg.id_mgmt_gate.pipe_inx_mask = 0x01;
    EXPECT_FALSE(nfa_hci_is_valid_cfg());
}

TEST_F(NfaHciIsValidCfgTest, InvalidAppNameLength) {
    memset(nfa_hci_cb.cfg.reg_app_names[0], 'A', NFA_MAX_HCI_APP_NAME_LEN + 1);
    EXPECT_FALSE(nfa_hci_is_valid_cfg());
}

TEST_F(NfaHciIsValidCfgTest, DuplicateAppNames) {
    strncpy(nfa_hci_cb.cfg.reg_app_names[0], "App1", NFA_MAX_HCI_APP_NAME_LEN);
    strncpy(nfa_hci_cb.cfg.reg_app_names[1], "App1", NFA_MAX_HCI_APP_NAME_LEN);
    EXPECT_FALSE(nfa_hci_is_valid_cfg());
}

TEST_F(NfaHciIsValidCfgTest, InvalidConnectivityEventFlag) {
    strncpy(nfa_hci_cb.cfg.reg_app_names[0], "App1", NFA_MAX_HCI_APP_NAME_LEN);
    nfa_hci_cb.cfg.b_send_conn_evts[0] = 2; // Invalid value
    EXPECT_TRUE(nfa_hci_is_valid_cfg());
}

TEST_F(NfaHciIsValidCfgTest, InvalidGateId) {
    nfa_hci_cb.cfg.dyn_gates[0].gate_id = 0xFF;
    EXPECT_TRUE(nfa_hci_is_valid_cfg());
}

TEST_F(NfaHciIsValidCfgTest, DuplicateGateIds) {
    nfa_hci_cb.cfg.dyn_gates[0].gate_id = NFA_HCI_LOOP_BACK_GATE;
    nfa_hci_cb.cfg.dyn_gates[1].gate_id = NFA_HCI_LOOP_BACK_GATE;
    EXPECT_FALSE(nfa_hci_is_valid_cfg());
}

TEST_F(NfaHciIsValidCfgTest, InvalidPipeState) {
    nfa_hci_cb.cfg.dyn_pipes[0].pipe_id = NFA_HCI_FIRST_DYNAMIC_PIPE;
    nfa_hci_cb.cfg.dyn_pipes[0].pipe_state = 0xFF;
    EXPECT_FALSE(nfa_hci_is_valid_cfg());
}

TEST_F(NfaHciIsValidCfgTest, InvalidAdminGatePipeState) {
    nfa_hci_cb.cfg.admin_gate.pipe01_state = 0xFF;
    EXPECT_FALSE(nfa_hci_is_valid_cfg());
}

TEST_F(NfaHciIsValidCfgTest, InvalidLinkMgmtGatePipeState) {
    nfa_hci_cb.cfg.link_mgmt_gate.pipe00_state = 0xFF;
    EXPECT_FALSE(nfa_hci_is_valid_cfg());
}

TEST_F(NfaHciIsValidCfgTest, InvalidPipeInIdentityManagementGate) {
    nfa_hci_cb.cfg.id_mgmt_gate.pipe_inx_mask = 0x01;
    nfa_hci_cb.cfg.dyn_pipes[0].pipe_id = NFA_HCI_FIRST_DYNAMIC_PIPE;
    nfa_hci_cb.cfg.dyn_pipes[0].local_gate = 0xFF;
    EXPECT_FALSE(nfa_hci_is_valid_cfg());
}

TEST_F(NfaHciIsValidCfgTest, DuplicatePipeIds) {
    nfa_hci_cb.cfg.dyn_pipes[0].pipe_id = NFA_HCI_FIRST_DYNAMIC_PIPE;
    nfa_hci_cb.cfg.dyn_pipes[1].pipe_id = NFA_HCI_FIRST_DYNAMIC_PIPE;
    EXPECT_FALSE(nfa_hci_is_valid_cfg());
}

class NfaHciProcNfccPowerModeTest : public testing::Test {
protected:
    void SetUp() override {
        memset(&nfa_hci_cb, 0, sizeof(nfa_hci_cb));
    }
};

TEST_F(NfaHciProcNfccPowerModeTest, FullPowerModeWhenIdle) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_IDLE;
    nfa_hci_cb.num_nfcee = 1;
    nfa_hci_proc_nfcc_power_mode(NFA_DM_PWR_MODE_FULL);
    EXPECT_EQ(nfa_hci_cb.b_low_power_mode, false);
    EXPECT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_RESTORE);
    EXPECT_EQ(nfa_hci_cb.ee_disc_cmplt, false);
    EXPECT_EQ(nfa_hci_cb.ee_disable_disc, true);
    EXPECT_EQ(nfa_hci_cb.w4_hci_netwk_init, false);
    EXPECT_EQ(nfa_hci_cb.conn_id, 0);
    EXPECT_EQ(nfa_hci_cb.num_ee_dis_req_ntf, 0);
    EXPECT_EQ(nfa_hci_cb.num_hot_plug_evts, 0);
}

TEST_F(NfaHciProcNfccPowerModeTest, FullPowerModeWhenNotIdle) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_RESTORE;
    nfa_hci_proc_nfcc_power_mode(NFA_DM_PWR_MODE_FULL);
}

TEST_F(NfaHciProcNfccPowerModeTest, NonFullPowerMode) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_IDLE;
    nfa_hci_cb.num_nfcee = 1;
    nfa_hci_proc_nfcc_power_mode(0);
    EXPECT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_IDLE);
    EXPECT_EQ(nfa_hci_cb.w4_rsp_evt, false);
    EXPECT_EQ(nfa_hci_cb.conn_id, 0);
    EXPECT_EQ(nfa_hci_cb.b_low_power_mode, true);
}

TEST_F(NfaHciProcNfccPowerModeTest, FullPowerModeWhenMultipleNfcee) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_IDLE;
    nfa_hci_cb.num_nfcee = 2;
    nfa_hci_proc_nfcc_power_mode(NFA_DM_PWR_MODE_FULL);
    EXPECT_EQ(nfa_hci_cb.w4_hci_netwk_init, true);
}

TEST_F(NfaHciProcNfccPowerModeTest, FullPowerModeWhenSingleNfcee) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_IDLE;
    nfa_hci_cb.num_nfcee = 1;
    nfa_hci_proc_nfcc_power_mode(NFA_DM_PWR_MODE_FULL);
    EXPECT_EQ(nfa_hci_cb.w4_hci_netwk_init, false);
}

class NfaHciRspTimeoutTest : public ::testing::Test {
protected:
    void SetUp() override {
        memset(&nfa_hci_cb, 0, sizeof(nfa_hci_cb));
    }
};

TEST_F(NfaHciRspTimeoutTest, TestStartupState) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_STARTUP;
    nfa_hci_rsp_timeout();
    ASSERT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_DISABLED);
}

TEST_F(NfaHciRspTimeoutTest, TestNetworkEnableStateWithSuccess) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_WAIT_NETWK_ENABLE;
    nfa_hci_cb.w4_hci_netwk_init = true;
    nfa_hci_rsp_timeout();
    ASSERT_FALSE(nfa_hci_cb.w4_hci_netwk_init);
}

TEST_F(NfaHciRspTimeoutTest, TestNetworkEnableStateWithFailure) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_WAIT_NETWK_ENABLE;
    nfa_hci_cb.w4_hci_netwk_init = false;
    nfa_hci_rsp_timeout();
    ASSERT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_DISABLED);
}

TEST_F(NfaHciRspTimeoutTest, TestRemoveGateState) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_REMOVE_GATE;
    nfa_hci_cb.cmd_sent = NFA_HCI_ADM_DELETE_PIPE;
    nfa_hci_rsp_timeout();
}

TEST_F(NfaHciRspTimeoutTest, TestAppDeregisterState) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_APP_DEREGISTER;
    nfa_hci_cb.cmd_sent = NFA_HCI_ADM_DELETE_PIPE;
    nfa_hci_rsp_timeout();
}

TEST_F(NfaHciRspTimeoutTest, TestInvalidState) {
    nfa_hci_cb.hci_state = static_cast<tNFA_HCI_STATE>(-1);
    nfa_hci_rsp_timeout();
    ASSERT_EQ(nfa_hci_cb.hci_state, static_cast<tNFA_HCI_STATE>(-1));
}

class NfaHciSetReceiveBufTest : public ::testing::Test {
protected:
    void SetUp() override {
        nfa_hci_cb.p_msg_data = nullptr;
        nfa_hci_cb.max_msg_len = 0;
        nfa_hci_cb.rsp_buf_size = 0;
        nfa_hci_cb.p_rsp_buf = nullptr;
        nfa_hci_cb.type = 0;
    }
};

TEST_F(NfaHciSetReceiveBufTest, PipeNotInRange) {
    uint8_t pipe = 0;
    nfa_hci_set_receive_buf(pipe);
    EXPECT_EQ(nfa_hci_cb.p_msg_data, nfa_hci_cb.msg_data);
    EXPECT_EQ(nfa_hci_cb.max_msg_len, NFA_MAX_HCI_EVENT_LEN);
}

TEST_F(NfaHciSetReceiveBufTest, PipeInRangeButWrongType) {
    uint8_t pipe = NFA_HCI_FIRST_DYNAMIC_PIPE;
    nfa_hci_cb.type = 1;
    nfa_hci_set_receive_buf(pipe);
    EXPECT_EQ(nfa_hci_cb.p_msg_data, nfa_hci_cb.msg_data);
    EXPECT_EQ(nfa_hci_cb.max_msg_len, NFA_MAX_HCI_EVENT_LEN);
}

TEST_F(NfaHciSetReceiveBufTest, PipeInRangeWithNoResponseBuffer) {
    uint8_t pipe = NFA_HCI_FIRST_DYNAMIC_PIPE;
    nfa_hci_cb.type = NFA_HCI_EVENT_TYPE;
    nfa_hci_cb.rsp_buf_size = 0;
    nfa_hci_set_receive_buf(pipe);
    EXPECT_EQ(nfa_hci_cb.p_msg_data, nfa_hci_cb.msg_data);
    EXPECT_EQ(nfa_hci_cb.max_msg_len, NFA_MAX_HCI_EVENT_LEN);
}

TEST_F(NfaHciSetReceiveBufTest, PipeInRangeWithRspBufSizeZeroAndNullRspBuf) {
    uint8_t pipe = NFA_HCI_FIRST_DYNAMIC_PIPE;
    nfa_hci_cb.type = NFA_HCI_EVENT_TYPE;
    nfa_hci_cb.rsp_buf_size = 10;
    nfa_hci_cb.p_rsp_buf = nullptr;
    nfa_hci_set_receive_buf(pipe);
    EXPECT_EQ(nfa_hci_cb.p_msg_data, nfa_hci_cb.msg_data);
    EXPECT_EQ(nfa_hci_cb.max_msg_len, NFA_MAX_HCI_EVENT_LEN);
}

TEST_F(NfaHciSetReceiveBufTest, PipeInRangeWithValidRspBuf) {
    uint8_t pipe = NFA_HCI_FIRST_DYNAMIC_PIPE;
    nfa_hci_cb.type = NFA_HCI_EVENT_TYPE;
    nfa_hci_cb.rsp_buf_size = 10;
    uint8_t rsp_buf[10] = {0};
    nfa_hci_cb.p_rsp_buf = rsp_buf;
    nfa_hci_set_receive_buf(pipe);
    EXPECT_EQ(nfa_hci_cb.p_msg_data, rsp_buf);
    EXPECT_EQ(nfa_hci_cb.max_msg_len, 10);
}

TEST_F(NfaHciSetReceiveBufTest, PipeInRangeWithValidRspBufOfDifferentSize) {
    uint8_t pipe = NFA_HCI_FIRST_DYNAMIC_PIPE;
    nfa_hci_cb.type = NFA_HCI_EVENT_TYPE;
    nfa_hci_cb.rsp_buf_size = 20;
    uint8_t rsp_buf[20] = {0};
    nfa_hci_cb.p_rsp_buf = rsp_buf;
    nfa_hci_set_receive_buf(pipe);
    EXPECT_EQ(nfa_hci_cb.p_msg_data, rsp_buf);
    EXPECT_EQ(nfa_hci_cb.max_msg_len, 20);
}

class NfaHciStartupTest : public ::testing::Test {
protected:
    void SetUp() override {
        memset(&nfa_hci_cb, 0, sizeof(nfa_hci_cb));
    }
};

TEST_F(NfaHciStartupTest, TestLoopbackDebugOn) {
    HCI_LOOPBACK_DEBUG = NFA_HCI_DEBUG_ON;
    nfa_hci_startup();
    ASSERT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_DISABLED);
}

TEST_F(NfaHciStartupTest, TestNvRamNotRead) {
    nfa_hci_cb.nv_read_cmplt = false;
    nfa_hci_cb.ee_disc_cmplt = true;
    nfa_hci_cb.conn_id = 0;
    nfa_hci_startup();
    ASSERT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_DISABLED);
}

TEST_F(NfaHciStartupTest, TestEeDiscNotComplete) {
    nfa_hci_cb.nv_read_cmplt = true;
    nfa_hci_cb.ee_disc_cmplt = false;
    nfa_hci_cb.conn_id = 0;
    nfa_hci_startup();
    ASSERT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_DISABLED);
}

TEST_F(NfaHciStartupTest, TestConnIdNotZero) {
    nfa_hci_cb.nv_read_cmplt = true;
    nfa_hci_cb.ee_disc_cmplt = true;
    nfa_hci_cb.conn_id = 1;
    nfa_hci_startup();
    ASSERT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_DISABLED);
}

TEST_F(NfaHciStartupTest, TestNoHciAccessInterfaceFound) {
    nfa_hci_cb.nv_read_cmplt = true;
    nfa_hci_cb.ee_disc_cmplt = true;
    nfa_hci_cb.conn_id = 0;
    nfa_hci_cb.num_nfcee = 1;
    nfa_hci_cb.ee_info[0].ee_interface[0] = 0;
    nfa_hci_startup();
    ASSERT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_DISABLED);
}

TEST_F(NfaHciStartupTest, TestHciAccessInterfaceFoundButInactive) {
    nfa_hci_cb.nv_read_cmplt = true;
    nfa_hci_cb.ee_disc_cmplt = true;
    nfa_hci_cb.conn_id = 0;
    nfa_hci_cb.num_nfcee = 1;
    nfa_hci_cb.ee_info[0].ee_interface[0] = NFA_EE_INTERFACE_HCI_ACCESS;
    nfa_hci_cb.ee_info[0].ee_status = NFA_EE_STATUS_INACTIVE;
    nfa_hci_startup();
    ASSERT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_DISABLED);
}

TEST_F(NfaHciStartupTest, TestFailedConnCreate) {
    nfa_hci_cb.nv_read_cmplt = true;
    nfa_hci_cb.ee_disc_cmplt = true;
    nfa_hci_cb.conn_id = 0;
    nfa_hci_cb.num_nfcee = 1;
    nfa_hci_cb.ee_info[0].ee_interface[0] = NFA_EE_INTERFACE_HCI_ACCESS;
    nfa_hci_cb.ee_info[0].ee_status = NFA_EE_STATUS_ACTIVE;
    nfa_hci_startup();
    ASSERT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_DISABLED);
}

TEST_F(NfaHciStartupTest, TestSuccessStartup) {
    nfa_hci_cb.nv_read_cmplt = true;
    nfa_hci_cb.ee_disc_cmplt = true;
    nfa_hci_cb.conn_id = 0;
    nfa_hci_cb.num_nfcee = 1;
    nfa_hci_cb.ee_info[0].ee_interface[0] = NFA_EE_INTERFACE_HCI_ACCESS;
    nfa_hci_cb.ee_info[0].ee_status = NFA_EE_STATUS_ACTIVE;
    nfa_hci_startup();
    ASSERT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_DISABLED);
}

class NfaHciEeInfoCbackTest : public ::testing::Test {
protected:
    void SetUp() override {
        nfa_hci_cb.hci_state = NFA_HCI_STATE_STARTUP;
        nfa_hci_cb.num_nfcee = 1;
        nfa_hci_cb.num_ee_dis_req_ntf = 0;
        nfa_hci_cb.num_hot_plug_evts = 0;
        nfa_hci_cb.conn_id = 0;
        nfa_hci_cb.ee_disable_disc = false;
        nfa_hci_cb.ee_disc_cmplt = false;
        nfa_hci_cb.w4_hci_netwk_init = false;
        nfa_hci_cb.timer = {};
    }
};

TEST_F(NfaHciEeInfoCbackTest, TestEEStatusOn) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_STARTUP;
    nfa_hci_ee_info_cback(NFA_EE_DISC_STS_ON);
    EXPECT_TRUE(nfa_hci_cb.ee_disc_cmplt);
    EXPECT_EQ(nfa_hci_cb.num_ee_dis_req_ntf, 0);
    EXPECT_EQ(nfa_hci_cb.num_hot_plug_evts, 0);
    EXPECT_EQ(nfa_hci_cb.conn_id, 0);
}

TEST_F(NfaHciEeInfoCbackTest, TestEEStatusOff) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_WAIT_NETWK_ENABLE;
    nfa_hci_cb.num_nfcee = 2;
    nfa_hci_cb.num_ee_dis_req_ntf = 1;
    nfa_hci_cb.num_hot_plug_evts = 1;
    nfa_hci_ee_info_cback(NFA_EE_DISC_STS_OFF);
    EXPECT_TRUE(nfa_hci_cb.ee_disable_disc);
}

TEST_F(NfaHciEeInfoCbackTest, TestEEStatusOffNoUiccHost) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_WAIT_NETWK_ENABLE;
    nfa_hci_cb.num_nfcee = 1;
    nfa_hci_ee_info_cback(NFA_EE_DISC_STS_OFF);
    EXPECT_FALSE(nfa_hci_cb.w4_hci_netwk_init);
}

TEST_F(NfaHciEeInfoCbackTest, TestEEStatusReq) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_WAIT_NETWK_ENABLE;
    nfa_hci_cb.num_ee_dis_req_ntf = 1;
    nfa_hci_cb.num_nfcee = 2;
    nfa_hci_ee_info_cback(NFA_EE_DISC_STS_REQ);
    EXPECT_EQ(nfa_hci_cb.num_ee_dis_req_ntf, 2);
}

TEST_F(NfaHciEeInfoCbackTest, TestEEStatusRecoveryRediscovered) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_WAIT_NETWK_ENABLE;
    nfa_hci_ee_info_cback(NFA_EE_RECOVERY_REDISCOVERED);
    EXPECT_EQ(nfa_hci_cb.num_nfcee, 0);
}

TEST_F(NfaHciEeInfoCbackTest, TestEEStatusModeSetComplete) {
    nfa_hci_cb.hci_state = NFA_HCI_STATE_WAIT_NETWK_ENABLE;
    nfa_hci_ee_info_cback(NFA_EE_MODE_SET_COMPLETE);
    EXPECT_EQ(nfa_hci_cb.num_nfcee, 0);
}

TEST_F(NfaHciEeInfoCbackTest, TestEEStatusRecoveryInit) {
  tNFA_DM_CB nfa_dm_cb_mock;
  nfa_dm_cb_mock = tNFA_DM_CB{};
  nfa_dm_cb_mock.disc_cb = tNFA_DM_DISC_CB{};
  nfa_dm_cb_mock.disc_cb.disc_flags = 0xFFFF;
  nfa_dm_cb = nfa_dm_cb_mock;
  nfa_hci_ee_info_cback(NFA_EE_RECOVERY_INIT);
  EXPECT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_EE_RECOVERY);
  EXPECT_TRUE(nfa_ee_cb.isDiscoveryStopped);
}

class NfaHciSysDisableTest : public ::testing::Test {
protected:
};

TEST_F(NfaHciSysDisableTest, TestConnIdZero) {
    nfa_hci_cb.conn_id = 0;
    nfa_hci_cb.hci_state = NFA_HCI_STATE_STARTUP;
    nfa_hci_sys_disable();
    EXPECT_EQ(nfa_hci_cb.conn_id, 0);
    EXPECT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_DISABLED);
}

TEST_F(NfaHciSysDisableTest, TestStateUpdate) {
    nfa_hci_cb.conn_id = 1;
    nfa_hci_cb.hci_state = NFA_HCI_STATE_STARTUP;
    nfa_hci_sys_disable();
    EXPECT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_DISABLED);
}

TEST_F(NfaHciSysDisableTest, TestGracefulDisableFalse) {
    nfa_hci_cb.conn_id = 42;
    nfa_hci_cb.hci_state = NFA_HCI_STATE_STARTUP;
    nfa_sys_cb.graceful_disable = false;
    uint8_t initial_conn_id = nfa_hci_cb.conn_id;
    uint8_t initial_state = nfa_hci_cb.hci_state;
    nfa_hci_sys_disable();
    EXPECT_EQ(nfa_hci_cb.conn_id, 0);
    EXPECT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_DISABLED);
}

TEST_F(NfaHciSysDisableTest, TestGracefulDisableWithNciVersion1_0) {
    nfa_hci_cb.conn_id = 42;
    nfa_hci_cb.hci_state = NFA_HCI_STATE_STARTUP;
    nfa_sys_cb.graceful_disable = true;
    nfc_cb.nci_version = NCI_VERSION_1_0;
    nfa_hci_sys_disable();
    EXPECT_EQ(nfa_hci_cb.conn_id, 42);
    EXPECT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_STARTUP);
}

class NfaHciHandleNvReadTest : public ::testing::Test {
protected:
    void SetUp() override {
        memset(&nfa_hci_cb, 0, sizeof(nfa_hci_cb));
        nfa_hci_cb.cfg.admin_gate.session_id[0] = 0x00;
        nfa_hci_cb.timer = {};
    }
    uint8_t default_session[NFA_HCI_SESSION_ID_LEN] = {
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};
    uint8_t reset_session[NFA_HCI_SESSION_ID_LEN] = {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
};

TEST_F(NfaHciHandleNvReadTest, StatusOkValidConfig) {
    uint8_t block = DH_NV_BLOCK;
    tNFA_STATUS status = NFA_STATUS_OK;
    memcpy(nfa_hci_cb.cfg.admin_gate.session_id, reset_session, NFA_HCI_SESSION_ID_LEN);
    nfa_hci_handle_nv_read(block, status);
    EXPECT_TRUE(nfa_hci_cb.nv_read_cmplt);
    EXPECT_TRUE(nfa_hci_cb.b_hci_netwk_reset);
}

TEST_F(NfaHciHandleNvReadTest, StatusNotOk) {
    uint8_t block = DH_NV_BLOCK;
    tNFA_STATUS status = NFA_STATUS_FAILED;
    nfa_hci_handle_nv_read(block, status);
    EXPECT_TRUE(nfa_hci_cb.b_hci_netwk_reset);
}

TEST_F(NfaHciHandleNvReadTest, InvalidConfig) {
    uint8_t block = DH_NV_BLOCK;
    tNFA_STATUS status = NFA_STATUS_OK;
    uint8_t invalid_session[NFA_HCI_SESSION_ID_LEN] = {
            0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01};
    memcpy(nfa_hci_cb.cfg.admin_gate.session_id, invalid_session, NFA_HCI_SESSION_ID_LEN);
    nfa_hci_handle_nv_read(block, status);
    EXPECT_FALSE(nfa_hci_cb.b_hci_netwk_reset);
}

TEST_F(NfaHciHandleNvReadTest, SessionIdIsDefaultSession) {
    uint8_t block = DH_NV_BLOCK;
    tNFA_STATUS status = NFA_STATUS_OK;
    memcpy(nfa_hci_cb.cfg.admin_gate.session_id, default_session, NFA_HCI_SESSION_ID_LEN);
    nfa_hci_handle_nv_read(block, status);
    EXPECT_TRUE(nfa_hci_cb.b_hci_netwk_reset);
}
TEST_F(NfaHciHandleNvReadTest, SessionIdIsResetSession) {
    uint8_t block = DH_NV_BLOCK;
    tNFA_STATUS status = NFA_STATUS_OK;
    memcpy(nfa_hci_cb.cfg.admin_gate.session_id, reset_session, NFA_HCI_SESSION_ID_LEN);
    nfa_hci_handle_nv_read(block, status);
    EXPECT_TRUE(nfa_hci_cb.b_hci_netwk_reset);
}

class NfaHciEvtHdlrTest : public ::testing::Test {
protected:
    void SetUp() override {
        memset(&nfa_hci_cb, 0, sizeof(nfa_hci_cb));
        gki_utils = new MockGkiUtils();
    }
    void TearDown() override { gki_utils = nullptr; }
};

TEST_F(NfaHciEvtHdlrTest, EventInApiRequestRange) {
    NFC_HDR msg;
    msg.event = NFA_HCI_FIRST_API_EVENT + 1;
    msg.len = 0;
    nfa_hci_evt_hdlr(&msg);
    EXPECT_EQ(nfa_hci_cb.hci_api_q.count, 0);
}

TEST_F(NfaHciEvtHdlrTest, NvWriteEvent) {
    NFC_HDR msg;
    msg.event = NFA_HCI_RSP_NV_WRITE_EVT;
    msg.len = 0;
    nfa_hci_evt_hdlr(&msg);
    EXPECT_EQ(nfa_hci_cb.nv_write_needed, false);
}

TEST_F(NfaHciEvtHdlrTest, EventGreaterThanLastApiEvent) {
    NFC_HDR msg;
    msg.event = NFA_HCI_LAST_API_EVENT + 1;
    msg.len = 0;
    nfa_hci_evt_hdlr(&msg);
}

TEST_F(NfaHciEvtHdlrTest, NvWriteNeededInIdleState) {
    NFC_HDR msg;
    msg.event = NFA_HCI_RSP_NV_READ_EVT;
    msg.len = 0;
    nfa_hci_cb.hci_state = NFA_HCI_STATE_IDLE;
    nfa_hci_cb.nv_write_needed = true;
    nfa_hci_evt_hdlr(&msg);
    EXPECT_EQ(nfa_hci_cb.nv_write_needed, false);
}

TEST_F(NfaHciEvtHdlrTest, NoHostResetting) {
    NFC_HDR msg;
    msg.event = NFA_HCI_RSP_NV_READ_EVT;
    msg.len = 0;
    nfa_hci_cb.hci_state = NFA_HCI_STATE_IDLE;
    nfa_hci_cb.nv_write_needed = true;
    nfa_hci_evt_hdlr(&msg);
    EXPECT_EQ(nfa_hci_cb.nv_write_needed, false);
}

class NfaHciInitTest : public ::testing::Test {
protected:
    void SetUp() override {
        memset(&nfa_hci_cb, 0, sizeof(nfa_hci_cb));
    }
};

TEST_F(NfaHciInitTest, ControlBlockInitialization) {
    nfa_hci_init();
    EXPECT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_STARTUP);
    EXPECT_EQ(nfa_hci_cb.num_nfcee, NFA_HCI_MAX_HOST_IN_NETWORK);
}

class NfaHciRestoreDefaultConfigTest : public ::testing::Test {
protected:
    void SetUp() override {
        memset(&nfa_hci_cb, 0, sizeof(nfa_hci_cb));
    }
    void TearDown() override {
        memset(&nfa_hci_cb, 0, sizeof(nfa_hci_cb));
    }
};
TEST_F(NfaHciRestoreDefaultConfigTest, SessionIdCopy) {
    uint8_t session_id[NFA_HCI_SESSION_ID_LEN] = {1, 2, 3, 4, 5, 6, 7, 8};
    nfa_hci_restore_default_config(session_id);
    for (size_t i = 0; i < NFA_HCI_SESSION_ID_LEN; ++i) {
        EXPECT_EQ(nfa_hci_cb.cfg.admin_gate.session_id[i], session_id[i]);
    }
}

TEST_F(NfaHciRestoreDefaultConfigTest, NvWriteNeededFlag) {
    uint8_t session_id[NFA_HCI_SESSION_ID_LEN] = {1, 2, 3, 4, 5, 6, 7, 8};
    nfa_hci_restore_default_config(session_id);
    EXPECT_TRUE(nfa_hci_cb.nv_write_needed);
}

class NfaHciEnableOneNfceeTest : public :: testing :: Test{
    void SetUp() override{
        memset(&nfa_hci_cb, 0, sizeof(nfa_hci_cb));
        memset(&nfa_ee_cb, 0, sizeof(nfa_ee_cb));
    }
};

TEST_F(NfaHciEnableOneNfceeTest, ActivateInactiveNFCEE) {
    nfa_hci_cb.num_nfcee = 1;
    nfa_hci_cb.ee_info[0].ee_status = NFA_EE_STATUS_INACTIVE;
    nfa_hci_cb.ee_info[0].ee_handle = 0x01;
    nfa_hci_enable_one_nfcee();
    EXPECT_EQ(nfa_hci_cb.ee_info[0].ee_status, NFC_MODE_ACTIVATE);
}

TEST_F(NfaHciEnableOneNfceeTest, NoActionWhenAllNfceesActive) {
    nfa_hci_cb.num_nfcee = 2;
    nfa_hci_cb.ee_info[0].ee_status = NFA_EE_STATUS_ACTIVE;
    nfa_hci_cb.ee_info[1].ee_status = NFA_EE_STATUS_ACTIVE;
    nfa_hci_enable_one_nfcee();
    EXPECT_EQ(nfa_hci_cb.num_nfcee, 2);
    EXPECT_EQ(nfa_hci_cb.ee_info[0].ee_status, NFA_EE_STATUS_ACTIVE);
    EXPECT_EQ(nfa_hci_cb.ee_info[1].ee_status, NFA_EE_STATUS_ACTIVE);
}

TEST_F(NfaHciEnableOneNfceeTest, WaitNetworkEnableState) {
    nfa_hci_cb.num_nfcee = 0;
    nfa_hci_cb.hci_state = NFA_HCI_STATE_WAIT_NETWK_ENABLE;
    nfa_hci_enable_one_nfcee();
    EXPECT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_WAIT_NETWK_ENABLE);
}

TEST_F(NfaHciEnableOneNfceeTest, RestoreNetworkEnableState) {
    nfa_hci_cb.num_nfcee = 0;
    nfa_hci_cb.hci_state = NFA_HCI_STATE_RESTORE_NETWK_ENABLE;
    nfa_hci_enable_one_nfcee();
    EXPECT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_RESTORE_NETWK_ENABLE);
}

TEST_F(NfaHciEnableOneNfceeTest, EeRecoveryState) {
    nfa_hci_cb.num_nfcee = 0;
    nfa_hci_cb.hci_state = NFA_HCI_STATE_EE_RECOVERY;
    nfa_ee_cb.isDiscoveryStopped = false;
    nfa_hci_enable_one_nfcee();
    EXPECT_EQ(nfa_hci_cb.hci_state, NFA_HCI_STATE_IDLE);
    EXPECT_FALSE(nfa_ee_cb.isDiscoveryStopped);
}

