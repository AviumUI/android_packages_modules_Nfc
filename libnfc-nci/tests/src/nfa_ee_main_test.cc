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

#include "nfa_ee_main.cc"

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <string>

#include "nfa_ee_int.h"

// Tests for nfa_ee_init
TEST(NfaEeMainInitTest, ProcessEeInit) {
  nfa_ee_init();
  EXPECT_EQ(nfa_ee_cb.ecb[0].nfcee_id, NFA_EE_INVALID);
  EXPECT_EQ(nfa_ee_cb.ecb[0].ee_status, NFC_NFCEE_STATUS_INACTIVE);
  EXPECT_EQ(nfa_ee_cb.ecb[NFA_EE_MAX_EE_SUPPORTED].nfcee_id, NFC_DH_ID);
  EXPECT_EQ(nfa_ee_cb.ecb[NFA_EE_MAX_EE_SUPPORTED].ee_status,
            NFC_NFCEE_STATUS_ACTIVE);
}

// Tests for nfa_ee_sys_enable
TEST(NfaEeMainSysEnableTest, ProcessEeSysEnableMaxEeToZero) {
  nfa_ee_max_ee_cfg = 0;
  nfa_ee_sys_enable();
  EXPECT_EQ(nfa_ee_cb.route_block_control, 0x00);
  EXPECT_EQ(nfa_ee_cb.em_state, NFA_EE_EM_STATE_INIT_DONE);
}

// Tests for nfa_ee_sys_disable
TEST(NfaEeMainSysDisableTest, ProcessEeSysDisable) {
  nfa_ee_max_ee_cfg = 0;
  nfa_ee_sys_disable();
}

// Tests for nfa_ee_check_disable
TEST(NfaEeMainCheckDisableTest, ProcessCheckEeDisable) {
  nfa_ee_cb.em_state = NFA_EE_EM_STATE_INIT_DONE;
  nfa_ee_cb.ee_flags = NFA_EE_FLAG_WAIT_HCI;
  nfa_ee_check_disable();
  EXPECT_EQ(nfa_ee_cb.em_state, NFA_EE_EM_STATE_DISABLED);
}

// Tests for nfa_ee_restore_one_ecb
TEST(NfaEeMainRestoreOneEcb, ProcessRestoreOneEcbInvalidNfceeId) {
  tNFA_EE_ECB* p_cb = nfa_ee_cb.ecb;
  p_cb->nfcee_id = NFA_EE_INVALID;
  nfa_ee_restore_one_ecb(nfa_ee_cb.ecb);
}

// Tests for nfa_ee_proc_nfcc_power_mode
TEST(NfaEeMainProcNfccPowerModeTest, ProcessPowerModeFullNoEe) {
  nfa_ee_max_ee_cfg = 0;
  nfa_ee_proc_nfcc_power_mode(NFA_DM_PWR_MODE_FULL);
}

TEST(NfaEeMainProcNfccPowerModeTest, ProcessPowerModeOff) {
  nfa_ee_proc_nfcc_power_mode(NFA_DM_PWR_MODE_OFF_SLEEP);
}

// Tests nfa_ee_proc_hci_info_cback
TEST(NfaEeMainProcHciInfo, ProcessHciInfo) {
  nfa_ee_proc_hci_info_cback();
  ASSERT_FALSE(nfa_ee_cb.ee_flags & NFA_EE_FLAG_WAIT_HCI);
}

// Tests for nfa_ee_proc_evt
TEST(NfaEeMainProcEvt, ProcessEnableEvent) {
  nfa_ee_proc_evt(NFC_ENABLE_REVT, nullptr);
}

// Tests for nfa_ee_ecb_to_mask
TEST(NfaEeMainEcbToMask, ProcessDefaultEeEcb) {
  tNFA_EE_ECB* p_cb = nfa_ee_cb.ecb;
  ASSERT_TRUE(nfa_ee_ecb_to_mask(nfa_ee_cb.ecb) != 0);
}

// Tests for nfa_ee_reg_cback_enable_done
TEST(NfaEeMainRegCbackDone, CbackAssignment) {
  nfa_ee_reg_cback_enable_done(nullptr);
  ASSERT_TRUE(nfa_ee_cb.p_enable_cback == nullptr);
}

// Tests for nfa_ee_find_ecb
TEST(NfaEeMainFindEcb, InvalidNfceeId) {
  tNFA_EE_ECB* ee_ecb = nfa_ee_find_ecb(NFC_TEST_ID);
  ASSERT_TRUE(ee_ecb == nullptr);
}

TEST(NfaEeMainFindEcb, HostNfceeId) {
  tNFA_EE_ECB* ee_ecb = nfa_ee_find_ecb(NFC_DH_ID);
  ASSERT_TRUE(ee_ecb != nullptr);
}

