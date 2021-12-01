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

import com.krypton.settings.fragment.KryptonDashboardFragment
import com.android.settings.R

class ThemeSettingsFragment: KryptonDashboardFragment() {
    override protected fun getPreferenceScreenResId() = R.xml.theme_settings

    override protected fun getLogTag() = TAG

    companion object {
        private const val TAG = "ThemeSettingsFragment"
    }
}
