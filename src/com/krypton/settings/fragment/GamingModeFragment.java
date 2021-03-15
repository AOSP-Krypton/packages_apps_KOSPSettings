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

import static android.provider.Settings.System.GAMINGMODE_LOCK_BRIGHTNESS;
import static android.provider.Settings.System.GAMINGMODE_RESTORE_BRIGHTNESS;
import static android.provider.Settings.System.GAMINGMODE_ENABLED;
import static android.provider.Settings.System.GAMINGMODE_TOAST;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;

public class GamingModeFragment extends SettingsPreferenceFragment {

    private static final String masterSwitchKey = "gamingmode_switch_preference";
    private static final String brightnessLockKey = "brightness_lock_preference";
    private static final String brightnessRestoreKey = "brightness_restore_preference";
    private static final String showToastKey = "show_toast_preference";
    private SharedPreferences sharedPrefs;
    private Editor mEditor;
    private Context mContext;
    private SwitchPreference masterSwitch;
    private ArrayList<Preference> mList;
    private String key;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.gamingmode_settings);
        mContext = getContext();
        mList = new ArrayList<>();
        key = mContext.getPackageName();
        sharedPrefs = mContext.getSharedPreferences(key, Context.MODE_PRIVATE);
        mEditor = sharedPrefs.edit();
        setPreferenceListeners(getPreferenceScreen());
        updateSwitch(masterSwitch);
        disableViewIfNeeded();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.KRYPTON;
    }

    private void setPreferenceListeners(PreferenceGroup preferenceGroup) {
        if (preferenceGroup != null) {
            for (int i=0; i < preferenceGroup.getPreferenceCount(); i++) {
                Preference preference = preferenceGroup.getPreference(i);
                if (preference instanceof SwitchPreference) {
                    if (preference.getKey() != null) {
                        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                boolean checked = ((SwitchPreference) preference).isChecked();
                                mEditor.putBoolean(getCustomKey(preference), checked).apply();
                                if (preference.getKey().equals(masterSwitchKey)) {
                                    putInt(GAMINGMODE_ENABLED, checked);
                                    disableViewIfNeeded();
                                }
                                else if (preference.getKey().equals(brightnessLockKey)) {
                                    putInt(GAMINGMODE_LOCK_BRIGHTNESS, checked);
                                }
                                else if (preference.getKey().equals(brightnessRestoreKey)) {
                                    putInt(GAMINGMODE_RESTORE_BRIGHTNESS, checked);
                                }
                                else if (preference.getKey().equals(showToastKey)) {
                                    putInt(GAMINGMODE_TOAST, checked);
                                }
                                return true;
                            }
                        });
                        if (preference.getKey().equals(masterSwitchKey)) {
                            masterSwitch = (SwitchPreference) preference;
                        }
                        else {
                            mList.add(preference);
                        }
                    }
                }
                else if (preference instanceof PreferenceGroup) {
                    setPreferenceListeners((PreferenceGroup) preference);
                }
                else {
                    mList.add(preference);
                }
            }
        }
    }

    private void disableViewIfNeeded() {
        for (Preference preference: mList) {
            if (preference instanceof SwitchPreference) {
                updateSwitch(preference);
            }
            preference.setEnabled(getBoolean(masterSwitch));
        }
    }

    private void updateSwitch(Preference preference) {
        ((SwitchPreference) preference).setChecked(getBoolean(preference));
    }

    private boolean getBoolean(Preference preference) {
        return sharedPrefs.getBoolean(getCustomKey(preference),false);
    }

    private String getCustomKey(Preference preference) {
        return key + "." + preference.getKey();
    }

    private void putInt(String key, boolean value) {
        Settings.System.putInt(mContext.getContentResolver(), key, value ? 1 : 0);
    }
}
