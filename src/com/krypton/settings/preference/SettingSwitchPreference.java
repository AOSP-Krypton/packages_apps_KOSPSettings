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

import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.krypton.settings.Utils;

public class SettingSwitchPreference extends SwitchPreference {
    private final Context mContext;
    private final Handler mHandler;
    private final String mSettingKey, mSettingNamespace,
        mSettingDependencyKey, mSettingDependencyNS;
    private final int mSettingDependencyValue;
    private final boolean mDisableIfDependencyMet;
    private ContentObserver mSettingsObserver;
    private int mSettingDefault;
    private boolean mDependencyMet = true;

    public SettingSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        final TypedArray typedArray = mContext.getResources().obtainAttributes(attrs, R.styleable.SettingPreferenceBaseAttrs);
        mSettingKey = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingKey);
        mSettingNamespace = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingNamespace);
        mSettingDependencyKey = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingDependencyKey);
        mSettingDependencyNS = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingDependencyNS);
        mSettingDependencyValue = typedArray.getInteger(R.styleable.SettingPreferenceBaseAttrs_settingDependencyValue, 1);
        mDisableIfDependencyMet = typedArray.getBoolean(R.styleable.SettingPreferenceBaseAttrs_disableIfDependencyMet, false);
        parseDefaultValue(typedArray, R.styleable.SettingPreferenceBaseAttrs_settingDefault);
        typedArray.recycle();
        super.setChecked(isChecked());
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
    public void onDetached() {
        super.onDetached();
        if (mSettingsObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
        }
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && (mDisableIfDependencyMet ?
            !mDependencyMet : mDependencyMet);
    }

    @Override
    public boolean isChecked() {
        return Utils.getSettingBoolean(mContext,
            mSettingNamespace, mSettingKey, mSettingDefault);
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        Utils.applySetting(mContext, mSettingNamespace, mSettingKey, checked);
    }

    public void setSettingDefault(int def) {
        mSettingDefault = def;
    }

    private void parseDefaultValue(TypedArray a, int index) {
        if (!a.hasValue(index)) {
            return;
        }
        mSettingDefault = a.getBoolean(index, false) ? 1 : 0;
    }

    private void updateIfDependencyMet() {
        mDependencyMet = Utils.getSettingInt(mContext, mSettingDependencyNS,
            mSettingDependencyKey) == mSettingDependencyValue;
        notifyChanged();
        notifyDependencyChange(true);
    }
}
