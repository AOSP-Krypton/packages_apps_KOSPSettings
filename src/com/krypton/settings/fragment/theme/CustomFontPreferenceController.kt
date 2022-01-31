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

import android.content.Context
import android.graphics.fonts.FontManager
import android.os.UserHandle
import android.provider.Settings
import android.util.Log

import androidx.preference.Preference
import androidx.preference.PreferenceScreen

import com.android.settings.R
import com.krypton.settings.KryptonBasePreferenceController

import org.json.JSONException
import org.json.JSONObject

class CustomFontPreferenceController(
    private val context: Context,
    private val key: String,
): KryptonBasePreferenceController(context, key) {

    private val fontManager: FontManager

    init {
        fontManager = context.getSystemService(FontManager::class.java)
    }

    override fun getAvailabilityStatus(): Int {
        val overlayPackageJson: String? = Settings.Secure.getStringForUser(
                context.contentResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                UserHandle.USER_CURRENT)
        try {
            val jsonObject = overlayPackageJson?.let { JSONObject(it) } ?: JSONObject()
            val isCustomFontMode = jsonObject.optString(OVERLAY_CATEGORY_FONT).takeIf {
                it.isNotEmpty()
            }?.let {
                it == CUSTOM_FONT_OVERLAY_PACKAGE
            } ?: false
            if (isCustomFontMode) {
                return AVAILABLE
            } else {
                return DISABLED_DEPENDENT_SETTING
            }
        } catch (e: JSONException) {
            Log.e(TAG, "JSONException while processing json, ${e.message}")
        }
        return CONDITIONALLY_UNAVAILABLE
    }

    override fun getSummary() = fontManager.fontConfig.fontFamilies.find {
            it.name?.contains(ThemeSettingsFragment.CUSTOM_FONT_FAMILY_REGULAR_NAME) == true
        }?.fontList?.first()?.postScriptName ?: context.getString(
            R.string.overlay_option_device_default)
    
    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        screen.findPreference<Preference>(FOOTER_PREF_KEY)
            ?.setEnabled(getAvailabilityStatus() == AVAILABLE)
    }

    companion object {
        private const val TAG = "CustomFontPreferenceController"

        private const val OVERLAY_CATEGORY_FONT = "android.theme.customization.font"
        private const val CUSTOM_FONT_OVERLAY_PACKAGE = "com.krypton.theme.font.custom"

        private const val FOOTER_PREF_KEY = "custom_font_preference_footer"
    }
}