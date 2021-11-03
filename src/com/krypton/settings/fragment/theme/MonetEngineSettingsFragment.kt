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

import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.provider.Settings.Secure.MONET_ENGINE_CHROMA_FACTOR
import android.provider.Settings.Secure.MONET_ENGINE_COLOR_OVERRIDE
import android.widget.Toast

import androidx.preference.EditTextPreference
import androidx.preference.Preference

import com.krypton.settings.fragment.BaseFragment
import com.krypton.settings.preference.CustomSeekBarPreference
import com.krypton.settings.Utils
import com.android.settings.R

class MonetEngineSettingsFragment: BaseFragment(),
        Preference.OnPreferenceChangeListener {
    
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        addPreferencesFromResource(R.xml.monet_engine_settings)

        findPreference<EditTextPreference>(COLOR_OVERRIDE_PREF_KEY)?.also {
            it.setText(Settings.Secure.getString(context!!.contentResolver,
                MONET_ENGINE_COLOR_OVERRIDE))
        }?.setOnPreferenceChangeListener(this)

        val chromaFactor = Settings.Secure.getFloat(
            context!!.contentResolver, MONET_ENGINE_CHROMA_FACTOR,
                CHROMA_DEFAULT) * 100
        findPreference<CustomSeekBarPreference>(CHROMA_SLIDER_PREF_KEY)?.also {
            it.setValue(chromaFactor.toInt())
        }?.setOnPreferenceChangeListener(this)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean =
        when (preference.key) {
            CHROMA_SLIDER_PREF_KEY -> {
                Settings.Secure.putFloat(context!!.contentResolver,
                    MONET_ENGINE_CHROMA_FACTOR, (newValue as Int) / 100f)
            }
            COLOR_OVERRIDE_PREF_KEY -> {
                val color = newValue as String?
                if (color != null && color.isNotEmpty() && !isProperColor(color)) {
                    Toast.makeText(context!!, R.string.invalid_color_input,
                        Toast.LENGTH_SHORT).show()
                    false
                } else {
                    Settings.Secure.putString(context!!.contentResolver,
                        MONET_ENGINE_COLOR_OVERRIDE, color)
                }
            }
            else -> false
        }

    companion object {
        private const val COLOR_OVERRIDE_PREF_KEY = "color_override"

        private const val CHROMA_SLIDER_PREF_KEY = "chroma_factor"
        private const val CHROMA_DEFAULT = 1f

        private fun isProperColor(color: String): Boolean {
            if (color.length != 7 || !color.startsWith("#")) {
                return false
            }
            return Utils.HEX_PATTERN.matcher(color.substring(1, 7)).matches()
        }
    }
}
