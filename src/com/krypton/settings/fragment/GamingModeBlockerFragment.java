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

import static android.provider.Settings.System.GAMINGMODE_DISABLE_HEADSUP;
import static android.provider.Settings.System.GAMINGMODE_RINGERMODE;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.SettingsPreferenceFragment;

public class GamingModeBlockerFragment extends SettingsPreferenceFragment {
    private static final String KEY_HEADSUP = "disable_headsup_preference";
    private static final String KEY_VIBRATE = "set_ringermode_vibrate";
    private static final String KEY_SILENT = "set_ringermode_silent";
    private Context mContext;
    private ContentResolver mResolver;
    private SharedPreferences sharedPrefs;
    private Editor mEditor;
    private String key;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.gamingmode_blocker_settings);
        mContext = getContext();
        mResolver = mContext.getContentResolver();
        key = mContext.getPackageName();
        sharedPrefs = mContext.getSharedPreferences(key, Context.MODE_PRIVATE);
        mEditor = sharedPrefs.edit();
        setPreferenceListeners(getPreferenceScreen());
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
                        updateSwitch(preference);
                        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                mEditor.putBoolean(getCustomKey(preference), ((SwitchPreference) preference).isChecked()).apply();
                                updateStatus(preference);
                                return true;
                            }
                        });
                    }
                }
                else if (preference instanceof PreferenceGroup) {
                    setPreferenceListeners((PreferenceGroup) preference);
                }
            }
        }
    }

    private void updateStatus(Preference preference) {
        if (preference.getKey().equals(KEY_HEADSUP)) {
            Settings.System.putInt(mResolver, GAMINGMODE_DISABLE_HEADSUP, getBoolean(preference) == true ? 1 : 0);
        }
        else if (preference.getKey().equals(KEY_VIBRATE)) {
            if (getBoolean(preference)) {
                Settings.System.putInt(mResolver, GAMINGMODE_RINGERMODE, 1);
            }
            else {
                Settings.System.putInt(mResolver, GAMINGMODE_RINGERMODE, 0);
            }
        }
        else if (preference.getKey().equals(KEY_SILENT)) {
            if (getBoolean(preference)) {
                Settings.System.putInt(mResolver, GAMINGMODE_RINGERMODE, 2);
                ((SwitchPreference) findPreference(KEY_VIBRATE)).setEnabled(false);
            }
            else {
                Settings.System.putInt(mResolver, GAMINGMODE_RINGERMODE, 0);
                ((SwitchPreference) findPreference(KEY_VIBRATE)).setEnabled(true);
            }
        }
    }

    private void updateSwitch(Preference preference) {
        ((SwitchPreference) preference).setChecked(getBoolean(preference));
        if (preference.getKey().equals(KEY_SILENT)) {
            ((SwitchPreference) findPreference(KEY_VIBRATE)).setEnabled(!getBoolean(preference));
        }
    }

    private boolean getBoolean(Preference preference) {
        return sharedPrefs.getBoolean(getCustomKey(preference), false);
    }

    private String getCustomKey(Preference preference) {
        return key + "." + preference.getKey();
    }

}