// Tests for nfa_ee_find_ecb_by_conn_id
TEST(NfaEeMainFindEcbByConnId, CloseConn) {
  tNFA_EE_ECB* ee_ecb = nfa_ee_find_ecb_by_conn_id(0);
  ASSERT_TRUE(ee_ecb == nullptr);
}

// Tests for nfa_ee_sm_st_2_str
TEST(NfaEeMainStrTest, EeStateToString) {
  EXPECT_EQ(nfa_ee_sm_st_2_str(NFA_EE_EM_STATE_INIT), "INIT");
  EXPECT_EQ(nfa_ee_sm_st_2_str(NFA_EE_EM_STATE_INIT_DONE), "INIT_DONE");
  EXPECT_EQ(nfa_ee_sm_st_2_str(NFA_EE_EM_STATE_RESTORING), "RESTORING");
  EXPECT_EQ(nfa_ee_sm_st_2_str(NFA_EE_EM_STATE_DISABLING), "DISABLING");
  EXPECT_EQ(nfa_ee_sm_st_2_str(NFA_EE_EM_STATE_DISABLED), "DISABLED");
  EXPECT_EQ(nfa_ee_sm_st_2_str(NFA_EE_EM_STATE_MAX), "Unknown");
  EXPECT_EQ(nfa_ee_sm_st_2_str(100), "Unknown");
  EXPECT_EQ(nfa_ee_sm_st_2_str(255), "Unknown");
}

// Tests for nfa_ee_sm_evt_2_str
TEST(NfaEeMainStrTest, EeEventToString) {
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_DISCOVER_EVT), "API_DISCOVER");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_REGISTER_EVT), "API_REGISTER");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_DEREGISTER_EVT), "API_DEREGISTER");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_MODE_SET_EVT), "API_MODE_SET");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_SET_TECH_CFG_EVT),
            "API_SET_TECH_CFG");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_CLEAR_TECH_CFG_EVT),
            "API_CLEAR_TECH_CFG");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_SET_PROTO_CFG_EVT),
            "API_SET_PROTO_CFG");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_CLEAR_PROTO_CFG_EVT),
            "API_CLEAR_PROTO_CFG");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_ADD_AID_EVT), "API_ADD_AID");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_REMOVE_AID_EVT), "API_REMOVE_AID");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_ADD_SYSCODE_EVT),
            "NFA_EE_API_ADD_SYSCODE_EVT");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_REMOVE_SYSCODE_EVT),
            "NFA_EE_API_REMOVE_SYSCODE_EVT");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_LMRT_SIZE_EVT), "API_LMRT_SIZE");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_UPDATE_NOW_EVT), "API_UPDATE_NOW");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_CONNECT_EVT), "API_CONNECT");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_SEND_DATA_EVT), "API_SEND_DATA");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_DISCONNECT_EVT), "API_DISCONNECT");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_API_PWR_AND_LINK_CTRL_EVT),
            "NFA_EE_API_PWR_AND_LINK_CTRL_EVT");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_NCI_DISC_RSP_EVT), "NCI_DISC_RSP");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_NCI_DISC_NTF_EVT), "NCI_DISC_NTF");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_NCI_MODE_SET_RSP_EVT), "NCI_MODE_SET");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_NCI_CONN_EVT), "NCI_CONN");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_NCI_DATA_EVT), "NCI_DATA");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_NCI_ACTION_NTF_EVT), "NCI_ACTION");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_NCI_DISC_REQ_NTF_EVT), "NCI_DISC_REQ");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_NCI_WAIT_RSP_EVT), "NCI_WAIT_RSP");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_ROUT_TIMEOUT_EVT), "ROUT_TIMEOUT");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_DISCV_TIMEOUT_EVT),
            "NFA_EE_DISCV_TIMEOUT_EVT");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_CFG_TO_NFCC_EVT), "CFG_TO_NFCC");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(NFA_EE_PWR_CONTROL_EVT),
            "NFA_EE_PWR_CONTROL_EVT");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(0x9999), "Unknown");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(0x0000), "Unknown");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(0xFFFF), "Unknown");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(0x0100), "Unknown");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(0x01FF), "Unknown");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(0x1000), "Unknown");
  EXPECT_EQ(nfa_ee_sm_evt_2_str(0x2000), "Unknown");
}

// Tests for nfa_ee_evt_hdlr
TEST(NfaEeMainEvtHdlrTest, ProcessEventNciConnStateInit) {
  nfa_ee_cb.em_state = NFA_EE_EM_STATE_INIT;
  tNFA_EE_INT_EVT int_event = NFA_EE_NCI_CONN_EVT;
  tNFA_EE_NCI_WAIT_RSP cbk = tNFA_EE_NCI_WAIT_RSP();
  cbk.hdr.event = int_event;
  cbk.p_data = nullptr;
  tNFA_EE_MSG nfa_ee_msg;
  nfa_ee_msg.wait_rsp = cbk;
  EXPECT_TRUE(nfa_ee_evt_hdlr(&nfa_ee_msg.hdr));
}
