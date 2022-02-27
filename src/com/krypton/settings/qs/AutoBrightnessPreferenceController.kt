/*
 * Copyright (C) 2022 AOSP-Krypton Project
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
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings

import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen

import com.android.internal.R
import com.android.settingslib.core.lifecycle.Lifecycle
import com.krypton.settings.KryptonBasePreferenceController

class AutoBrightnessPreferenceController(
    context: Context,
    lifecycle: Lifecycle?,
) : KryptonBasePreferenceController(context, KEY),
    LifecycleEventObserver {

    private val settingsObserver = object : ContentObserver(
        Handler(Looper.getMainLooper())
    ) {
        override fun onChange(selfChange: Boolean) {
            preference?.let {
                updateState(it)
            }
        }
    }

    private var preference: Preference? = null

    init {
        lifecycle?.addObserver(this)
    }

    override fun onStateChanged(owner: LifecycleOwner, event: Event) {
        if (event == Event.ON_START) {
            mContext.contentResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.QS_SHOW_BRIGHTNESS),
                false /* notifyDescendants */,
                settingsObserver,
            )
            mContext.contentResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.QQS_SHOW_BRIGHTNESS),
                false /* notifyDescendants */,
                settingsObserver,
            )
        } else if (event == Event.ON_STOP) {
            mContext.contentResolver.unregisterContentObserver(settingsObserver)
        }
    }

    override fun getAvailabilityStatus(): Int {
        val available = mContext.resources.getBoolean(
            R.bool.config_automatic_brightness_available)
        if (!available) return UNSUPPORTED_ON_DEVICE
        val qsSliderEnabled = Settings.System.getIntForUser(
            mContext.contentResolver,
            Settings.System.QS_SHOW_BRIGHTNESS,
            0, UserHandle.USER_CURRENT
        ) == 1
        val qqsSliderEnabled = Settings.System.getIntForUser(
            mContext.contentResolver,
            Settings.System.QQS_SHOW_BRIGHTNESS,
            0, UserHandle.USER_CURRENT
        ) == 1
        return if (qsSliderEnabled || qqsSliderEnabled) {
            AVAILABLE
        } else {
            DISABLED_DEPENDENT_SETTING
        }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(KEY)
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        preference.setEnabled(getAvailabilityStatus() == AVAILABLE)
    }

    companion object {
        private const val KEY = "qs_show_auto_brightness_button"
    }
}