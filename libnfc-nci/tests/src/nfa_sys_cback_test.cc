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
#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include "nfa_sys.h"
#include "nfa_sys_int.h"

static void mock_callback(__attribute__((unused)) void) {}

class NfaSysCbackTest : public ::testing::Test {
 protected:
  void SetUp() override { tNFA_SYS_CB nfa_sys_cb; }
};

TEST_F(NfaSysCbackTest, EnableCompleteTest) {
  nfa_sys_cb.p_enable_cback = mock_callback;
  nfa_sys_cback_reg_enable_complete(nfa_sys_cb.p_enable_cback);
  EXPECT_EQ(nfa_sys_cb.enable_cplt_flags, 0);
}
TEST_F(NfaSysCbackTest, PowerCompleteTest) {
  nfa_sys_cb.p_proc_nfcc_pwr_mode_cmpl_cback = mock_callback;
  nfa_sys_cback_reg_nfcc_power_mode_proc_complete(
      nfa_sys_cb.p_proc_nfcc_pwr_mode_cmpl_cback);
  EXPECT_EQ(nfa_sys_cb.proc_nfcc_pwr_mode_cplt_flags, 0);
}
TEST_F(NfaSysCbackTest, NotifyCompleteTest) {
  nfa_sys_cb.enable_cplt_flags = 0;
  nfa_sys_cb.enable_cplt_mask = 0;
  nfa_sys_cb.proc_nfcc_pwr_mode_cplt_flags = 0;
  nfa_sys_cb.proc_nfcc_pwr_mode_cplt_mask = 0;
  nfa_sys_cb.p_enable_cback = mock_callback;
  nfa_sys_cb.enable_cplt_mask |= (0x0001 << NFA_ID_EE);
  nfa_sys_cback_notify_enable_complete(NFA_ID_EE);
  EXPECT_EQ(nfa_sys_cb.p_enable_cback, nullptr);
  nfa_sys_cback_notify_partial_enable_complete(NFA_ID_SYS);
  EXPECT_EQ(nfa_sys_cb.p_enable_cback, nullptr);
  nfa_sys_cb.enable_cplt_flags = 0;
  nfa_sys_cb.proc_nfcc_pwr_mode_cplt_mask |= (0x0001 << NFA_ID_EE);
  nfa_sys_cback_notify_nfcc_power_mode_proc_complete(NFA_ID_EE);
  EXPECT_EQ(nfa_sys_cb.p_proc_nfcc_pwr_mode_cmpl_cback, nullptr);
}