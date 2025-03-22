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

#include <android-base/file.h>
#include <android-base/logging.h>
#include <android-base/properties.h>
#include <android-base/stringprintf.h>
#include <android/log.h>
#include <dlfcn.h>
#include <error.h>
#include <log/log.h>
#include <string>

using android::base::StringPrintf;
#define UNUSED_PROP(X) (void)(X);

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
std::string mLibPathName = "";

void* p_oem_extn_handle = NULL;

namespace {
  std::string searchLibPath(std::string file_name) {
    const std::vector<std::string> search_path = {
        "/system/lib/", "/system/lib64/"
    };
    for (std::string path : search_path) {
      path.append(file_name);
      struct stat file_stat;
      if (stat(path.c_str(), &file_stat) != 0) continue;
      if (S_ISREG(file_stat.st_mode)) return path;
    }
    return "";
  }
  // Extension library file Search sequence
  // 1. If prop_lib_file_name is defined.(where prop_config_file_name is the
  //   value of the property persist.nfc_vendor_extn.lib_file_name)
  //   Search a file matches prop_lib_file_name.
  // 2. If SKU is defined (where SKU is the value of the property
  //   ro.boot.product.hardware.sku)
  //   Search a file matches libnfc_vendor_extn-SKU.so
  // 3. If none of 1,2 is defined, search a default file name "libnfc_vendor_extn.so".
  std::string findLibPath() {
    std::string f_path = searchLibPath(
        android::base::GetProperty("persist.nfc_vendor_extn.lib_file_name", ""));
    if (!f_path.empty()) return f_path;

    // Search for libnfc_vendor_extn-SKU.so
    f_path = searchLibPath(
        "libnfc_vendor_extn-" +
        android::base::GetProperty("ro.boot.product.hardware.sku", "") + ".so");
    if (!f_path.empty()) return f_path;

    // load default file if the desired file not found.
    return searchLibPath("libnfc_vendor_extn.so");
  }
}  // namespace

NfcVendorExtn::NfcVendorExtn() {}

NfcVendorExtn::~NfcVendorExtn() { sNfcVendorExtn = nullptr; }

NfcVendorExtn* NfcVendorExtn::getInstance() {
  if (sNfcVendorExtn == nullptr) {
    sNfcVendorExtn = new NfcVendorExtn();
  }
  return sNfcVendorExtn;
}

void NfcExtn_LibInit() {
  LOG(VERBOSE) << __func__;
  if (fp_extn_init != NULL) {
    if (!fp_extn_init(NfcVendorExtn::getInstance()->getVendorExtnCb())) {
      LOG(ERROR) << StringPrintf("%s: %s failed!", __func__,
                                 vendor_nfc_init_name.c_str());
    }
  }
}

