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
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.EditTextPreference;

import com.android.settings.R;
import com.krypton.settings.Utils;

public class SettingEditTextPreference extends EditTextPreference
        implements OnPreferenceChangeListener {

    private final Context mContext;
    private final String mSettingKey, mSettingNamespace;
    private final int mSettingDefault;

    public SettingEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        final TypedArray typedArray = mContext.getResources().obtainAttributes(attrs, R.styleable.SettingPreferenceBaseAttrs);
        mSettingKey = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingKey);
        mSettingNamespace = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingNamespace);
        mSettingDefault = typedArray.getInteger(R.styleable.SettingPreferenceBaseAttrs_settingDefault, 0);
        typedArray.recycle();
        setText(String.valueOf(Utils.getSettingInt(mContext,
            mSettingNamespace, mSettingKey, mSettingDefault)));
        setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            return Utils.applySetting(mContext, mSettingNamespace,
                mSettingKey, Integer.parseInt((String) newValue));
        } catch(NumberFormatException e) {
            Toast.makeText(mContext, R.string.invalid_integer_value, Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
