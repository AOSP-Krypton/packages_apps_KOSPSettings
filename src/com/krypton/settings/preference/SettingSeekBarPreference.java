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
 * limitations under the License
 */

package com.krypton.settings.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.krypton.settings.Utils;

public class SettingSeekBarPreference extends Preference
        implements OnSeekBarChangeListener {

    private final Context mContext;
    private final Handler mHandler;
    private final String mSettingKey, mSettingNamespace,
        mSettingDependencyKey, mSettingDependencyNS;
    private final int mSettingDefault, mSettingDependencyValue,
        mMaxValue, mMinValue;
    private ContentObserver mSettingsObserver;
    private SeekBar mSeekBar;
    private TextView mProgressValue;
    private boolean mDependencyMet = true;

    public SettingSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        TypedArray typedArray = mContext.getResources().obtainAttributes(attrs, R.styleable.SettingSeekBarPreference);
        mMinValue = typedArray.getInteger(R.styleable.SettingSeekBarPreference_min, 0);
        mMaxValue = typedArray.getInteger(R.styleable.SettingSeekBarPreference_max, mMinValue);
        typedArray.recycle();
        typedArray = mContext.getResources().obtainAttributes(attrs, R.styleable.SettingPreferenceBaseAttrs);
        mSettingKey = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingKey);
        mSettingNamespace = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingNamespace);
        mSettingDependencyKey = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingDependencyKey);
        mSettingDependencyNS = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingDependencyNS);
        mSettingDefault = typedArray.getInteger(R.styleable.SettingPreferenceBaseAttrs_settingDefault, mMinValue);
        mSettingDependencyValue = typedArray.getInteger(R.styleable.SettingPreferenceBaseAttrs_settingDependencyValue, 1);
        typedArray.recycle();
        setLayoutResource(R.layout.seekbar_preference_layout);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mProgressValue != null) {
            mProgressValue.setText(String.valueOf(progress));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Utils.applySetting(mContext, mSettingNamespace, mSettingKey, seekBar.getProgress());
    }

    @Override
    public void onAttached() {
        super.onAttached();
        if (!Utils.isEmpty(mSettingDependencyKey)) {
            updateIfDependencyMet();
            Uri uri = Utils.getUri(mSettingDependencyNS, mSettingDependencyKey);
            if (uri != null) {
                mSettingsObserver = new ContentObserver(mHandler) {
                    @Override
                    public void onChange(boolean selfChange, Uri uri) {
                        updateIfDependencyMet();
                    }
                };
                mContext.getContentResolver().registerContentObserver(uri, false, mSettingsObserver);
            }
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View itemView = holder.itemView;
        itemView.setOnClickListener(null);
        itemView.setFocusable(false);
        itemView.setClickable(false);
        mSeekBar = (SeekBar) holder.findViewById(R.id.seekBar);
        mProgressValue = (TextView) holder.findViewById(R.id.progress_value);
        if (mSeekBar != null) {
            mSeekBar.setOnSeekBarChangeListener(this);
            mSeekBar.setMax(mMaxValue);
            mSeekBar.setMin(mMinValue);
            mSeekBar.setProgress(Utils.getSettingInt(mContext, mSettingNamespace,
                mSettingKey, mSettingDefault));
        }
    }

    @Override
    public void onDetached() {
        super.onDetached();
        if (mSettingsObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
        }
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && mDependencyMet;
    }

    private void updateIfDependencyMet() {
        mDependencyMet = Utils.getSettingInt(mContext, mSettingDependencyNS,
            mSettingDependencyKey) == mSettingDependencyValue;
        notifyChanged();
    }
}
