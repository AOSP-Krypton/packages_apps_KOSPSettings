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
package com.krypton.settings.fragment

import android.annotation.ColorInt
import android.content.Context
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.Spanned
import android.view.HapticFeedbackConstants.KEYBOARD_PRESS
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView

import androidx.core.graphics.ColorUtils
import androidx.fragment.app.DialogFragment.STYLE_NORMAL
import androidx.preference.PreferenceDataStore

import com.android.settings.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.krypton.settings.Utils

abstract class ColorPickerFragment(@ColorInt initialColor: Int): BottomSheetDialogFragment(),
        RadioGroup.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {

    private val hueGradientColors = intArrayOf(
        Utils.HSVToColor(0f, 1f, 1f),
        Utils.HSVToColor(60f, 1f, 1f),
        Utils.HSVToColor(120f, 1f, 1f),
        Utils.HSVToColor(180f, 1f, 1f),
        Utils.HSVToColor(240f, 1f, 1f),
        Utils.HSVToColor(300f, 1f, 1f),
        Utils.HSVToColor(360f, 1f, 1f),
    )

    private lateinit var colorPreview: View
    private lateinit var colorInput: EditText
    private lateinit var seekBarOne: SeekBar
    private lateinit var seekBarTwo: SeekBar
    private lateinit var seekBarThree: SeekBar
    private var colorModel = ColorModel.RGB
    private var textInputChangedInternal = false // Internal variable to prevent loops with TextWatcher

    @ColorInt
    private var color = initialColor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT)
        setStyle(STYLE_NORMAL, R.style.ColorPickerStyle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.color_picker_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)  {
        colorPreview = view.findViewById(R.id.color_preview)
        colorInput = view.findViewById(R.id.color_input)

        colorInput.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Not implemented
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Not implemented
            }

            override fun afterTextChanged(s: Editable) {
                if (textInputChangedInternal) {
                    // Reset it here
                    textInputChangedInternal = false
                } else if (s.length == 7) {
                    color = Color.parseColor(s.toString())
                    updateSliders()
                    updateSliderGradients(false)
                    previewColor(true)
                }
            }
        });

        colorInput.setFilters(arrayOf<InputFilter>(
            InputFilter.LengthFilter(7),
            object: InputFilter {
                override fun filter(
                    source: CharSequence,
                    start: Int,
                    end: Int,
                    dest: Spanned,
                    dstart: Int,
                    dend: Int,
                ): String? {
                    // Deletion
                    if (start == 0 && end == 0) {
                        return null
                    }
                    var hexSubString = source
                    if ((end - start) == 7) {
                        hexSubString = source.subSequence(1, 7)
                        if (!source.get(0).equals('#')) {
                            return ""
                        }
                    }
                    if (Utils.HEX_PATTERN.matcher(hexSubString).matches()) {
                        return null
                    } else {
                        return ""
                    }
                }
            }
        ));

        val cancelButton: Button = view.findViewById(R.id.cancel_button)
        cancelButton.setOnClickListener(View.OnClickListener{
            it.performHapticFeedback(KEYBOARD_PRESS)
            dialog?.dismiss()
        })

        val confirmButton: Button = view.findViewById(R.id.confirm_button)
        confirmButton.setOnClickListener({
            it.performHapticFeedback(KEYBOARD_PRESS)
            dialog?.dismiss()
            colorInput.text.toString().let {
                if (it.isEmpty() || it.length == 7) {
                    persistValue(it)
                }
            }
        })

        seekBarOne = view.findViewById(R.id.seekBar1)
        seekBarTwo = view.findViewById(R.id.seekBar2)
        seekBarThree = view.findViewById(R.id.seekBar3)

        /*
         * Set the drawables as mutable so that they
         * do not share a constant state or else all
         * three slider gradients will look alike
         */
        seekBarOne.getProgressDrawable().mutate()
        seekBarTwo.getProgressDrawable().mutate()
        seekBarThree.getProgressDrawable().mutate()

        // Register progress change listeners
        seekBarTwo.setOnSeekBarChangeListener(this)
        seekBarOne.setOnSeekBarChangeListener(this)
        seekBarThree.setOnSeekBarChangeListener(this)

        // Register listener for color model change
        val colorModelGroup: RadioGroup = view.findViewById(R.id.color_model_group)
        colorModelGroup.setOnCheckedChangeListener(this)

        // Update sliders and preview
        updateSliderMax()
        updateSliders()
        updateSliderGradients(true)
        previewColor(false)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (!fromUser) {
            return
        }
        when (colorModel) {
            ColorModel.RGB -> {
                color = Color.rgb(seekBarOne.progress, seekBarTwo.progress,
                    seekBarThree.progress)
            }
            ColorModel.HSV -> {
                color = Utils.HSVToColor(seekBarOne.progress.toFloat(), seekBarTwo.progress / 100f,
                    seekBarThree.progress / 100f)
                updateSliderGradients(false)
            }
            ColorModel.HSL -> {
                color = Utils.HSLToColor(seekBarOne.progress.toFloat(), seekBarTwo.progress / 100f,
                    seekBarThree.progress / 100f)
                updateSliderGradients(false)
            }
        }
        previewColor(false)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        // Not implemented
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        // Not implemented
    }

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        when (checkedId) {
            R.id.rgb_button -> { colorModel = ColorModel.RGB }
            R.id.hsv_button -> { colorModel = ColorModel.HSV }
            R.id.hsl_button -> { colorModel = ColorModel.HSL }
        }
        updateSliderMax()
        updateSliders()
        updateSliderGradients(true)
    }

    /*
     * Called when confirm button of the dialog is pressed.
     * @param hexColor will be with # prefix and of RGB type
     */
    abstract fun persistValue(hexColor: String); 

    /**
     * Used to update sliders if color model changes or
     * user inputs a color hex. For the latter it must be called
     * only after the accent colors are updated.
     */
    private fun updateSliders() {
        when (colorModel) {
            ColorModel.RGB -> { updateSliderProgressFromColor() }
            ColorModel.HSV -> {
                with (FloatArray(3)) {
                    Color.colorToHSV(color, this)
                    updateSliderProgressFromHSVorHSL(this)
                }
            }
            ColorModel.HSL -> {
                with (FloatArray(3)) {
                    ColorUtils.colorToHSL(color, this)
                    updateSliderProgressFromHSVorHSL(this)
                }
            }
        }
    }

    // For updating RGB slider progress
    private fun updateSliderProgressFromColor() {
        seekBarOne.setProgress(Color.red(color))
        seekBarTwo.setProgress(Color.green(color))
        seekBarThree.setProgress(Color.blue(color))
    }

    // For updating HSV / HSL slider progress
    private fun updateSliderProgressFromHSVorHSL(hsvOrHSL: FloatArray) {
        seekBarOne.setProgress(hsvOrHSL[0].toInt())
        seekBarTwo.setProgress((hsvOrHSL[1] * 100).toInt())
        seekBarThree.setProgress((hsvOrHSL[2] * 100).toInt())
    }

    // For updating the slider GradientDrawable's based on ColorModel
    private fun updateSliderGradients(colorModelChanged: Boolean) {
        if (colorModel == ColorModel.RGB) {
            if (colorModelChanged) {
                updateRGBGradient(seekBarOne.getProgressDrawable(), Color.RED)
                updateRGBGradient(seekBarTwo.getProgressDrawable(), Color.GREEN)
                updateRGBGradient(seekBarThree.getProgressDrawable(), Color.BLUE)
            }
        } else {
            if (colorModelChanged) {
                updateHueGradient()
            }
            updateSaturationGradient()
            if (colorModel == ColorModel.HSV) {
                updateValueGradient()
            } else {
                updateLuminanceGradient()
            }
        }
    }

    private fun updateLuminanceGradient() {
        val drawable = seekBarThree.getProgressDrawable() as GradientDrawable
        drawable.setColors(intArrayOf(
            Color.BLACK,
            Utils.HSLToColor(seekBarOne.progress.toFloat(), seekBarTwo.progress / 100f, 0.5f),
            Color.WHITE,
        ))
    }

    private fun updateValueGradient() {
        val drawable = seekBarThree.getProgressDrawable() as GradientDrawable
        drawable.setColors(intArrayOf(
            Color.BLACK,
            Utils.HSVToColor(seekBarOne.progress.toFloat(), seekBarTwo.progress / 100f, 1f),
        ))
    }

    private fun updateSaturationGradient() {
        val drawable = seekBarTwo.getProgressDrawable() as GradientDrawable
        var colors = intArrayOf(Color.WHITE, 0)
        if (colorModel == ColorModel.HSV) {
            colors[1] = Utils.HSVToColor(seekBarOne.progress.toFloat(), 1f,
                seekBarThree.progress / 100f)
        } else {
            colors[1] = Utils.HSLToColor(seekBarOne.progress.toFloat(), 1f,
                seekBarThree.progress / 100f)
        }
        drawable.setColors(colors)
    }

    private fun updateHueGradient() {
        val drawable = seekBarOne.getProgressDrawable() as GradientDrawable
        drawable.setColors(hueGradientColors)
    }

    private fun updateRGBGradient(progressDrawable: Drawable, color: Int) {
        val drawable = progressDrawable as GradientDrawable
        drawable.setColors(intArrayOf(Color.BLACK, color))
    }

    // inputFromUser should be set to true when user has entered a hex color
    private fun previewColor(inputFromUser: Boolean) {
        colorPreview.setBackgroundTintList(ColorStateList.valueOf(color))
        if (ColorUtils.calculateLuminance(color) > 0.5) {
            colorInput.setTextColor(Color.BLACK)
        } else {
            colorInput.setTextColor(Color.WHITE)
        }
        textInputChangedInternal = true
        if (!inputFromUser) {
            colorInput.setText(Utils.colorToHex(color))
        }
    }

    private fun updateSliderMax() {
        val isRGB = colorModel == ColorModel.RGB
        seekBarOne.setMax(if (isRGB) 255 else 360)
        seekBarTwo.setMax(if (isRGB) 255 else 100)
        seekBarThree.setMax(if (isRGB) 255 else 100)
    }
}

enum class ColorModel {
    RGB,
    HSL,
    HSV
}