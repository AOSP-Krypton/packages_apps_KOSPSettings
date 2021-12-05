/*
 * Copyright (C) 2017 The Android Open Source Project
 *               2021 AOSP-Krypton Project
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

package com.krypton.settings.preference

import android.content.Context
import android.content.res.TypedArray
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.widget.Switch

import androidx.preference.PreferenceViewHolder

import com.android.settings.R
import com.android.settingslib.widget.TwoTargetPreference

/**
 * A custom preference that provides inline switch toggle. It has a mandatory field for title, and
 * optional fields for icon and sub-text.
 */
open class MasterSwitchPreference(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int,
): TwoTargetPreference(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): this(context, attrs, defStyleAttr, 0)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0, 0)
    constructor(context: Context): this(context, null, 0, 0)

    private var switch: Switch? = null
    private var checked = false
    private var enableSwitch = true

    override protected fun getSecondTargetResId() = R.layout.preference_widget_master_switch

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.setDividerAllowedAbove(false)
        holder.setDividerAllowedBelow(false)
        switch = holder.findViewById(R.id.switchWidget) as? Switch
        holder.findViewById(android.R.id.widget_frame)?.setOnClickListener {
            if (switch?.isEnabled() == false) {
                return@setOnClickListener
            }
            setChecked(!checked)
            if (!callChangeListener(checked)) {
                setChecked(!checked)
            } else {
                persistBoolean(checked)
            }
            setSelectable(checked)
        }
        switch?.let {
            it.contentDescription = title
            it.setChecked(checked)
            it.setEnabled(enableSwitch)
        }
    }

    fun isChecked() = checked

    fun setChecked(checked: Boolean) {
        this.checked = checked
        switch?.setChecked(checked)
    }

    fun setSwitchEnabled(enabled: Boolean) {
        enableSwitch = enabled
        switch?.setEnabled(enableSwitch)
    }

    fun getSwitch() = switch

    override protected fun onGetDefaultValue(a: TypedArray, index: Int): Any? =
        a.getBoolean(index, false)
    
    override protected fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        defaultValue?.let {
            if (it is Boolean) {
                val peristedBool = if (restoreValue) getPersistedBoolean(it) else it
                setSelectable(peristedBool)
                setChecked(peristedBool)
            }
        }
    }

    override protected fun persistBoolean(value: Boolean): Boolean {
        if (shouldPersist()) {
            if (value == getPersistedBoolean(!value)) {
                // It's already there, so the same as persisting
                return true
            }
            preferenceDataStore?.putInt(getKey(), if (value) 1 else 0)
            return true
        }
        return false
    }

    override protected fun getPersistedBoolean(defaultReturnValue: Boolean): Boolean {
        if (!shouldPersist()) {
            return defaultReturnValue
        }
        return preferenceDataStore?.let { it.getInt(key,
            if (defaultReturnValue) 1 else 0) == 1 } ?: defaultReturnValue
    }
}
