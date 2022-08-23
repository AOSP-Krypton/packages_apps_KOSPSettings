/*
 * Copyright (C) 2022 FlamingoOS Project
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

package com.flamingo.settings.statusbar

import android.content.Context
import android.provider.Settings
import android.os.UserHandle

import androidx.preference.Preference
import androidx.preference.SwitchPreference

import com.android.settings.core.BasePreferenceController
import com.android.settingslib.Utils
import com.flamingo.support.preference.SecureSettingSwitchPreference
import com.flamingo.settings.getBoolSysUIResource

private const val CONFIG_RESOURCE_NAME = "flag_combined_status_bar_signal_icons"

class CombinedSignalIconPreferenceController(
    context: Context,
    key: String,
) : BasePreferenceController(context, key) {

    private val defaultValue = if (getBoolSysUIResource(context, CONFIG_RESOURCE_NAME)) 1 else 0

    override fun getAvailabilityStatus(): Int {
        return if (Utils.isWifiOnly(mContext)) {
            UNSUPPORTED_ON_DEVICE
        } else {
            AVAILABLE
        }
    }

    override fun updateState(preference: Preference) {
        (preference as SwitchPreference).setChecked(
            Settings.Secure.getIntForUser(
                mContext.contentResolver,
                Settings.Secure.SHOW_COMBINED_STATUS_BAR_SIGNAL_ICONS,
                defaultValue,
                UserHandle.USER_CURRENT,
            ) == 1
        )
    }
}