/*
 * Copyright 2024, The Android Open Source Project
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

#include "NfcVendorExtn.h"

#include <android-base/logging.h>
#include <android-base/stringprintf.h>
#include <android/log.h>
#include <dlfcn.h>
#include <error.h>
#include <log/log.h>

using android::base::StringPrintf;
#define UNUSED_PROP(X) (void)(X);

std::string mLibName = "libnfc_vendor_extn.so";
#if (defined(__arm64__) || defined(__aarch64__) || defined(_M_ARM64))
std::string mLibPathName = "/system/lib64/" + mLibName;
#else
std::string mLibPathName = "/system/lib/" + mLibName;
#endif
const std::string vendor_nfc_init_name = "vendor_nfc_init";
const std::string vendor_nfc_de_init_name = "vendor_nfc_de_init";
const std::string vendor_nfc_handle_event_name = "vendor_nfc_handle_event";
const std::string vendor_nfc_on_config_update_name =
    "vendor_nfc_on_config_update";

static NfcVendorExtn* sNfcVendorExtn;

typedef bool (*fp_extn_init_t)(VendorExtnCb*);
typedef bool (*fp_extn_deinit_t)();
typedef tNFC_STATUS (*fp_extn_handle_nfc_event_t)(NfcExtEvent_t,
                                                  NfcExtEventData_t);
typedef void (*fp_extn_on_config_update_t)(std::map<std::string, ConfigValue>*);

fp_extn_init_t fp_extn_init = NULL;
fp_extn_deinit_t fp_extn_deinit = NULL;
fp_extn_handle_nfc_event_t fp_extn_handle_nfc_event = NULL;
fp_extn_on_config_update_t fp_extn_on_config_update = NULL;

NfcExtEventData_t mNfcExtEventData;

void* p_oem_extn_handle = NULL;

NfcVendorExtn::NfcVendorExtn() {}

NfcVendorExtn::~NfcVendorExtn() { sNfcVendorExtn = nullptr; }

NfcVendorExtn* NfcVendorExtn::getInstance() {
  if (sNfcVendorExtn == nullptr) {
    sNfcVendorExtn = new NfcVendorExtn();
  }
  return sNfcVendorExtn;
}

void NfcExtn_LibInit() {
  LOG(VERBOSE) << StringPrintf("%s Enter", __func__);
  if (fp_extn_init != NULL) {
    if (!fp_extn_init(NfcVendorExtn::getInstance()->getVendorExtnCb())) {
      LOG(ERROR) << StringPrintf("%s : %s failed!", __func__,
                                 vendor_nfc_init_name.c_str());
    }
  }
}

bool NfcExtn_LibSetup() {
  LOG(VERBOSE) << StringPrintf("%s Enter", __func__);
  p_oem_extn_handle = dlopen(mLibPathName.c_str(), RTLD_NOW);
  if (p_oem_extn_handle == NULL) {
    LOG(DEBUG) << StringPrintf(
        "%s Error : opening (%s) !! dlerror: "
        "%s",
        __func__, mLibPathName.c_str(), dlerror());
    return false;
  }
  if ((fp_extn_init = (fp_extn_init_t)dlsym(
           p_oem_extn_handle, vendor_nfc_init_name.c_str())) == NULL) {
    LOG(ERROR) << StringPrintf("%s Failed to find %s !!", __func__,
                               vendor_nfc_init_name.c_str());
  }

  if ((fp_extn_deinit = (fp_extn_deinit_t)dlsym(
           p_oem_extn_handle, vendor_nfc_de_init_name.c_str())) == NULL) {
    LOG(ERROR) << StringPrintf("%s Failed to find %s !!", __func__,
                               vendor_nfc_de_init_name.c_str());
  }

  if ((fp_extn_handle_nfc_event = (fp_extn_handle_nfc_event_t)dlsym(
           p_oem_extn_handle, vendor_nfc_handle_event_name.c_str())) == NULL) {
    LOG(ERROR) << StringPrintf("%s Failed to find %s !!", __func__,
                               vendor_nfc_handle_event_name.c_str());
  }

  if ((fp_extn_on_config_update = (fp_extn_on_config_update_t)dlsym(
           p_oem_extn_handle, vendor_nfc_on_config_update_name.c_str())) ==
      NULL) {
    LOG(ERROR) << StringPrintf("%s Failed to find %s !!", __func__,
                               vendor_nfc_on_config_update_name.c_str());
  }

  NfcExtn_LibInit();
  return true;
}

bool NfcVendorExtn::Initialize(sp<INfc> hidlHal,
                               std::shared_ptr<INfcAidl> aidlHal) {
  LOG(VERBOSE) << StringPrintf("%s:", __func__);
  mVendorExtnCb.hidlHal = hidlHal;
  mVendorExtnCb.aidlHal = aidlHal;
  return NfcExtn_LibSetup();
}

void NfcVendorExtn::setNciCallback(tHAL_NFC_CBACK* pHalCback,
                                   tHAL_NFC_DATA_CBACK* pDataCback) {
  LOG(VERBOSE) << StringPrintf("%s:", __func__);
  mVendorExtnCb.pHalCback = pHalCback;
  mVendorExtnCb.pDataCback = pDataCback;
}

bool NfcVendorExtn::processCmd(uint16_t dataLen, uint8_t* pData) {
  LOG(VERBOSE) << StringPrintf("%s: Enter dataLen:%d", __func__, dataLen);
  NciData_t nci_data;
  nci_data.data_len = dataLen;
  nci_data.p_data = (uint8_t*)pData;
  mNfcExtEventData.nci_msg = nci_data;

  if (fp_extn_handle_nfc_event != NULL) {
    tNFC_STATUS stat =
        fp_extn_handle_nfc_event(HANDLE_VENDOR_NCI_MSG, mNfcExtEventData);
    LOG(VERBOSE) << StringPrintf("%s: Exit status(%d)", __func__, stat);
    return stat == NFCSTATUS_EXTN_FEATURE_SUCCESS;
  } else {
    LOG(ERROR) << StringPrintf("%s: %s", __func__, "processCmd not found!");
    return false;
  }
}

bool NfcVendorExtn::processRspNtf(uint16_t dataLen, uint8_t* pData) {
  LOG(VERBOSE) << StringPrintf("%s: Enter dataLen:%d", __func__, dataLen);
  NciData_t nciData;
  nciData.data_len = dataLen;
  nciData.p_data = (uint8_t*)pData;
  mNfcExtEventData.nci_rsp_ntf = nciData;

  if (fp_extn_handle_nfc_event != NULL) {
    tNFC_STATUS stat =
        fp_extn_handle_nfc_event(HANDLE_VENDOR_NCI_RSP_NTF, mNfcExtEventData);
    LOG(VERBOSE) << StringPrintf("%s: Exit status(%d)", __func__, stat);
    return stat == NFCSTATUS_EXTN_FEATURE_SUCCESS;
  } else {
    LOG(ERROR) << StringPrintf("%s: %s", __func__, "processRspNtf not found!");
    return false;
  }
}

bool NfcVendorExtn::processEvent(uint8_t event, uint8_t status) {
  LOG(VERBOSE) << StringPrintf("%s: Enter event: %d, status: %d", __func__,
                               event, status);
  if (fp_extn_handle_nfc_event != NULL) {
    mNfcExtEventData.hal_event = event;
    mNfcExtEventData.hal_event_status = status;
    tNFC_STATUS stat =
        fp_extn_handle_nfc_event(HANDLE_HAL_EVENT, mNfcExtEventData);
    LOG(DEBUG) << StringPrintf("%s: Exit status(%d)", __func__, stat);
    return stat == NFCSTATUS_EXTN_FEATURE_SUCCESS;
  } else {
    LOG(ERROR) << StringPrintf("%s: %s", __func__, "processEvent not found!");
    return false;
  }
}

void NfcVendorExtn::getVendorConfigs(
    std::map<std::string, ConfigValue>* pConfigMap) {
  LOG(VERBOSE) << StringPrintf("%s:", __func__);
  mVendorExtnCb.configMap = *pConfigMap;
  if (fp_extn_on_config_update != NULL) {
    fp_extn_on_config_update(pConfigMap);
  } else {
    LOG(ERROR) << StringPrintf("%s: %s", __func__,
                               "getVendorConfigs not found!");
  }
}

VendorExtnCb* NfcVendorExtn::getVendorExtnCb() { return &mVendorExtnCb; }

void phNfcExtn_LibClose() {
  LOG(VERBOSE) << StringPrintf("%s Enter", __func__);
  if (fp_extn_deinit != NULL) {
    if (!fp_extn_deinit()) {
      LOG(ERROR) << StringPrintf("%s: %s failed", __func__,
                                 vendor_nfc_de_init_name.c_str());
    }
  }
  if (p_oem_extn_handle != NULL) {
    LOG(DEBUG) << StringPrintf("%s Closing %s!!", __func__, mLibName.c_str());
    dlclose(p_oem_extn_handle);
    p_oem_extn_handle = NULL;
  }
}

bool NfcVendorExtn::finalize(void) {
  LOG(VERBOSE) << StringPrintf("%s:", __func__);
  phNfcExtn_LibClose();
  return true;
}
