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

import android.content.Context
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.provider.DeviceConfig
import android.provider.Settings

import androidx.preference.Preference
import androidx.preference.SwitchPreference

import com.android.settings.R
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.Utils
import com.krypton.settings.fragment.KryptonDashboardFragment

class StatusbarSettingsFragment: KryptonDashboardFragment(),
    Preference.OnPreferenceChangeListener {

    private var qsBottomSliderPreference: Preference? = null
    private var autoBrightnessPreference: Preference? = null
    private var isQSsliderEnabled = false
    private var isQQSsliderEnabled = false

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        findPreference<SwitchPreference>(LOCATION_INDICATOR_PREF_KEY)?.also {
            val showLocationIndicator = Settings.Secure.getIntForUser(context!!.contentResolver,
                Settings.Secure.ENABLE_LOCATION_PRIVACY_INDICATOR,
                if (shouldShowLocationIndicator()) 1 else 0,
                UserHandle.USER_CURRENT) == 1
            it.setChecked(showLocationIndicator)
        }

        val combinedSignalIconPref = findPreference<SwitchPreference>(KEY_SHOW_COMBINED_STATUS_BAR_SIGNAL_ICONS)
        val hideCombinedSignalIconsPref = Utils.isWifiOnly(context) ||
            !context!!.getSystemService(UserManager::class.java).isAdminUser() ||
            RestrictedLockUtilsInternal.hasBaseUserRestriction(context,
                UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
                UserHandle.myUserId())
        if (hideCombinedSignalIconsPref) preferenceScreen.removePreference(combinedSignalIconPref)
        context!!.packageManager.getResourcesForApplication(SYSTEMUI_PACKAGE).also { res ->
            val defaultEnabled: Boolean = res.getIdentifier(CONFIG_RESOURCE_NAME, BOOL_RES_TYPE, SYSTEMUI_PACKAGE)
                .takeIf { resId -> resId != 0 }
                ?.let { res.getBoolean(it) } ?: false
            combinedSignalIconPref?.setChecked(
                Settings.Secure.getIntForUser(context!!.contentResolver,
                    Settings.Secure.SHOW_COMBINED_STATUS_BAR_SIGNAL_ICONS,
                    if (defaultEnabled) 1 else 0,
                    UserHandle.USER_CURRENT) == 1
            )
        }

        findPreference<SwitchPreference>(QS_SHOW_BRIGHTNESS_PREF_KEY)?.let {
            it.setOnPreferenceChangeListener(this)
            isQSsliderEnabled = it.isChecked()
        }
        findPreference<SwitchPreference>(QQS_SHOW_BRIGHTNESS_PREF_KEY)?.let {
            it.setOnPreferenceChangeListener(this)
            isQQSsliderEnabled = it.isChecked()
        }
        qsBottomSliderPreference = findPreference<Preference>(QS_BOTTOM_BRIGHTNESS_PREF_KEY)
        val isAutoBrightnessAvailable = context!!.resources.getBoolean(
            com.android.internal.R.bool.config_automatic_brightness_available)
        if (!isAutoBrightnessAvailable) {
            removePreference(AUTO_BRIGHTNESS_BUTTON_PREF_KEY)
        } else {
            autoBrightnessPreference = findPreference<Preference>(AUTO_BRIGHTNESS_BUTTON_PREF_KEY)
        }
        updateOtherSliderPrefs()
    }

    override protected fun getPreferenceScreenResId() = R.xml.statusbar_settings

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val result = when (preference.key) {
            QS_SHOW_BRIGHTNESS_PREF_KEY -> {
                isQSsliderEnabled = newValue as Boolean
                true
            }
            QQS_SHOW_BRIGHTNESS_PREF_KEY -> {
                isQQSsliderEnabled = newValue as Boolean
                true
            }
            else -> false
        }
        updateOtherSliderPrefs()
        return result
    }

    override protected fun getLogTag() = TAG

    private fun updateOtherSliderPrefs() {
        val showOtherSliderPrefs = isQSsliderEnabled || isQQSsliderEnabled
        qsBottomSliderPreference?.setEnabled(showOtherSliderPrefs)
        autoBrightnessPreference?.setEnabled(showOtherSliderPrefs)
    }

    companion object {
        private const val TAG = "StatusbarSettingsFragment"

        private const val LOCATION_INDICATOR_PREF_KEY = "enable_location_privacy_indicator"

        private fun shouldShowLocationIndicator() = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY,
                "location_indicators_enabled", false)

        private const val KEY_SHOW_COMBINED_STATUS_BAR_SIGNAL_ICONS = "show_combined_status_bar_signal_icons"
        private const val CONFIG_RESOURCE_NAME = "flag_combined_status_bar_signal_icons"
        private const val BOOL_RES_TYPE = "bool"
        private const val SYSTEMUI_PACKAGE = "com.android.systemui"

        private const val QS_SHOW_BRIGHTNESS_PREF_KEY = "qs_show_brightness"
        private const val QQS_SHOW_BRIGHTNESS_PREF_KEY = "qqs_show_brightness"
        private const val QS_BOTTOM_BRIGHTNESS_PREF_KEY = "qs_brightness_position_bottom"
        private const val AUTO_BRIGHTNESS_BUTTON_PREF_KEY = "qs_show_auto_brightness_button"
    }
}
