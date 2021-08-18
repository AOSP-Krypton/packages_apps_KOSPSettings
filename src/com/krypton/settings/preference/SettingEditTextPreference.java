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
import android.text.InputType;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.EditTextPreference;
import androidx.preference.EditTextPreference.OnBindEditTextListener;

import com.android.settings.R;
import com.krypton.settings.Utils;

public class SettingEditTextPreference extends EditTextPreference
        implements OnPreferenceChangeListener, OnBindEditTextListener {
    private static final String INPUT_INT = "integer";
    private static final String INPUT_STR = "string";
    private final Context mContext;
    private final Handler mHandler;
    private final String mSettingKey, mSettingNamespace,
        mSettingDependencyKey, mSettingDependencyNS;
    private final int mSettingDefault, mSettingDependencyValue;
    private String mInputType;
    private boolean mDependencyMet = true;
    private ContentObserver mSettingsObserver;

    public SettingEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        final Resources res = mContext.getResources();
        TypedArray typedArray = res.obtainAttributes(attrs, R.styleable.SettingEditTextPreference);
        mInputType = typedArray.getString(R.styleable.SettingEditTextPreference_inputType);
        typedArray.recycle();
        typedArray = res.obtainAttributes(attrs, R.styleable.SettingPreferenceBaseAttrs);
        mSettingKey = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingKey);
        mSettingNamespace = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingNamespace);
        mSettingDependencyKey = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingDependencyKey);
        mSettingDependencyNS = typedArray.getString(R.styleable.SettingPreferenceBaseAttrs_settingDependencyNS);
        mSettingDefault = typedArray.getInteger(R.styleable.SettingPreferenceBaseAttrs_settingDefault, 0);
        mSettingDependencyValue = typedArray.getInteger(R.styleable.SettingPreferenceBaseAttrs_settingDependencyValue, 1);
        typedArray.recycle();
        if (Utils.isEmpty(mInputType)) {
            mInputType = INPUT_INT;
        }
        setOnBindEditTextListener(this);
        setText(getText());
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
    public String getText() {
        switch (mInputType) {
            case INPUT_INT:
                return String.valueOf(Utils.getSettingInt(mContext,
                    mSettingNamespace, mSettingKey, mSettingDefault));
            case INPUT_STR:
                return Utils.getStringFromSettings(mContext,
                    mSettingNamespace, mSettingKey);
        }
        return null;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (mInputType) {
            case INPUT_INT:
                Utils.applySetting(mContext, mSettingNamespace,
                    mSettingKey, Integer.parseInt((String) newValue));
                return true;
            case INPUT_STR:
                Utils.putStringInSettings(mContext, mSettingNamespace,
                    mSettingKey, (String) newValue);
                return true;
        }
        return false;
    }

    @Override
    public void onBindEditText(EditText editText) {
        switch (mInputType) {
            case INPUT_INT:
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            case INPUT_STR:
                editText.setInputType(InputType.TYPE_CLASS_TEXT);
        }
    }

    private void updateIfDependencyMet() {
        mDependencyMet = Utils.getSettingInt(mContext, mSettingDependencyNS,
            mSettingDependencyKey) == mSettingDependencyValue;
        notifyChanged();
    }
}
