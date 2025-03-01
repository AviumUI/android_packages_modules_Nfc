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
#include "nfa_sys_ptim.h"

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include "gki.h"
#include "nfa_sys.h"
#include "nfa_sys_int.h"
#include "nfc_target.h"

class NfaSysPtimTest : public ::testing::Test {
 protected:
  void SetUp() override {
    tNFA_SYS_CB nfa_sys_cb;
    nfa_sys_ptim_init(&nfa_sys_cb.ptim_cb, 10, p_nfa_sys_cfg->timer);
  }
};

TEST_F(NfaSysPtimTest, TimerTest) {
  nfa_sys_ptim_timer_update(&nfa_sys_cb.ptim_cb);
  tPTIM_CB p_cb;
  TIMER_LIST_ENT tle;
  nfa_sys_ptim_start_timer(&p_cb, &tle, 0,
                           NFA_DM_DISC_TIMEOUT_KOVIO_PRESENCE_CHECK);
  nfa_sys_ptim_stop_timer(&p_cb, &tle);
}
