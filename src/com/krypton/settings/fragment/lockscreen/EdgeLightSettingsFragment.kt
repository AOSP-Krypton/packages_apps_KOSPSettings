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

import android.os.Bundle
import android.widget.Switch

import androidx.preference.forEachIndexed
import androidx.preference.ListPreference
import androidx.preference.Preference

import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.OnMainSwitchChangeListener
import com.android.settingslib.widget.TopIntroPreference
import com.android.settings.R
import com.krypton.settings.fragment.KryptonDashboardFragment
import com.krypton.settings.preference.SettingColorPickerPreference

class EdgeLightSettingsFragment: KryptonDashboardFragment(),
        Preference.OnPreferenceChangeListener, OnMainSwitchChangeListener {
    private var colorPickerPreference: SettingColorPickerPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        findPreference<MainSwitchPreference>(MAIN_SWITCH_KEY)?.also {
            updatePreferences(it.isChecked)
            it.addOnSwitchChangeListener(this)
        }
        colorPickerPreference = findPreference(COLOR_PICKER_PREF_KEY)
        findPreference<ListPreference>(COLOR_MODE_PREF_KEY)?.also {
            colorPickerPreference?.setEnabled(it.value.toInt() == 3)
        }?.setOnPreferenceChangeListener(this)
    }

    override protected fun getPreferenceScreenResId() = R.xml.edge_light_settings

    override protected fun getLogTag() = TAG

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference.key == COLOR_MODE_PREF_KEY) {
            colorPickerPreference?.setEnabled((newValue as String).toInt() == 3)
            return true
        }
        return false
    }

    override fun onSwitchChanged(switchView: Switch, isChecked: Boolean) {
        updatePreferences(isChecked)
    }

    private fun updatePreferences(isChecked: Boolean) {
        preferenceScreen.forEachIndexed { _, preference ->
            if (preference !is MainSwitchPreference &&
                preference !is TopIntroPreference
            ) preference.isVisible = isChecked
        }
    }

    companion object {
        private const val TAG = "EdgeLightSettingsFragment"

        private const val MAIN_SWITCH_KEY = "edge_light_enabled"
        private const val COLOR_MODE_PREF_KEY = "edge_light_color_mode"
        private const val COLOR_PICKER_PREF_KEY = "edge_light_custom_color"
    }
}