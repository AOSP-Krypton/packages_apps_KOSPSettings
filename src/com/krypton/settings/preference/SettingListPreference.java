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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.ListPreference;

import com.android.settings.R;
import com.krypton.settings.Utils;

public class SettingListPreference extends ListPreference
        implements OnPreferenceChangeListener {

    private final Context mContext;
    private final Handler mHandler;
    private final String mSettingKey, mSettingNamespace,
        mSettingDependencyKey, mSettingDependencyNS;
    private final int mSettingDefault, mSettingDependencyValue;
    private final int[] mSettingValues;
    private ContentObserver mSettingsObserver;
    private boolean mDependencyMet = true;

    public SettingListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        final Resources res = mContext.getResources();
        TypedArray typedArray = res.obtainAttributes(attrs, R.styleable.SettingListPreference);
        int arrayResourceId = typedArray.getInteger(R.styleable.SettingListPreference_settingValues, -1);
        typedArray.recycle();
        typedArray = res.obtainAttributes(attrs, R.styleable.SettingPreferenceBaseAttrs);
        mSettingKey = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingKey);
        mSettingNamespace = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingNamespace);
        mSettingDependencyKey = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingDependencyKey);
        mSettingDependencyNS = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingDependencyNS);
        mSettingDefault = typedArray.getInteger(R.styleable.SettingPreferenceBaseAttrs_settingDefault, 0);
        mSettingDependencyValue = typedArray.getInteger(R.styleable.SettingPreferenceBaseAttrs_settingDependencyValue, 1);
        typedArray.recycle();
        if (arrayResourceId != -1) {
            mSettingValues = res.getIntArray(arrayResourceId);
        } else {
            final CharSequence[] arr = getEntryValues();
            mSettingValues = new int[arr.length];
            for (int i = 0; i < arr.length; i++) {
                mSettingValues[i] = Integer.parseInt(arr[i].toString());
            }
        }
        setDefaultValue(String.valueOf(Utils.getSettingInt(mContext,
            mSettingNamespace, mSettingKey, mSettingDefault)));
        setOnPreferenceChangeListener(this);
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
        return super.isEnabled() && mDependencyMet;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return Utils.applySetting(mContext, mSettingNamespace,
            mSettingKey, mSettingValues[findIndexOfValue((String) newValue)]);
    }

    private void updateIfDependencyMet() {
        mDependencyMet = Utils.getSettingInt(mContext, mSettingDependencyNS,
            mSettingDependencyKey) == mSettingDependencyValue;
        notifyChanged();
    }
}
