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

#include "nfa_dm_int.h"

static void mock_callback(__attribute__((unused)) uint8_t nfcc_power_mode) {}

class NfaSysMainTest : public ::testing::Test {
 protected:
  void SetUp() override { nfa_sys_init(); }
};

TEST_F(NfaSysMainTest, SysMainTest) {
  tNFA_SYS_REG nfa_hci_sys_reg;
  nfa_hci_sys_reg.proc_nfcc_pwr_mode = mock_callback;
  nfa_sys_register(NFA_ID_HCI, &nfa_hci_sys_reg);
  EXPECT_TRUE(nfa_sys_is_register(NFA_ID_HCI));
  NFC_HDR p_msg;
  p_msg.event = 0xa00;
  nfa_sys_event(&p_msg);
  nfa_sys_sendmsg(nullptr);
  nfa_sys_timer_update();
  nfa_sys_enable_subsystems();
  nfa_sys_notify_nfcc_power_mode(NFA_DM_PWR_MODE_OFF_SLEEP);
  nfa_sys_disable_timers();
  nfa_sys_disable_subsystems(true);
  EXPECT_TRUE(nfa_sys_is_graceful_disable());
  nfa_dm_disable_complete();
  nfa_sys_deregister(NFA_ID_HCI);
}