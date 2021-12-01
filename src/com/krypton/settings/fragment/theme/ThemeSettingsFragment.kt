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

import com.android.internal.util.krypton.KryptonUtils
import com.android.settings.R
import com.android.settingslib.core.AbstractPreferenceController
import com.krypton.settings.fragment.KryptonDashboardFragment

class ThemeSettingsFragment: KryptonDashboardFragment() {

    override protected fun getPreferenceScreenResId() = R.xml.theme_settings

    override protected fun createPreferenceControllers(
        context: Context
    ): List<AbstractPreferenceController> {
        val isAospLauncherInstalled = KryptonUtils.isPackageInstalled(
            context, TARGET_LAUNCHER, false /** ignoreState */
        )
        val isAospThemePickerInstalled = KryptonUtils.isPackageInstalled(
            context, TARGET_THEME_PICKER, false /** ignoreState */
        )
        return listOf(
            ThemeOverlayPreferenceController(context,
                "font_list_preference",
                mapOf(OVERLAY_CATEGORY_FONT to TARGET_ANDROID),
            ),
            ThemeOverlayPreferenceController(context,
                "icon_pack_list_preference",
                mutableMapOf(
                    OVERLAY_CATEGORY_ICON_ANDROID to TARGET_ANDROID,
                    OVERLAY_CATEGORY_ICON_SYSUI to TARGET_SYSUI,
                    OVERLAY_CATEGORY_ICON_SETTINGS to TARGET_SETTINGS,
                ).also {
                    // Conditionally add launcher and themepicker
                    if (isAospLauncherInstalled) it.put(
                        OVERLAY_CATEGORY_ICON_LAUNCHER, TARGET_LAUNCHER)
                    if (isAospThemePickerInstalled) it.put(
                        OVERLAY_CATEGORY_ICON_THEME_PICKER, TARGET_THEME_PICKER)
                },
            )
        )
    }

    override protected fun getLogTag() = TAG

    companion object {
        private const val TAG = "ThemeSettingsFragment"

        private const val OVERLAY_CATEGORY_FONT = "android.theme.customization.font"
        private const val OVERLAY_CATEGORY_ICON_ANDROID = "android.theme.customization.icon_pack.android"
        private const val OVERLAY_CATEGORY_ICON_SYSUI = "android.theme.customization.icon_pack.systemui"
        private const val OVERLAY_CATEGORY_ICON_SETTINGS = "android.theme.customization.icon_pack.settings"
        private const val OVERLAY_CATEGORY_ICON_LAUNCHER = "android.theme.customization.icon_pack.launcher"
        private const val OVERLAY_CATEGORY_ICON_THEME_PICKER = "android.theme.customization.icon_pack.themepicker"

        private const val TARGET_ANDROID = "android"
        private const val TARGET_SYSUI = "com.android.systemui"
        private const val TARGET_SETTINGS = "com.android.settings"
        private const val TARGET_LAUNCHER = "com.android.launcher3"
        private const val TARGET_THEME_PICKER = "com.android.wallpaper"
    }
}
