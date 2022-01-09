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

package com.krypton.settings.fragment

import androidx.preference.Preference

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.dashboard.DashboardFragment
import com.krypton.settings.preference.SettingColorPickerPreference

abstract class KryptonDashboardFragment: DashboardFragment() {
    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.KRYPTON

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is SettingColorPickerPreference) {
            childFragmentManager.setFragmentResultListener(REQUEST_KEY, this) { _, resultBundle ->
                preference.setSummary(resultBundle.getString(ColorPickerFragment.BUNDLE_KEY))
            }
            ColorPickerFragment(
                preference.key,
                preference.getSettingsDataStore(requireContext()),
                preference.getColor()
            ).show(childFragmentManager, preference.key)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    companion object {
        const val REQUEST_KEY = "KryptonDashboardFragment#RequestKey"
    }
}
