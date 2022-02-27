/*
 * Copyright (C) 2022 AOSP-Krypton Project
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

package com.krypton.settings.fragment.statusbar

import android.content.Context
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings

import androidx.preference.Preference
import androidx.preference.SwitchPreference

import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.Utils
import com.krypton.settings.KryptonBasePreferenceController
import com.krypton.settings.preference.SecureSettingSwitchPreference

class CombinedSignalIconPreferenceController(
    context: Context,
    key: String,
) : KryptonBasePreferenceController(context, key) {

    private val defaultEnabled = com.krypton.settings.Utils.getBoolSysUIResource(
        context, CONFIG_RESOURCE_NAME)

    override fun getAvailabilityStatus(): Int {
        val unavailable = Utils.isWifiOnly(mContext) ||
            !mContext.getSystemService(UserManager::class.java).isAdminUser() ||
            RestrictedLockUtilsInternal.hasBaseUserRestriction(mContext,
                UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
                UserHandle.myUserId())
        return if (unavailable) {
            UNSUPPORTED_ON_DEVICE
        } else {
            AVAILABLE
        }
    }

    override fun updateState(preference: Preference) {
        (preference as SwitchPreference).setChecked(
            Settings.Secure.getIntForUser(mContext.contentResolver,
                Settings.Secure.SHOW_COMBINED_STATUS_BAR_SIGNAL_ICONS,
                if (defaultEnabled) 1 else 0,
                UserHandle.USER_CURRENT,
            ) == 1
        )
    }

    companion object {
        private const val CONFIG_RESOURCE_NAME = "flag_combined_status_bar_signal_icons"
    }
}