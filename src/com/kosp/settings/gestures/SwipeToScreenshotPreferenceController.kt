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

import android.content.Context
import android.provider.Settings
import android.provider.Settings.System.THREE_FINGER_GESTURE

import com.android.settings.R
import com.android.settings.gestures.GesturePreferenceController

class SwipeToScreenshotPreferenceController(
    context: Context, preferenceKey: String
) : GesturePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun isSliceable() = true

    override protected fun getVideoPrefKey() = PREF_KEY_VIDEO

    override fun setChecked(isChecked: Boolean): Boolean =
        Settings.System.putInt(mContext.contentResolver,
            THREE_FINGER_GESTURE, if (isChecked) 1 else 0)

    override fun isChecked() =
        Settings.System.getInt(mContext.contentResolver,
            THREE_FINGER_GESTURE, 0) == 1

    override fun getSliceHighlightMenuRes() = R.string.menu_key_system

    companion object {
        private const val PREF_KEY_VIDEO = "swipe_to_screenshot_video"
    }
}