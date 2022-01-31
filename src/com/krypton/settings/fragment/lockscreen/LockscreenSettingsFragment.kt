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

import android.os.Bundle

import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import com.krypton.settings.Utils
import com.krypton.settings.fragment.KryptonDashboardFragment

@SearchIndexable
class LockscreenSettingsFragment : KryptonDashboardFragment() {

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        if (!Utils.hasUDFPS(activity!!)) {
            removePreference(SCREEN_OFF_FOD_KEY)
        }
    }

    override protected fun getPreferenceScreenResId() = R.xml.lockscreen_settings

    override protected fun getLogTag() = TAG

    companion object {
        private const val TAG = "LockscreenSettingsFragment"

        private const val SCREEN_OFF_FOD_KEY = "screen_off_fod"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.lockscreen_settings)
    }
}
