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

import static android.provider.Settings.System.GAMINGMODE_ENABLED;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;

public class GamingModeFragment extends SettingsPreferenceFragment {

    private static final String TAG = "GamingMode";
    private static final String masterSwitchKey = "gamingmode_switch_preference";
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
                                mEditor.putBoolean(getCustomKey(preference), ((SwitchPreference) preference).isChecked()).apply();
                                if (preference.getKey().equals(masterSwitchKey)) {
                                    Settings.System.putInt(mContext.getContentResolver(), GAMINGMODE_ENABLED,
                                     ((SwitchPreference) preference).isChecked() == true ? 1 : 0);
                                    disableViewIfNeeded();
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
        else {
            Log.d(TAG, "PreferenceGroup is null");
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
}
