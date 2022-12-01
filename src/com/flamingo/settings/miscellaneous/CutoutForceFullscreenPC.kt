/*
 * Copyright (C) 2022 Flamingo-OS Project
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
 * limitations under the License
 */

package com.flamingo.settings.miscellaneous

import android.content.Context

import com.android.settings.core.BasePreferenceController

class CutoutForceFullscreenPC(
    context: Context,
    preferenceKey: String
) : BasePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus(): Int {
        val displayCutout = mContext.getString(
            com.android.internal.R.string.config_mainBuiltInDisplayCutout)
        return if (displayCutout.isBlank()) {
            CONDITIONALLY_UNAVAILABLE
        } else {
            AVAILABLE
        }
    }
}
