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
import static android.content.res.Configuration.UI_MODE_NIGHT_YES;
import static android.provider.Settings.Secure.ACCENT_DARK;
import static android.provider.Settings.Secure.ACCENT_LIGHT;
import static androidx.fragment.app.DialogFragment.STYLE_NORMAL;

import android.annotation.ColorInt;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AccentUtils;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.constraintlayout.widget.Group;

import com.android.settings.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.krypton.settings.Utils;

public class AccentPickerFragment extends BottomSheetDialogFragment
        implements OnCheckedChangeListener, OnSeekBarChangeListener,
            OnClickListener {
    /**
     * The luminance value which allows us to determine whether
     * a light color is too light or a dark color is too dark.
     * Must be in the range of [0, 1]
     */
    private static final double mTolerance = 0.5d;
    private static final int[] mHueGradientColors = new int[] {
            Utils.HSVToColor(0, 1f, 1f),
            Utils.HSVToColor(60, 1f, 1f),
            Utils.HSVToColor(120, 1f, 1f),
            Utils.HSVToColor(180, 1f, 1f),
            Utils.HSVToColor(240, 1f, 1f),
            Utils.HSVToColor(300, 1f, 1f),
            Utils.HSVToColor(360, 1f, 1f)
    };

    private Context mContext;
    private Drawable mPreviewTextBackground;
    private View mLightAccentPreview, mDarkAccentPreview;
    private EditText mLightAccentInput, mDarkAccentInput;
    private TextView mLightTextView, mDarkTextView;
    private Group mLightAccentWarning, mDarkAccentWarning;
    private Switch mAutoModeSwitch;
    private SeekBar mSeekBarOne, mSeekBarTwo, mSeekBarThree;
    private ColorModel mColorModel;
    private int mPreviewMode; // 0 for light mode preview, 1 for dark mode preview
    private boolean mTextInputChangedInternal; // Internal variable to prevent loops with TextWatcher
    private boolean mIsDarkMode;

    @ColorInt
    private int mLightAccent, mDarkAccent;
    @ColorInt
    private int mSavedLightAccent, mSavedDarkAccent;

    public AccentPickerFragment() {
        super();
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
        return inflater.inflate(R.layout.accent_picker_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        final Resources res = getResources();
        mPreviewTextBackground = res.getDrawable(
            R.drawable.selected_preview_text_background, null);
        mLightTextView = view.findViewById(R.id.light_textView);
        mLightAccentPreview = view.findViewById(R.id.light_accent_preview);
        mLightAccentPreview.setOnClickListener(this);
        mLightAccentInput = view.findViewById(R.id.light_accent_input);
        mLightAccentWarning = view.findViewById(R.id.light_accent_warning_group);

        mDarkTextView = view.findViewById(R.id.dark_textView);
        mDarkAccentPreview = view.findViewById(R.id.dark_accent_preview);
        mDarkAccentPreview.setOnClickListener(this);
        mDarkAccentInput = view.findViewById(R.id.dark_accent_input);
        mDarkAccentWarning = view.findViewById(R.id.dark_accent_warning_group);

        final TextWatcher textWatcher = new TextWatcher() {
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
                    int color = Color.parseColor(s.toString());
                    if (mPreviewMode == 0) {
                        mLightAccent = color;
                    } else {
                        mDarkAccent = color;
                    }
                    updateSliders();
                    updateSliderGradients(false);
                    updatePreview(true);
                }
            }
        };
        mLightAccentInput.addTextChangedListener(textWatcher);
        mDarkAccentInput.addTextChangedListener(textWatcher);

        final InputFilter[] filters = new InputFilter[] {
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
        };
        mLightAccentInput.setFilters(filters);
        mDarkAccentInput.setFilters(filters);

        // Switch to automatically calculate and preview light and dark accent
        mAutoModeSwitch = view.findViewById(R.id.auto_mode_switch);
        mAutoModeSwitch.setOnClickListener(v -> updatePreview(false));

        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> requireDialog().dismiss());

        Button confirmButton = view.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(v -> {
            Utils.putStringInSettings(mContext, Utils.TYPE_SECURE, ACCENT_LIGHT,
                colorToHex(mLightAccent));
            Utils.putStringInSettings(mContext, Utils.TYPE_SECURE, ACCENT_DARK,
                colorToHex(mDarkAccent));
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

        /**
         * Get system theme mode and update mPreviewMode,
         * also get accent colors either from attrs or
         * from stored settings
         */
        mIsDarkMode = (res.getConfiguration().uiMode & UI_MODE_NIGHT_YES) != 0;
        mPreviewMode = mIsDarkMode ? 1 : 0;
        updateSelectedState();
        try {
            Resources androidRes = mContext.getPackageManager().getResourcesForApplication("android");
            int resId = androidRes.getIdentifier("accent_device_default_light", "color", "android");
            if (resId != 0) {
                mLightAccent = mSavedLightAccent = AccentUtils.getLightAccentColor(androidRes.getColor(resId, null));
            }
            resId = androidRes.getIdentifier("accent_device_default_dark", "color", "android");
            if (resId != 0) {
                mDarkAccent = mSavedDarkAccent = AccentUtils.getDarkAccentColor(androidRes.getColor(resId, null));
            }
        } catch(NameNotFoundException e) {
            // Do nothing
        }

        // Update sliders and preview
        updateSliderMax();
        updateSliders();
        updateSliderGradients(true);
        previewLightAccent(mLightAccent, false);
        previewDarkAccent(mDarkAccent, false);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            return;
        }
        int color = -1;
        switch (mColorModel) {
            case RGB:
                color = Color.rgb(mSeekBarOne.getProgress(), mSeekBarTwo.getProgress(),
                    mSeekBarThree.getProgress());
                break;
            case HSV:
                color = Utils.HSVToColor(mSeekBarOne.getProgress(), mSeekBarTwo.getProgress() / 100f,
                    mSeekBarThree.getProgress() / 100f);
                updateSliderGradients(false);
                break;
            case HSL:
                color = Utils.HSLToColor(mSeekBarOne.getProgress(), mSeekBarTwo.getProgress() / 100f,
                    mSeekBarThree.getProgress() / 100f);
                updateSliderGradients(false);
        }
        if (mPreviewMode == 0) {
            mLightAccent = color;
        } else {
            mDarkAccent = color;
        }
        updatePreview(false);
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

    @Override
    public void onClick(View v) {
        mPreviewMode = v == mLightAccentPreview ? 0 : 1;
        updateSelectedState();
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
                updateSliderProgressFromColor(mPreviewMode == 0 ? mLightAccent : mDarkAccent);
                break;
            case HSV:
                float[] hsv = new float[3];
                Color.colorToHSV(mPreviewMode == 0 ? mLightAccent : mDarkAccent, hsv);
                updateSliderProgressFromHSVorHSL(hsv);
                break;
            case HSL:
                float[] hsl = new float[3];
                ColorUtils.colorToHSL(mPreviewMode == 0 ? mLightAccent : mDarkAccent, hsl);
                updateSliderProgressFromHSVorHSL(hsl);
        }
    }

    private void updateSelectedState() {
        int checkedTextColor = mIsDarkMode ? Color.BLACK : Color.WHITE;
        int uncheckedTextColor = mIsDarkMode ? Color.WHITE : Color.BLACK;
        if (mPreviewMode == 0) {
            mLightTextView.setBackground(mPreviewTextBackground);
            mLightTextView.setTextColor(checkedTextColor);
            mDarkTextView.setBackground(null);
            mDarkTextView.setTextColor(uncheckedTextColor);
        } else {
            mLightTextView.setBackground(null);
            mLightTextView.setTextColor(uncheckedTextColor);
            mDarkTextView.setBackground(mPreviewTextBackground);
            mDarkTextView.setTextColor(checkedTextColor);
        }
    }

    // For updating RGB slider progress
    private void updateSliderProgressFromColor(@ColorInt int color) {
        mSeekBarOne.setProgress(Color.red(color));
        mSeekBarTwo.setProgress(Color.green(color));
        mSeekBarThree.setProgress(Color.blue(color));
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
    private void updatePreview(boolean inputFromUser) {
        if (mPreviewMode == 0) {
            previewLightAccent(mLightAccent, inputFromUser);
            if (mAutoModeSwitch.isChecked()) {
                previewDarkAccent(getAltAccent(mLightAccent), inputFromUser);
            }
        } else {
            previewDarkAccent(mDarkAccent, inputFromUser);
            if (mAutoModeSwitch.isChecked()) {
                previewLightAccent(getAltAccent(mDarkAccent), inputFromUser);
            }
        }
    }

    // inputFromUser should be set to true when user has entered a hex color
    private void previewLightAccent(int color, boolean inputFromUser) {
        mLightAccentPreview.setBackgroundTintList(ColorStateList.valueOf(color));
        mTextInputChangedInternal = true;
        if (!inputFromUser) {
            mLightAccentInput.setText("#" + colorToHex(color));
        }
        double luminance = ColorUtils.calculateLuminance(color);
        if (luminance < (1 - mTolerance)) {
            mLightAccentWarning.setVisibility(View.INVISIBLE);
            mLightAccentInput.setTextColor(Color.WHITE);
            mLightAccentInput.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        } else { // Light accent is too light
            mLightAccentWarning.setVisibility(View.VISIBLE);
            mLightAccentInput.setTextColor(Color.BLACK);
            mLightAccentInput.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
        }
    }

    private void previewDarkAccent(int color, boolean inputFromUser) {
        mDarkAccentPreview.setBackgroundTintList(ColorStateList.valueOf(color));
        mTextInputChangedInternal = true;
        if (!inputFromUser) {
            mDarkAccentInput.setText("#" + colorToHex(color));
        }
        double luminance = ColorUtils.calculateLuminance(color);
        if (luminance > mTolerance) {
            mDarkAccentWarning.setVisibility(View.INVISIBLE);
            mDarkAccentInput.setTextColor(Color.BLACK);
            mDarkAccentInput.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
        } else { // Dark accent is too dark
            mDarkAccentWarning.setVisibility(View.VISIBLE);
            mDarkAccentInput.setTextColor(Color.WHITE);
            mDarkAccentInput.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        }
    }

    private void updateSliderMax() {
        boolean isRGB = mColorModel == ColorModel.RGB;
        mSeekBarOne.setMax(isRGB ? 255 : 360);
        mSeekBarTwo.setMax(isRGB ? 255 : 100);
        mSeekBarThree.setMax(isRGB ? 255 : 100);
    }

    /**
     * Converts the given @ColorInt to HSL color model and
     * returns a @ColorInt with an inverted luminance value
     */
    private static int getAltAccent(int color) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);
        hsl[2] = 1 - hsl[2];
        return ColorUtils.HSLToColor(hsl);
    }

    private static String colorToHex(int color) {
        return Integer.toHexString(color).substring(2).toUpperCase(); // Skip the alpha bits
    }
}
