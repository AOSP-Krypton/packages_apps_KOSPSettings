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

import static android.provider.Settings.System.GAMINGMODE_APPS;

import android.os.Bundle;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.provider.Settings;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;
import java.util.ArrayList;

public class GamingModeAppsFragment extends SettingsPreferenceFragment {
    private static String TAG = "GamingModeAppsFragment";
    private Context mContext;
    private ContentResolver mResolver;
    private ArrayList<PackageInfo> userApps;
    private ArrayList<CheckBoxPreference> checkBoxes;
    private PackageManager pm;
    private PreferenceScreen mScreen;
    private SharedPreferences sharedPrefs;
    private Editor mEditor;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.gamingmode_apps_screen);
        mContext = getContext();
        mResolver = mContext.getContentResolver();
        mScreen = getPreferenceScreen();
        sharedPrefs = mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_PRIVATE);
        mEditor = sharedPrefs.edit();
        pm = mContext.getPackageManager();
        userApps = new ArrayList<>();
        checkBoxes = new ArrayList<>();
        fetchAppsList();
        setView();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.KRYPTON;
    }

    private void fetchAppsList() {
        for (PackageInfo packageInfo: pm.getInstalledPackages(0)) {
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                userApps.add(packageInfo);
            }
        }
    }

    private void setView() {
        Preference resetButton = new Preference(mContext);
    	resetButton.setTitle("Reset");
    	resetButton.setKey(mContext.getPackageName() + ".reset_button");
    	resetButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
    		@Override
    		public boolean onPreferenceClick(Preference preference) {
    			resetPrefs();
    			return true;
    		}
    	});
    	mScreen.addPreference(resetButton);
        for (PackageInfo packageInfo: userApps) {
            CheckBoxPreference checkBox = new CheckBoxPreference(mContext);
            checkBox.setIcon(packageInfo.applicationInfo.loadIcon(pm));
            checkBox.setTitle(packageInfo.applicationInfo.loadLabel(pm));
            checkBox.setKey(packageInfo.packageName);
            checkBox.setChecked(sharedPrefs.getBoolean(checkBox.getKey(), false));
            checkBox.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mEditor.putBoolean(preference.getKey(), ((CheckBoxPreference) preference).isChecked()).apply();
                    updateAppPrefs((CheckBoxPreference) preference);
                    return true;
                }
            });
            checkBoxes.add(checkBox);
            mScreen.addPreference(checkBox);
        }
    }

    private void updateAppPrefs(CheckBoxPreference preference) {
        String prefPackageName = preference.getKey();
        String appsList = Settings.System.getString(mResolver, GAMINGMODE_APPS);
        if (appsList != null) {
            if (preference.isChecked()) {
                if (!appsList.contains(prefPackageName)) {
                	appsList += prefPackageName + " ";
                }
            }
            else {
            	appsList = appsList.replace(prefPackageName + " ", "");
            }
        }
        else {
            appsList += prefPackageName + " ";
        }
        Settings.System.putString(mResolver, GAMINGMODE_APPS, appsList);
    }

    private void resetPrefs() {
    	for(CheckBoxPreference checkBox: checkBoxes) {
    		checkBox.setChecked(false);
    		mEditor.putBoolean(checkBox.getKey(), false).apply();
        }
        Settings.System.putString(mResolver, GAMINGMODE_APPS, "");
    }
}
