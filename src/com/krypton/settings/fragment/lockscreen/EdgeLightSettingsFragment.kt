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

package com.krypton.settings.fragment.lockscreen

import android.graphics.Color
import android.hardware.display.AmbientDisplayConfiguration
import android.os.Bundle
import android.os.UserHandle.USER_CURRENT
import android.provider.Settings

import androidx.preference.Preference

import com.android.settings.R
import com.krypton.settings.fragment.ColorPickerFragment
import com.krypton.settings.fragment.KryptonDashboardFragment

class EdgeLightSettingsFragment: KryptonDashboardFragment(),
        Preference.OnPreferenceChangeListener {

    private var colorPickerPreference: Preference? = null
    private var timeoutPreference: Preference? = null

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        val isAodEnabled = AmbientDisplayConfiguration(context!!)
            .alwaysOnEnabledSetting(USER_CURRENT)
        findPreference<Preference>(EDGE_LIGHT_AOD_PREF_KEY)?.setEnabled(isAodEnabled)

        colorPickerPreference = findPreference<Preference>(EDGE_LIGHT_CUSTOM_COLOR_PREF_KEY)?.also {
            val isCustomMode = Settings.System.getIntForUser(context!!.contentResolver,
                Settings.System.NOTIFICATION_PULSE_COLOR_MODE, 0, USER_CURRENT) == 3
            it.setEnabled(isCustomMode)
        }
        timeoutPreference = findPreference(EDGE_LIGHT_TIMEOUT_PREF_KEY)

        findPreference<Preference>(EDGE_LIGHT_COLOR_MODE_PREF_KEY)?.setOnPreferenceChangeListener(this)
        findPreference<Preference>(EDGE_LIGHT_REPEAT_COUNT_PREF_KEY)?.setOnPreferenceChangeListener(this)
    }

    override protected fun getPreferenceScreenResId() = R.xml.edge_light_settings

    override protected fun getLogTag() = TAG

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when (preference.key) {
            EDGE_LIGHT_COLOR_MODE_PREF_KEY -> {
                newValue?.let {
                    val mode = (newValue as String).toInt()
                    colorPickerPreference?.setEnabled(mode == 3)
                }
            }
            EDGE_LIGHT_REPEAT_COUNT_PREF_KEY -> {
                newValue?.let {
                    val count = newValue as Int
                    timeoutPreference?.setEnabled(count == 0)
                }
            }
        }
        return true
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference == colorPickerPreference) {
            val preferenceDataStore = preference.getPreferenceDataStore()
            var defaultColor: Int = try {
                preferenceDataStore?.getString(preference.key, null)
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { Color.parseColor(it) }
            } catch (_: IllegalArgumentException) {
                null
            } ?: Color.WHITE
            EdgeLightColorPickerFragment(
                preference.key,
                preferenceDataStore,
                defaultColor,
            ).show(childFragmentManager, TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    companion object {
        private const val TAG = "EdgeLightSettingsFragment"

        private const val EDGE_LIGHT_AOD_PREF_KEY = "ambient_notification_light_enabled"
        private const val EDGE_LIGHT_COLOR_MODE_PREF_KEY = "ambient_notification_color_mode"
        private const val EDGE_LIGHT_CUSTOM_COLOR_PREF_KEY = "ambient_notification_light_color"
        private const val EDGE_LIGHT_REPEAT_COUNT_PREF_KEY = "ambient_notification_light_repeats"
        private const val EDGE_LIGHT_TIMEOUT_PREF_KEY = "ambient_notification_light_timeout"
    }
}
