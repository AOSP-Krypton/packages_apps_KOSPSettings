/*
 * Copyright (C) 2017 AICP
 *               2021 AOSP-Krypton Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.krypton.settings.preference

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet

import androidx.preference.PreferenceDataStore
import androidx.preference.ListPreference

abstract class SettingListPreference(
    context: Context,
    attrs: AttributeSet,
): ListPreference(
    context,
    attrs,
) {
    init {
        setPreferenceDataStore(getSettingsDataStore(context))
    }

    override protected fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        return a.getString(index)
    }

    override protected fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        defaultValue?.let {
            if (it is String) {
                setValue(if (restoreValue) getPersistedString(it) else it)
            }
        }
    }

    abstract fun getSettingsDataStore(context: Context): PreferenceDataStore
}
