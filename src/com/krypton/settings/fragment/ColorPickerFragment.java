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
package com.krypton.settings.fragment;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.view.HapticFeedbackConstants.KEYBOARD_PRESS;
import static androidx.fragment.app.DialogFragment.STYLE_NORMAL;

import android.annotation.ColorInt;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.android.settings.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.krypton.settings.Utils;

public class ColorPickerFragment extends BottomSheetDialogFragment
        implements OnCheckedChangeListener, OnSeekBarChangeListener {
    private static final int[] mHueGradientColors = new int[] {
        Utils.HSVToColor(0, 1f, 1f),
        Utils.HSVToColor(60, 1f, 1f),
        Utils.HSVToColor(120, 1f, 1f),
        Utils.HSVToColor(180, 1f, 1f),
        Utils.HSVToColor(240, 1f, 1f),
        Utils.HSVToColor(300, 1f, 1f),
        Utils.HSVToColor(360, 1f, 1f)
    };
    private final String mSettingKey, mSettingNamespace;
    private final int mSettingDefault;
    private Context mContext;
    private View mColorPreview;
    private EditText mColorInput;
    private SeekBar mSeekBarOne, mSeekBarTwo, mSeekBarThree;
    private ColorModel mColorModel;
    private boolean mTextInputChangedInternal; // Internal variable to prevent loops with TextWatcher

    @ColorInt
    private int mColor;

    public ColorPickerFragment(String key, String ns, int def) {
        super();
        mSettingKey = key;
        mSettingNamespace = ns;
        mSettingDefault = def;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        setStyle(STYLE_NORMAL, R.style.AccentDialogStyle);
        mContext = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.color_picker_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mColorPreview = view.findViewById(R.id.color_preview);
        mColorInput = view.findViewById(R.id.color_input);

        mColorInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not implemented
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not implemented
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mTextInputChangedInternal) {
                    // Reset it here
                    mTextInputChangedInternal = false;
                } else if (s.length() == 7) {
                    mColor = Color.parseColor(s.toString());
                    updateSliders();
                    updateSliderGradients(false);
                    previewColor(true);
                }
            }
        });

        mColorInput.setFilters(new InputFilter[] {
            new InputFilter.LengthFilter(7),
            (source, start, end, dest, dstart, dend) -> {
                // Make sure # is persistent
                if (dest.length() != 0 && dstart == 0) {
                    return "#";
                }
                // Deletion
                if (start == 0 && end == 0) {
                    return null;
                }
                return Utils.HEX_PATTERN.matcher(((end - start) == 7) ?
                    source.subSequence(1, 7) : source).matches() ? null : "";
            }
        });

        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
            v.performHapticFeedback(KEYBOARD_PRESS);
            requireDialog().dismiss();
        });

        Button confirmButton = view.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(v -> {
            v.performHapticFeedback(KEYBOARD_PRESS);
            Utils.applySetting(mContext, mSettingNamespace, mSettingKey, mColor);
            requireDialog().dismiss();
        });

        mSeekBarOne = view.findViewById(R.id.seekBar1);
        mSeekBarTwo = view.findViewById(R.id.seekBar2);
        mSeekBarThree = view.findViewById(R.id.seekBar3);

        /**
         * Set the drawables as mutable so that they
         * do not share a constant state or else all
         * three slider gradients will look alike
         */
        mSeekBarOne.getProgressDrawable().mutate();
        mSeekBarTwo.getProgressDrawable().mutate();
        mSeekBarThree.getProgressDrawable().mutate();

        // Register progress change listeners
        mSeekBarTwo.setOnSeekBarChangeListener(this);
        mSeekBarOne.setOnSeekBarChangeListener(this);
        mSeekBarThree.setOnSeekBarChangeListener(this);

        // Register listener for color model change
        RadioGroup colorModelGroup = view.findViewById(R.id.color_model_group);
        colorModelGroup.setOnCheckedChangeListener(this);

        mColorModel = ColorModel.RGB; // Default to RGB model

        mColor = Math.min(Utils.getSettingInt(mContext, mSettingNamespace,
            mSettingKey, mSettingDefault), -1);

        // Update sliders and preview
        updateSliderMax();
        updateSliders();
        updateSliderGradients(true);
        previewColor(false);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            return;
        }
        switch (mColorModel) {
            case RGB:
                mColor = Color.rgb(mSeekBarOne.getProgress(), mSeekBarTwo.getProgress(),
                    mSeekBarThree.getProgress());
                break;
            case HSV:
                mColor = Utils.HSVToColor(mSeekBarOne.getProgress(), mSeekBarTwo.getProgress() / 100f,
                    mSeekBarThree.getProgress() / 100f);
                updateSliderGradients(false);
                break;
            case HSL:
                mColor = Utils.HSLToColor(mSeekBarOne.getProgress(), mSeekBarTwo.getProgress() / 100f,
                    mSeekBarThree.getProgress() / 100f);
                updateSliderGradients(false);
        }
        previewColor(false);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Not implemented
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Not implemented
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.rgb_button) {
            mColorModel = ColorModel.RGB;
        } else if (checkedId == R.id.hsv_button) {
            mColorModel = ColorModel.HSV;
        } else if (checkedId == R.id.hsl_button) {
            mColorModel = ColorModel.HSL;
        }
        updateSliderMax();
        updateSliders();
        updateSliderGradients(true);
    }

    /**
     * Used to update sliders if color model changes or
     * user inputs a color hex. For the latter it must be called
     * only after the accent colors are updated.
     */
    private void updateSliders() {
        switch (mColorModel) {
            case RGB:
                updateSliderProgressFromColor();
                break;
            case HSV:
                float[] hsv = new float[3];
                Color.colorToHSV(mColor, hsv);
                updateSliderProgressFromHSVorHSL(hsv);
                break;
            case HSL:
                float[] hsl = new float[3];
                ColorUtils.colorToHSL(mColor, hsl);
                updateSliderProgressFromHSVorHSL(hsl);
        }
    }

    // For updating RGB slider progress
    private void updateSliderProgressFromColor() {
        mSeekBarOne.setProgress(Color.red(mColor));
        mSeekBarTwo.setProgress(Color.green(mColor));
        mSeekBarThree.setProgress(Color.blue(mColor));
    }

    // For updating HSV / HSL slider progress
    private void updateSliderProgressFromHSVorHSL(float[] hsvOrHSL) {
        mSeekBarOne.setProgress((int) hsvOrHSL[0]);
        mSeekBarTwo.setProgress((int) (hsvOrHSL[1] * 100));
        mSeekBarThree.setProgress((int) (hsvOrHSL[2] * 100));
    }

    // For updating the slider GradientDrawable's based on ColorModel
    private void updateSliderGradients(boolean colorModelChanged) {
        if (mColorModel == ColorModel.RGB) {
            if (colorModelChanged) {
                updateRGBGradient(mSeekBarOne.getProgressDrawable(), Color.RED);
                updateRGBGradient(mSeekBarTwo.getProgressDrawable(), Color.GREEN);
                updateRGBGradient(mSeekBarThree.getProgressDrawable(), Color.BLUE);
            }
        } else {
            if (colorModelChanged) {
                updateHueGradient();
            }
            updateSaturationGradient();
            if (mColorModel == ColorModel.HSV) {
                updateValueGradient();
            } else {
                updateLuminanceGradient();
            }
        }
    }

    private void updateLuminanceGradient() {
        GradientDrawable drawable = (GradientDrawable) mSeekBarThree.getProgressDrawable();
        drawable.setColors(new int[] { Color.BLACK,
                Utils.HSLToColor(mSeekBarOne.getProgress(), mSeekBarTwo.getProgress() / 100f, 0.5f),
                Color.WHITE
        });
    }

    private void updateValueGradient() {
        GradientDrawable drawable = (GradientDrawable) mSeekBarThree.getProgressDrawable();
        drawable.setColors(new int[] { Color.BLACK,
                Utils.HSVToColor(mSeekBarOne.getProgress(), mSeekBarTwo.getProgress() / 100f, 1f)
        });
    }

    private void updateSaturationGradient() {
        GradientDrawable drawable = (GradientDrawable) mSeekBarTwo.getProgressDrawable();
        int[] colors = new int[] { Color.WHITE, 0 };
        if (mColorModel == ColorModel.HSV) {
            colors[1] = Utils.HSVToColor(mSeekBarOne.getProgress(), 1f,
                mSeekBarThree.getProgress() / 100f);
        } else {
            colors[1] = Utils.HSLToColor(mSeekBarOne.getProgress(), 1f,
                mSeekBarThree.getProgress() / 100f);
        }
        drawable.setColors(colors);
    }

    private void updateHueGradient() {
        GradientDrawable drawable = (GradientDrawable) mSeekBarOne.getProgressDrawable();
        drawable.setColors(mHueGradientColors);
    }

    private void updateRGBGradient(Drawable progressDrawable, int color) {
        GradientDrawable drawable = (GradientDrawable) progressDrawable;
        drawable.setColors(new int[] { Color.BLACK, color });
    }

    // inputFromUser should be set to true when user has entered a hex color
    private void previewColor(boolean inputFromUser) {
        mColorPreview.setBackgroundTintList(ColorStateList.valueOf(mColor));
        if (ColorUtils.calculateLuminance(mColor) > 0.5d) {
            mColorInput.setTextColor(Color.BLACK);
        } else {
            mColorInput.setTextColor(Color.WHITE);
        }
        mTextInputChangedInternal = true;
        if (!inputFromUser) {
            mColorInput.setText(colorToHex(mColor));
        }
    }

    private void updateSliderMax() {
        boolean isRGB = mColorModel == ColorModel.RGB;
        mSeekBarOne.setMax(isRGB ? 255 : 360);
        mSeekBarTwo.setMax(isRGB ? 255 : 100);
        mSeekBarThree.setMax(isRGB ? 255 : 100);
    }

    private static String colorToHex(int color) {
        return "#" + Integer.toHexString(color).toUpperCase();
    }
}
