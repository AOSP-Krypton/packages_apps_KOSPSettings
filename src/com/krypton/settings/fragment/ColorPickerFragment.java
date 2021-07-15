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

import static android.view.HapticFeedbackConstants.KEYBOARD_PRESS;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.fragment.app.DialogFragment;

import com.android.settings.R;
import com.krypton.settings.Utils;

public class ColorPickerFragment extends DialogFragment implements OnSeekBarChangeListener {
    private final String mSettingKey, mSettingNamespace;
    private final int mSettingDefault;
    private Context mContext;
    private ImageView mColorPreview;
    private TextView mHexColor;
    private SeekBar mRedSeekBar, mGreenSeekBar, mBlueSeekBar;
    private Button mSelectButton;

    public ColorPickerFragment(String key, String ns, int def) {
        super(R.layout.color_picker_layout);
        mSettingKey = key;
        mSettingNamespace = ns;
        mSettingDefault = def;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        final Dialog dialog = requireDialog();
        dialog.setTitle(R.string.color_picker_title);
        mColorPreview = view.findViewById(R.id.color_preview);
        mHexColor = view.findViewById(R.id.hex_color);
        mRedSeekBar = view.findViewById(R.id.red_seekBar);
        mRedSeekBar.setOnSeekBarChangeListener(this);
        mGreenSeekBar = view.findViewById(R.id.green_seekBar);
        mGreenSeekBar.setOnSeekBarChangeListener(this);
        mBlueSeekBar = view.findViewById(R.id.blue_seekBar);
        mBlueSeekBar.setOnSeekBarChangeListener(this);
        mSelectButton = view.findViewById(R.id.select_button);
        mSelectButton.setOnClickListener(v -> {
            v.performHapticFeedback(KEYBOARD_PRESS);
            Utils.applySetting(mContext, mSettingNamespace, mSettingKey, getColor());
            dialog.dismiss();
        });
        final int color = Utils.getSettingInt(mContext, mSettingNamespace,
            mSettingKey, mSettingDefault);
        updateColorPreviewAndHex(color);
        mRedSeekBar.setProgress(Color.red(color));
        mGreenSeekBar.setProgress(Color.green(color));
        mBlueSeekBar.setProgress(Color.blue(color));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        updateColorPreviewAndHex(getColor());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    private void updateColorPreviewAndHex(int color) {
        mColorPreview.setColorFilter(color);
        mHexColor.setText(Integer.toHexString(color));
    }

    private int getColor() {
        return Color.rgb(mRedSeekBar.getProgress(),
            mGreenSeekBar.getProgress(), mBlueSeekBar.getProgress());
    }
}
