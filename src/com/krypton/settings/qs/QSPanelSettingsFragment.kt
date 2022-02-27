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

package com.krypton.settings.qs

import android.content.Context

import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.core.lifecycle.Lifecycle
import com.android.settingslib.search.SearchIndexable
import com.krypton.settings.KryptonDashboardFragment

@SearchIndexable
class QSPanelSettingsFragment : KryptonDashboardFragment() {

    override protected fun getPreferenceScreenResId() = R.xml.qs_settings

    override protected fun getLogTag() = TAG

    override protected fun createPreferenceControllers(
        context: Context
    ): List<AbstractPreferenceController> = buildPreferenceControllers(context, settingsLifecycle)

    companion object {
        private const val TAG = "QSSettingsFragment"

        private fun buildPreferenceControllers(
            context: Context,
            lifecycle: Lifecycle?,
        ): List<AbstractPreferenceController> = listOf(
            AutoBrightnessPreferenceController(context, lifecycle),
            BottomBrightnessSliderPreferenceController(context, lifecycle),
        )

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.qs_settings) {
            override fun createPreferenceControllers(
                context: Context
            ): List<AbstractPreferenceController> = buildPreferenceControllers(
                context, null /* lifecycle */)
        }
    }
}
