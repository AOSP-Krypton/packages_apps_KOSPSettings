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
import android.util.AttributeSet
import android.view.View

import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

import com.android.settings.R

open class TwoTargetPreference(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int,
): Preference(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): this(context, attrs, defStyleAttr, 0)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0, 0)
    constructor(context: Context): this(context, null, 0, 0)

    init {
        setLayoutResource(R.layout.preference_two_target)
        if (getSecondTargetResId() != 0) {
            setWidgetLayoutResource(getSecondTargetResId())
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val shouldHideSecondTarget: Boolean = shouldHideSecondTarget()
        holder.findViewById(R.id.two_target_divider)?.setVisibility(
            if (shouldHideSecondTarget) View.GONE else View.VISIBLE)
        holder.findViewById(android.R.id.widget_frame)?.setVisibility(
            if (shouldHideSecondTarget) View.GONE else View.VISIBLE)
    }

    protected fun shouldHideSecondTarget() = getSecondTargetResId() == 0

    open protected fun getSecondTargetResId() = 0
}
