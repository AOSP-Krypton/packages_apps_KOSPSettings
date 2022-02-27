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

package com.krypton.settings.fragment.lockscreen

import android.content.Context
import android.os.Bundle
import android.widget.Switch

import androidx.preference.forEachIndexed

import com.android.settings.R
import com.krypton.settings.fragment.KryptonDashboardFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.core.lifecycle.Lifecycle
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.OnMainSwitchChangeListener
import com.android.settingslib.widget.TopIntroPreference

@SearchIndexable
class EdgeLightSettingsFragment : KryptonDashboardFragment(),
    OnMainSwitchChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        findPreference<MainSwitchPreference>(MAIN_SWITCH_KEY)?.also {
            updatePreferences(it.isChecked)
            it.addOnSwitchChangeListener(this)
        }
    }

    override protected fun getPreferenceScreenResId() = R.xml.edge_light_settings

    override protected fun getLogTag() = TAG

    override protected fun createPreferenceControllers(
        context: Context
    ): List<AbstractPreferenceController> = buildPreferenceControllers(context, settingsLifecycle)

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

        private fun buildPreferenceControllers(
            context: Context,
            lifecycle: Lifecycle?,
        ): List<AbstractPreferenceController> = listOf(
            EdgeLightColorPickerPreferenceController(context, lifecycle)
        )

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.edge_light_settings) {
            override fun createPreferenceControllers(
                context: Context
            ): List<AbstractPreferenceController> = buildPreferenceControllers(
                context, null /* lifecycle */)
        }
    }
}