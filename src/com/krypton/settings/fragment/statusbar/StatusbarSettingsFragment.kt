/*
 * Copyright (C) 2021 AOSP-Krypton Project
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

import android.os.Bundle
import android.os.UserHandle
import android.provider.DeviceConfig
import android.provider.Settings

import androidx.preference.SwitchPreference

import com.android.settings.R
import com.krypton.settings.fragment.BaseFragment

class StatusbarSettingsFragment: BaseFragment() {
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        addPreferencesFromResource(R.xml.statusbar_settings)
        findPreference<SwitchPreference>(LOCATION_INDICATOR_PREF_KEY)?.also {
            val showLocationIndicator = Settings.Secure.getIntForUser(context!!.contentResolver,
                Settings.Secure.ENABLE_LOCATION_PRIVACY_INDICATOR,
                if (shouldShowLocationIndicator()) 1 else 0,
                UserHandle.USER_CURRENT) == 1
            it.setChecked(showLocationIndicator)
        }
    }

    companion object {
        private const val LOCATION_INDICATOR_PREF_KEY = "enable_location_privacy_indicator"

        private fun shouldShowLocationIndicator() = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY,
                "location_indicators_enabled", false)
    }
}
