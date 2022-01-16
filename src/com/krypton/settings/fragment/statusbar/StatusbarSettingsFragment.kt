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
import android.content.res.Resources
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
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
    private var batteryShowPercentPreference: Preference? = null
    private var isQSsliderEnabled = false
    private var isQQSsliderEnabled = false

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        val combinedSignalIconPref = findPreference<SwitchPreference>(KEY_SHOW_COMBINED_STATUS_BAR_SIGNAL_ICONS)
        val hideCombinedSignalIconsPref = Utils.isWifiOnly(context) ||
            !context!!.getSystemService(UserManager::class.java).isAdminUser() ||
            RestrictedLockUtilsInternal.hasBaseUserRestriction(context,
                UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
                UserHandle.myUserId())
        if (hideCombinedSignalIconsPref) preferenceScreen.removePreference(combinedSignalIconPref)

        context!!.packageManager.getResourcesForApplication(SYSTEMUI_PACKAGE).also { res ->
            val defaultEnabled = getBoolSysUIResource(res, CONFIG_RESOURCE_NAME)
            combinedSignalIconPref?.setChecked(
                Settings.Secure.getIntForUser(context!!.contentResolver,
                    Settings.Secure.SHOW_COMBINED_STATUS_BAR_SIGNAL_ICONS,
                    if (defaultEnabled) 1 else 0,
                    UserHandle.USER_CURRENT) == 1
            )

            val defaultShowCounter = getBoolSysUIResource(res, NOTIF_COUNTER_RESOURCE)
            findPreference<SwitchPreference>(KEY_STATUS_BAR_NOTIF_COUNT)?.setChecked(
                Settings.System.getIntForUser(context!!.contentResolver,
                    Settings.System.STATUS_BAR_NOTIF_COUNT,
                    if (defaultShowCounter) 1 else 0,
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

        batteryShowPercentPreference = findPreference<Preference>(BATTERY_SHOW_PERCENT_PREF_KEY)?.also {
            val isTextMode = Settings.System.getIntForUser(context!!.contentResolver,
                Settings.System.STATUS_BAR_BATTERY_STYLE, 0, UserHandle.USER_CURRENT) == 2
            it.setEnabled(!isTextMode)
        }
        findPreference<Preference>(BATTERY_STYLE_PREF_KEY)?.setOnPreferenceChangeListener(this)
    }

    override protected fun getPreferenceScreenResId() = R.xml.statusbar_settings

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference.key) {
            QS_SHOW_BRIGHTNESS_PREF_KEY -> {
                isQSsliderEnabled = newValue as Boolean
            }
            QQS_SHOW_BRIGHTNESS_PREF_KEY -> {
                isQQSsliderEnabled = newValue as Boolean
            }
            BATTERY_STYLE_PREF_KEY -> {
                val mode = (newValue as String).toInt()
                batteryShowPercentPreference?.setEnabled(mode != 2)
            }
        }
        updateOtherSliderPrefs()
        return true
    }

    override protected fun getLogTag() = TAG

    private fun updateOtherSliderPrefs() {
        val showOtherSliderPrefs = isQSsliderEnabled || isQQSsliderEnabled
        qsBottomSliderPreference?.setEnabled(showOtherSliderPrefs)
        autoBrightnessPreference?.setEnabled(showOtherSliderPrefs)
    }

    private fun getBoolSysUIResource(res: Resources, resName: String, def: Boolean = false): Boolean {
        return res.getIdentifier(resName, BOOL_RES_TYPE, SYSTEMUI_PACKAGE)
            .takeIf { resId -> resId != 0 }
            ?.let { res.getBoolean(it) } ?: def
    }

    companion object {
        private const val TAG = "StatusbarSettingsFragment"

        private const val KEY_SHOW_COMBINED_STATUS_BAR_SIGNAL_ICONS = "show_combined_status_bar_signal_icons"
        private const val CONFIG_RESOURCE_NAME = "flag_combined_status_bar_signal_icons"
        private const val BOOL_RES_TYPE = "bool"
        private const val SYSTEMUI_PACKAGE = "com.android.systemui"

        private const val QS_SHOW_BRIGHTNESS_PREF_KEY = "qs_show_brightness"
        private const val QQS_SHOW_BRIGHTNESS_PREF_KEY = "qqs_show_brightness"
        private const val QS_BOTTOM_BRIGHTNESS_PREF_KEY = "qs_brightness_position_bottom"
        private const val AUTO_BRIGHTNESS_BUTTON_PREF_KEY = "qs_show_auto_brightness_button"

        private const val BATTERY_STYLE_PREF_KEY = "status_bar_battery_style"
        private const val BATTERY_SHOW_PERCENT_PREF_KEY = "status_bar_show_battery_percent"

        private const val KEY_STATUS_BAR_NOTIF_COUNT = "status_bar_notif_count"
        private const val NOTIF_COUNTER_RESOURCE = "config_statusBarShowNumber"
    }
}
