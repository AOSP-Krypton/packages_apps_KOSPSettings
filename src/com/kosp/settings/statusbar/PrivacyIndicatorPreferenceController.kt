/*
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

package com.kosp.settings.statusbar

import android.content.Context
import android.os.UserHandle
import android.provider.DeviceConfig
import android.provider.Settings

import com.kosp.settings.KOSPTogglePreferenceController

class PrivacyIndicatorPreferenceController(
    context: Context,
    preferenceKey: String,
) : KOSPTogglePreferenceController(context, preferenceKey) {

    private val defaultValue = DeviceConfig.getBoolean(
        DeviceConfig.NAMESPACE_PRIVACY,
        preferenceKey,
        true
    )

    override fun getAvailabilityStatus() = AVAILABLE

    override fun isChecked() = Settings.Secure.getIntForUser(
        mContext.contentResolver,
        preferenceKey,
        if (defaultValue) 1 else 0,
        UserHandle.USER_CURRENT
    ) == 1

    override fun setChecked(checked: Boolean) = Settings.Secure.putIntForUser(
        mContext.contentResolver,
        preferenceKey,
        if (checked) 1 else 0,
        UserHandle.USER_CURRENT
    )
}