bool NfcExtn_LibSetup() {
  LOG(VERBOSE) << __func__;
  mLibPathName = findLibPath();
  if (mLibPathName.empty()) {
    LOG(ERROR) << StringPrintf("%s: Failed to find %s !!", __func__,
                               mLibPathName.c_str());
    return false;
  }
  p_oem_extn_handle = dlopen(mLibPathName.c_str(), RTLD_NOW);
  if (p_oem_extn_handle == NULL) {
    LOG(DEBUG) << StringPrintf(
        "%s: Error : opening (%s) !! dlerror: "
        "%s",
        __func__, mLibPathName.c_str(), dlerror());
    return false;
  }
  if ((fp_extn_init = (fp_extn_init_t)dlsym(
           p_oem_extn_handle, vendor_nfc_init_name.c_str())) == NULL) {
    LOG(ERROR) << StringPrintf("%s: Failed to find %s !!", __func__,
                               vendor_nfc_init_name.c_str());
  }

  if ((fp_extn_deinit = (fp_extn_deinit_t)dlsym(
           p_oem_extn_handle, vendor_nfc_de_init_name.c_str())) == NULL) {
    LOG(ERROR) << StringPrintf("%s: Failed to find %s !!", __func__,
                               vendor_nfc_de_init_name.c_str());
  }

  if ((fp_extn_handle_nfc_event = (fp_extn_handle_nfc_event_t)dlsym(
           p_oem_extn_handle, vendor_nfc_handle_event_name.c_str())) == NULL) {
    LOG(ERROR) << StringPrintf("%s: Failed to find %s !!", __func__,
                               vendor_nfc_handle_event_name.c_str());
  }

  if ((fp_extn_on_config_update = (fp_extn_on_config_update_t)dlsym(
           p_oem_extn_handle, vendor_nfc_on_config_update_name.c_str())) ==
      NULL) {
    LOG(ERROR) << StringPrintf("%s: Failed to find %s !!", __func__,
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
  if (!NfcExtn_LibSetup()) {
    mVendorExtnCb.hidlHal = nullptr;
    mVendorExtnCb.aidlHal = nullptr;
    return false;
  }
  return true;
}

void NfcVendorExtn::setNciCallback(tHAL_NFC_CBACK* pHalCback,
                                   tHAL_NFC_DATA_CBACK* pDataCback) {
  LOG(VERBOSE) << __func__;
  mVendorExtnCb.pHalCback = pHalCback;
  mVendorExtnCb.pDataCback = pDataCback;
}

bool NfcVendorExtn::processCmd(uint16_t dataLen, uint8_t* pData) {
  LOG(VERBOSE) << StringPrintf("%s: Enter dataLen=%d", __func__, dataLen);
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
    LOG(ERROR) << StringPrintf("%s: not found", __func__);
    return false;
  }
}

bool NfcVendorExtn::processRspNtf(uint16_t dataLen, uint8_t* pData) {
  LOG(VERBOSE) << StringPrintf("%s: dataLen=%d", __func__, dataLen);
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
    LOG(ERROR) << StringPrintf("%s: not found", __func__);
    return false;
  }
}

bool NfcVendorExtn::processEvent(uint8_t event, uint8_t status) {
  LOG(VERBOSE) << StringPrintf("%s: event=%d, status=%d", __func__, event,
                               status);
  if (fp_extn_handle_nfc_event != NULL) {
    mNfcExtEventData.hal_event = event;
    mNfcExtEventData.hal_event_status = status;
    tNFC_STATUS stat =
        fp_extn_handle_nfc_event(HANDLE_HAL_EVENT, mNfcExtEventData);
    LOG(DEBUG) << StringPrintf("%s: Exit status(%d)", __func__, stat);
    return stat == NFCSTATUS_EXTN_FEATURE_SUCCESS;
  } else {
    LOG(ERROR) << StringPrintf("%s: not found", __func__);
    return false;
  }
}

void NfcVendorExtn::getVendorConfigs(
    std::map<std::string, ConfigValue>* pConfigMap) {
  LOG(VERBOSE) << __func__;
  mVendorExtnCb.configMap = *pConfigMap;
  if (fp_extn_on_config_update != NULL) {
    fp_extn_on_config_update(pConfigMap);
  } else {
    LOG(ERROR) << StringPrintf("%s: not found", __func__);
  }
}

VendorExtnCb* NfcVendorExtn::getVendorExtnCb() { return &mVendorExtnCb; }

void phNfcExtn_LibClose() {
  LOG(VERBOSE) << __func__;
  if (fp_extn_deinit != NULL) {
    if (!fp_extn_deinit()) {
      LOG(ERROR) << StringPrintf("%s: %s failed", __func__,
                                 vendor_nfc_de_init_name.c_str());
    }
  }
  if (p_oem_extn_handle != NULL) {
    LOG(DEBUG) << StringPrintf("%s: Closing %s!!", __func__, mLibPathName.c_str());
    dlclose(p_oem_extn_handle);
    p_oem_extn_handle = NULL;
  }
}

bool NfcVendorExtn::finalize(void) {
  LOG(VERBOSE) << __func__;
  phNfcExtn_LibClose();
  mVendorExtnCb.hidlHal = nullptr;
  mVendorExtnCb.aidlHal = nullptr;
  return true;
}
