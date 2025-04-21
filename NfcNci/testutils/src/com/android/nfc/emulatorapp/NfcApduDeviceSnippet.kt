/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.nfc.emulatorapp

import android.app.Instrumentation
import android.content.Intent
import android.nfc.NfcAdapter
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry

import com.android.nfc.utils.NfcSnippet
import com.google.android.mobly.snippet.rpc.Rpc

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NfcApduDeviceSnippet : NfcSnippet() {
  private val TAG = "NfcApduDeviceSnippet"
  private lateinit var mActivity: MainActivity
  private val mContext = InstrumentationRegistry.getInstrumentation().getContext()

  @Rpc(description = "Checks if observe mode is supported on device")
  fun isObserveModeSupported(): Boolean {
    val nfcAdapter = NfcAdapter.getDefaultAdapter(mContext)
    return nfcAdapter.isObserveModeSupported()
  }

  @Rpc(description = "Checks if secure NFC is supported on device")
  fun isSecureNfcSupported(): Boolean {
    val nfcAdapter = NfcAdapter.getDefaultAdapter(mContext)
    return nfcAdapter.isSecureNfcSupported()
  }

  @Rpc(description = "Checks if reader option is supported on device")
  fun isReaderOptionSupported(): Boolean {
    val nfcAdapter = NfcAdapter.getDefaultAdapter(mContext)
    return nfcAdapter.isReaderOptionSupported()
  }

  @Rpc(description = "Checks if controller always on is supported on device")
  fun isControllerAlwaysOnSupported(): Boolean {
    val nfcAdapter = NfcAdapter.getDefaultAdapter(mContext)
    return nfcAdapter.isControllerAlwaysOnSupported()
  }

  @Rpc(description = "Start Main Activity")
  fun startMainActivity(json: String) {
    val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    val intent = Intent(Intent.ACTION_MAIN)
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.setClassName(
            instrumentation.getTargetContext(), MainActivity::class.java.getName())
    intent.putExtra(MainActivity.SNOOP_DATA_FLAG, json)

    mActivity = instrumentation.startActivitySync(intent) as MainActivity
  }

  @Rpc(description = "Close activity")
  fun closeActivity() {
    mActivity.finish()
  }

  @Rpc(description = "Call to set reader option")
  fun setReaderOption(enable: Boolean) {
    val nfcAdapter = NfcAdapter.getDefaultAdapter(mContext)
    try {
      val result = nfcAdapter.enableReaderOption(enable)
      if (!result) {
        Log.e(TAG, "Failed to set reader option")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Exception", e)
    }
  }

  @Rpc(description = "Call to set NFC controller always on feature")
  fun setControllerAlwaysOn(value: Boolean) {
    val nfcAdapter = NfcAdapter.getDefaultAdapter(mContext)
    val countDownLatch = CountDownLatch(1)
    val listener = NfcControllerAlwaysOnListener(countDownLatch)
    try {
      nfcAdapter.registerControllerAlwaysOnListener(Executors.newSingleThreadExecutor(), listener)
      nfcAdapter.setControllerAlwaysOn(value)
      countDownLatch.await(5, TimeUnit.SECONDS)
      nfcAdapter.unregisterControllerAlwaysOnListener(listener)
    } catch (e: Exception) {
      Log.e(TAG, "Exception", e)
    }
  }

  @Rpc(description = "Call to set observe mode")
  fun setObserveMode(enabled: Boolean) {
    val nfcAdapter = NfcAdapter.getDefaultAdapter(mContext)
    try {
      val result = nfcAdapter.setObserveModeEnabled(enabled)
      if (!result) {
        Log.e(TAG, "Failed to set observe mode")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Exception", e)
    }
  }

  @Rpc(description = "Call to set secure NFC")
  fun setSecureNfc(enabled: Boolean) {
    val nfcAdapter = NfcAdapter.getDefaultAdapter(mContext)
    try {
      val result = nfcAdapter.enableSecureNfc(enabled)
      if (!result) {
        Log.e(TAG, "Failed to set secure NFC")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Exception", e)
    }
  }

  @Rpc(description = "Adopt permissions necessary to use other functions in NfcApduDeviceSnippet")
  fun adoptPermissions() {
    InstrumentationRegistry.getInstrumentation()
      .getUiAutomation()
      .adoptShellPermissionIdentity(android.Manifest.permission.WRITE_SECURE_SETTINGS)
    InstrumentationRegistry.getInstrumentation()
      .getUiAutomation()
      .adoptShellPermissionIdentity(android.Manifest.permission.NFC_SET_CONTROLLER_ALWAYS_ON)
  }

  @Rpc(description = "Drop permissions")
  fun dropPermissions() {
    InstrumentationRegistry.getInstrumentation().getUiAutomation().dropShellPermissionIdentity()
  }

  class NfcControllerAlwaysOnListener(private val countDownLatch: CountDownLatch)
    : NfcAdapter.ControllerAlwaysOnListener {
    override fun onControllerAlwaysOnChanged(isEnabled: Boolean) {
      countDownLatch.countDown()
    }
  }
}