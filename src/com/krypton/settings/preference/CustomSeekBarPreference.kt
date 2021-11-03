/*
 * Copyright (C) 2016-2017 The Dirty Unicorns Project
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
 * limitations under the License
 */

package com.krypton.settings.preference

import android.content.Context
import android.content.res.TypedArray
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

import androidx.core.content.res.TypedArrayUtils
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

import com.android.settings.R

open class CustomSeekBarPreference(
    private val context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int,
): Preference(
    context,
    attrs,
    defStyleAttr,
    defStyleRes,
), SeekBar.OnSeekBarChangeListener, View.OnClickListener, View.OnLongClickListener {

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
    ): this(context, attrs, defStyleAttr, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
    ): this(
        context,
        attrs,
        TypedArrayUtils.getAttr(context,
            androidx.preference.R.attr.preferenceStyle,
            android.R.attr.preferenceStyle),
        )

    constructor(context: Context): this(context, null)

    private var interval = 1
    private var units = " "
    private var showSign: Boolean
    private var continuousUpdates = false

    private var minValue = 0
    private var maxValue = 0
    private var defaultValueExists = false
    private var defaultValue: Int = 0
    var seekBarValue = 0

    private var valueTextView: TextView? = null
    private var resetImageView: ImageView? = null
    private var minusImageView: ImageView? = null
    private var plusImageView: ImageView? = null
    private var seekBar: SeekBar

    private var trackingTouch = false
    private var trackingValue: Int

    init {
        context.obtainStyledAttributes(attrs, R.styleable.CustomSeekBarPreference).use {
            showSign = it.getBoolean(R.styleable.CustomSeekBarPreference_showSign, false)
            it.getString(R.styleable.CustomSeekBarPreference_units)?.let { units += it }
            continuousUpdates = it.getBoolean(R.styleable.CustomSeekBarPreference_continuousUpdates, continuousUpdates)
        }

        minValue = attrs?.getAttributeIntValue(SETTINGS_NS, "min", minValue) ?: minValue
        maxValue = attrs?.getAttributeIntValue(ANDROID_NS, "max", minValue) ?: minValue
        if (maxValue < minValue) {
            throw IllegalStateException("Max value $maxValue is less than min value $minValue")
        }

        try {
            attrs?.getAttributeValue(SETTINGS_NS, "interval")?.let { interval = Integer.parseInt(it) }
            if (interval > (maxValue - minValue)) {
                throw IllegalStateException("Interval $interval is out of range")
            }
        } catch (ex: NumberFormatException) {
            Log.e(TAG, "Invalid interval value, ${ex.message}")
        }

        val def: String? = attrs?.getAttributeValue(ANDROID_NS, "defaultValue")
        def?.takeIf{ it.isNotEmpty() }?.let {
            try {
                defaultValue = def.toInt()
                if (defaultValue < minValue || defaultValue > maxValue) {
                    throw IllegalStateException("Default value $defaultValue is out of range")
                }
                defaultValueExists = true
                seekBarValue = defaultValue
            } catch (ex: NumberFormatException) {
                Log.e(TAG, "Invalid default value, $ex")
                seekBarValue = minValue
            }
        } ?: run {
            seekBarValue = minValue
        }
        trackingValue = seekBarValue

        seekBar = SeekBar(context, attrs)
        setLayoutResource(R.layout.preference_custom_seekbar)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        try {
            // move our seekbar to the new view we've been given
            val oldContainer: ViewParent? = seekBar.getParent()
            val newContainer: ViewGroup? = holder.findViewById(R.id.seekbar) as ViewGroup
            if (oldContainer != newContainer) {
                // remove the seekbar from the old view
                oldContainer?.let { (oldContainer as ViewGroup).removeView(seekBar) }
                // remove the existing seekbar (there may not be one) and add ours
                newContainer?.let {
                    it.removeAllViews()
                    it.addView(seekBar, ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT)
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error binding view", ex)
        }

        seekBar.setMax(maxValue)
        seekBar.setProgress(seekBarValue)
        seekBar.setEnabled(isEnabled())

        valueTextView = holder.findViewById(R.id.value) as TextView?
        resetImageView = holder.findViewById(R.id.reset) as ImageView?
        minusImageView = holder.findViewById(R.id.minus) as ImageView?
        plusImageView = holder.findViewById(R.id.plus) as ImageView?

        updateValueViews()

        seekBar.setOnSeekBarChangeListener(this)

        resetImageView?.setOnClickListener(this)
        minusImageView?.setOnClickListener(this)
        plusImageView?.setOnClickListener(this)

        resetImageView?.setOnLongClickListener(this)
    }

    private fun updateValueViews() {
        valueTextView?.let {
            var text = context.getString(R.string.custom_seekbar_value);
            if (trackingTouch) {
                text += " $trackingValue"
            } else {
                text += " $seekBarValue"
            }
            if (showSign) {
                text += units
            }
            if (!trackingTouch && defaultValueExists && seekBarValue == defaultValue) {
                text += " (" + context.getString(R.string.custom_seekbar_default_value) + ")"
            }
            it.setText(text)
        }
        resetImageView?.let {
            if (!defaultValueExists || seekBarValue == defaultValue || trackingTouch)
                it.setVisibility(View.INVISIBLE)
            else
                it.setVisibility(View.VISIBLE)
        }
        minusImageView?.let {
            if (seekBarValue == minValue || trackingTouch) {
                it.setClickable(false)
                it.setColorFilter(context.getColor(R.color.disabled_text_color),
                    PorterDuff.Mode.MULTIPLY)
            } else {
                it.setClickable(true)
                it.clearColorFilter()
            }
        }
        plusImageView?.let {
            if (seekBarValue == maxValue || trackingTouch) {
                it.setClickable(false)
                it.setColorFilter(context.getColor(R.color.disabled_text_color), PorterDuff.Mode.MULTIPLY)
            } else {
                it.setClickable(true)
                it.clearColorFilter()
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (trackingTouch && !continuousUpdates) {
            trackingValue = progress
        } else if (seekBarValue != progress) {
            // change rejected, revert to the previous seekBarValue
            if (!callChangeListener(progress)) {
                seekBar.setProgress(seekBarValue)
                return
            }
            // change accepted, store it
            persistInt(progress)

            seekBarValue = progress
        }
        updateValueViews()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        trackingTouch = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        trackingTouch = false
        if (!continuousUpdates) onProgressChanged(seekBar, trackingValue, false)
        notifyChanged()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.reset -> {
                Toast.makeText(context, context.getString(R.string.custom_seekbar_default_value_to_set,
                    defaultValue), Toast.LENGTH_LONG).show()
            }
            R.id.minus -> {
                setValue(Math.max(seekBarValue - interval, minValue))
            }
            R.id.plus -> {
                setValue(Math.min(seekBarValue + interval, maxValue))
            }
        }
    }

    override fun onLongClick(v: View): Boolean {
        if (v.id == R.id.reset) {
            setValue(defaultValue)
            return false
        }
        return true
    }

    override protected fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        if (restoreValue) seekBarValue = getPersistedInt(minValue)
    }

    override fun setDefaultValue(defaultValue: Any?) {
        if (defaultValue is Int?)
            defaultValue?.let { setDefaultValue(it) }
        else if (defaultValue != null) {
            setDefaultValue((defaultValue as String).toInt())
        }
    }

    fun setDefaultValue(newValue: Int) {
        if (newValue < minValue || newValue > maxValue) {
            throw IllegalArgumentException("New default value $newValue is out of range")
        }
        if (!defaultValueExists || defaultValue != newValue) {
            defaultValue = newValue
            defaultValueExists = true
            updateValueViews()
        }
    }

    fun setMax(max: Int) {
        maxValue = max
        seekBar.setMax(maxValue)
    }

    fun setMin(min: Int) {
        minValue = min
        seekBar.setMin(minValue)
    }

    fun setValue(newValue: Int) {
        if (seekBarValue != newValue) {
            seekBarValue = newValue
            seekBar.setProgress(seekBarValue)
            updateValueViews()             
        }
    }

    companion object {
        private const val TAG = "CustomSeekBarPreference"
        private const val SETTINGS_NS = "http://schemas.android.com/apk/res-auto"
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
