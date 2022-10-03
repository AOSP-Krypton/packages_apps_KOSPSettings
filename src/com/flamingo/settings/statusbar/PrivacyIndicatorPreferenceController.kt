/*
 * Copyright (C) 2022 FlamingoOS Project
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

package com.flamingo.settings.statusbar

import android.content.Context
import android.provider.DeviceConfig

import com.flamingo.settings.FlamingoTogglePreferenceController

class PrivacyIndicatorPreferenceController(
    context: Context,
    preferenceKey: String,
) : FlamingoTogglePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus() = AVAILABLE

    override fun isChecked() = DeviceConfig.getBoolean(
        DeviceConfig.NAMESPACE_PRIVACY,
        preferenceKey,
        true
    )

    override fun setChecked(checked: Boolean): Boolean {
        return DeviceConfig.setProperty(
            DeviceConfig.NAMESPACE_PRIVACY,
            preferenceKey,
            checked.toString(),
            false /* makeDefault */
        )
    }
}
