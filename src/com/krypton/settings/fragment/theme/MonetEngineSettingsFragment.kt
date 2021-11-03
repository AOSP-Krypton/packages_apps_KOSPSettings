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
package com.krypton.settings.fragment.theme

import android.graphics.Color
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.provider.Settings.Secure.MONET_ENGINE_CHROMA_FACTOR

import androidx.preference.Preference

import com.krypton.settings.fragment.BaseFragment
import com.krypton.settings.preference.CustomSeekBarPreference
import com.krypton.settings.preference.SettingColorPickerPreference
import com.android.settings.R

class MonetEngineSettingsFragment: BaseFragment(),
        Preference.OnPreferenceChangeListener {
    
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        addPreferencesFromResource(R.xml.monet_engine_settings)

        val chromaFactor = Settings.Secure.getFloat(
            context!!.contentResolver, MONET_ENGINE_CHROMA_FACTOR,
                CHROMA_DEFAULT) * 100
        findPreference<CustomSeekBarPreference>(CHROMA_SLIDER_PREF_KEY)?.also {
            it.setValue(chromaFactor.toInt())
        }?.setOnPreferenceChangeListener(this)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean =
        if (preference.key == CHROMA_SLIDER_PREF_KEY) {
            Settings.Secure.putFloat(context!!.contentResolver,
                    MONET_ENGINE_CHROMA_FACTOR, (newValue as Int) / 100f)
        } else {
            false
        }
    
    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is SettingColorPickerPreference) {
            val preferenceDataStore = preference.getSettingsDataStore(context!!)
            var defaultColor: Int = preferenceDataStore.getString(preference.key, null)
                ?.takeIf { it.isNotEmpty() }
                ?.let { Color.parseColor(it) } ?: Color.GREEN
            MonetColorOverrideFragment(
                preference.key,
                preferenceDataStore,
                defaultColor,
            ).let {
                it.setTargetFragment(this, 0)
                it.show(getParentFragmentManager(), TAG)
            }
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    companion object {
        private const val TAG = "MonetEngineSettingsFragment"

        private const val CHROMA_SLIDER_PREF_KEY = "chroma_factor"
        private const val CHROMA_DEFAULT = 1f
    }
}
