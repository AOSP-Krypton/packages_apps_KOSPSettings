/*
 * Copyright (C) 2019 The Android Open Source Project
 * Copyright (C) 2021-2023 AOSP-Krypton Project
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

package com.kosp.settings.gestures

import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import com.kosp.settings.KOSPDashboardFragment

@SearchIndexable
class SwipeToScreenshotSettings : KOSPDashboardFragment() {

    override protected fun getPreferenceScreenResId() = R.xml.swipe_to_screenshot_settings

    override protected fun getLogTag() = TAG

    companion object {
        private const val TAG = "SwipeToScreenshotSettings"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.swipe_to_screenshot_settings)
    }
}
