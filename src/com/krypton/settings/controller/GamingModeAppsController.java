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

package com.krypton.settings.controller;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import java.util.List;
import java.util.ArrayList;

public class GamingModeAppsController {
    private Context mContext;
    private ArrayList<PackageInfo> userApps;
    private PackageManager pm;
    private PreferenceScreen mPrefScreen;
    private SharedPreferences sharedPrefs;
    private Editor mEditor;

    public GamingModeAppsController(Context context, PreferenceScreen prefScreen) {
        mContext = context;
        mPrefScreen = prefScreen;
        sharedPrefs = mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_PRIVATE);
        mEditor = sharedPrefs.edit();
        pm = mContext.getPackageManager();
        userApps = new ArrayList<>();
        fetchAppsList();
        setView();
    }

    private void fetchAppsList() {
        for (PackageInfo packageInfo: pm.getInstalledPackages(0)) {
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                userApps.add(packageInfo);
            }
        }
    }

    private void setView() {
        for (PackageInfo packageInfo: userApps) {
            CheckBoxPreference checkBox = new CheckBoxPreference(mContext);
            checkBox.setIcon(packageInfo.applicationInfo.loadIcon(pm));
            checkBox.setTitle(packageInfo.applicationInfo.loadLabel(pm));
            checkBox.setKey(packageInfo.applicationInfo.className);
            checkBox.setChecked(sharedPrefs.getBoolean(checkBox.getKey(), false));
            checkBox.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mEditor.putBoolean(preference.getKey(), ((CheckBoxPreference) preference).isChecked()).apply();
                    return true;
                }
            });
            mPrefScreen.addPreference(checkBox);
        }
    }
}
