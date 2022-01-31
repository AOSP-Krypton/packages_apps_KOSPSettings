/*
 * Copyright (C) 2021-2022 AOSP-Krypton Project
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

package com.krypton.settings.fragment.theme

import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.Secure.MONET_ENGINE_CHROMA_FACTOR
import android.provider.Settings.Secure.MONET_ENGINE_COLOR_OVERRIDE

import androidx.preference.Preference
import androidx.preference.SwitchPreference

import com.krypton.settings.fragment.KryptonDashboardFragment
import com.krypton.settings.preference.CustomSeekBarPreference
import com.krypton.settings.preference.SettingColorPickerPreference
import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class MonetEngineSettingsFragment : KryptonDashboardFragment(),
        Preference.OnPreferenceChangeListener {

    private var customColorPickerPreference: SettingColorPickerPreference? = null
    
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        val customColor: String? = Settings.Secure.getString(
            context!!.contentResolver, MONET_ENGINE_COLOR_OVERRIDE)

        customColorPickerPreference = findPreference<SettingColorPickerPreference>(CUSTOM_COLOR_PREF_KEY)

        if (customColor == null) {
            customColorPickerPreference?.setSummary(getString(R.string.color_override_summary))
        }

        findPreference<SwitchPreference>(USE_WALLPAPER_COLOR_PREF_KEY)?.also {
            val isEnabled = customColor == null || customColor.isEmpty()
            it.setChecked(isEnabled)
            customColorPickerPreference?.setEnabled(!isEnabled)
        }?.setOnPreferenceChangeListener(this)

        val chromaFactor = Settings.Secure.getFloat(
            context!!.contentResolver, MONET_ENGINE_CHROMA_FACTOR,
                CHROMA_DEFAULT) * 100
        findPreference<CustomSeekBarPreference>(CHROMA_SLIDER_PREF_KEY)?.also {
            it.setValue(chromaFactor.toInt())
        }?.setOnPreferenceChangeListener(this)
    }

    override protected fun getPreferenceScreenResId() = R.xml.monet_engine_settings

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean =
        when (preference.key) {
            USE_WALLPAPER_COLOR_PREF_KEY -> {
                (newValue as Boolean).let {
                    if (it) {
                        Settings.Secure.putString(context!!.contentResolver,
                            MONET_ENGINE_COLOR_OVERRIDE, null)
                    }
                    customColorPickerPreference?.setEnabled(!it)
                    return true
                }
            }
            CHROMA_SLIDER_PREF_KEY -> Settings.Secure.putFloat(context!!.contentResolver,
                    MONET_ENGINE_CHROMA_FACTOR, (newValue as Int) / 100f)
            else -> false
        }

    override protected fun getLogTag() = TAG

    companion object {
        private const val TAG = "MonetEngineSettingsFragment"

        private const val USE_WALLPAPER_COLOR_PREF_KEY = "monet_engine_use_wallpaper_color"
        private const val CUSTOM_COLOR_PREF_KEY = "monet_engine_color_override"

        private const val CHROMA_SLIDER_PREF_KEY = "chroma_factor"
        private const val CHROMA_DEFAULT = 1f

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.monet_engine_settings)
    }
}
