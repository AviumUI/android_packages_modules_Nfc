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

#include "gki.h"
#include "nfc_int.h"

class MockGkiUtils : public GkiUtilsInterface {
 public:
  MOCK_METHOD(void, remove_from_timer_list,
              (TIMER_LIST_Q * p_timer_listq, TIMER_LIST_ENT* p_tle),
              (override));
  MOCK_METHOD(void, stop_timer, (uint8_t tnum), (override));
  MOCK_METHOD(bool, timer_list_empty, (TIMER_LIST_Q * p_list), (override));
  MOCK_METHOD(void, start_timer,
              (uint8_t timer_id, uint32_t ticks, bool periodic), (override));
  MOCK_METHOD(uint8_t, get_taskid, (), (override));
  MOCK_METHOD(void*, getbuf, (uint16_t size), (override));
  MOCK_METHOD(void, send_msg,
              (uint8_t task_id, uint8_t mailbox_id, void* p_msg), (override));
  MOCK_METHOD(void, add_to_timer_list,
              (TIMER_LIST_Q * p_list, TIMER_LIST_ENT* p_tle), (override));
  MOCK_METHOD(uint16_t, update_timer_list,
              (TIMER_LIST_Q * p_timer_listq,
               int32_t num_units_since_last_update),
              (override));
  MOCK_METHOD(TIMER_LIST_ENT*, timer_list_first, (TIMER_LIST_Q * p_timer_listq),
              (override));

  MOCK_METHOD(void, freebuf, (void* p_buf), (override));
  MOCK_METHOD(void, enqueue, (BUFFER_Q * p_q, void* p_buf), (override));
  MOCK_METHOD(void*, dequeue, (BUFFER_Q * p_q), (override));
};
