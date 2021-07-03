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

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.ListPreference;

import com.android.settings.R;
import com.krypton.settings.Utils;

public class SettingListPreference extends ListPreference
        implements OnPreferenceChangeListener {

    private final Context mContext;
    private final String mSettingKey, mSettingNamespace;
    private final int[] mSettingValues;

    public SettingListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        final TypedArray typedArray = mContext.getTheme().obtainStyledAttributes(attrs,
            R.styleable.SettingListPreference, 0, 0);
        mSettingKey = typedArray.getString(R.styleable.SettingListPreference_settingKey);
        mSettingNamespace = typedArray.getString(R.styleable.SettingListPreference_settingNamespace);
        int arrayResourceId = typedArray.getInteger(R.styleable.SettingListPreference_settingValues, -1);
        typedArray.recycle();
        if (arrayResourceId != -1) {
            mSettingValues = mContext.getResources().getIntArray(arrayResourceId);
        } else {
            final CharSequence[] arr = getEntryValues();
            mSettingValues = new int[arr.length];
            for (int i = 0; i < arr.length; i++) {
                mSettingValues[i] = Integer.parseInt(arr[i].toString());
            }
        }
        setDefaultValue(String.valueOf(Utils.getSettingInt(mContext, mSettingNamespace, mSettingKey)));
        setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return Utils.applySetting(mContext, mSettingNamespace,
            mSettingKey, mSettingValues[findIndexOfValue((String) newValue)]);
    }
}
