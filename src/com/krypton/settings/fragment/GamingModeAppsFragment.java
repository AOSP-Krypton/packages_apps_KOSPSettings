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
import static com.android.settings.search.actionbar.SearchMenuController.MENU_SEARCH;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.provider.Settings;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GamingModeAppsFragment extends SettingsPreferenceFragment {

    private static final String TAG = "GamingModeAppsFragment";
    private ArrayList<Pair<String, String>> mList;
    private Context mContext;
    private ContentResolver mResolver;
    private PackageManager pm;
    private PreferenceScreen mScreen;
    private SharedPreferences sharedPrefs;
    private Editor mEditor;
    private ExecutorService mExecutor;
    private Handler mHandler;
    private SearchView mSearchView;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.gamingmode_apps_screen);
        mContext = getContext();
        mResolver = mContext.getContentResolver();
        mHandler = new Handler(Looper.getMainLooper());
        mExecutor = Executors.newSingleThreadExecutor();
        mScreen = getPreferenceScreen();
        sharedPrefs = mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_PRIVATE);
        mEditor = sharedPrefs.edit();
        pm = mContext.getPackageManager();
        mList = new ArrayList<>();
        setView();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.KRYPTON;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.gamingmode_apps_menu, menu);
        MenuItem item = menu.findItem(R.id.search_apps_menu);
        mSearchView = (SearchView) item.getActionView();
        mSearchView.setQueryHint(mContext.getResources().getString(R.string.search_settings));
        handleSearch();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.reset_button) {
            resetPrefs();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleSearch() {
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterApps(newText);
                return true;
            }
        });
    }

    private void setView() {
        mExecutor.execute(() -> {
            for (PackageInfo packageInfo: getSortedList()) {
                CheckBoxPreference checkBox = new CheckBoxPreference(mContext);
                checkBox.setIcon(packageInfo.applicationInfo.loadIcon(pm));
                checkBox.setTitle(packageInfo.applicationInfo.loadLabel(pm));
                checkBox.setKey(packageInfo.packageName);
                checkBox.setChecked(sharedPrefs.getBoolean(checkBox.getKey(), false));
                checkBox.setOnPreferenceClickListener(preference -> {
                    putBoolean(preference.getKey(), ((CheckBoxPreference) preference).isChecked());
                    updateAppPrefs((CheckBoxPreference) preference);
                    return true;
                });
                mList.add(new Pair(checkBox.getKey(), checkBox.getTitle()));
                mHandler.post(() -> {
                    mScreen.addPreference(checkBox);
                });
            }
        });
    }

    private ArrayList<PackageInfo> getSortedList() {
        ArrayList<PackageInfo> list = new ArrayList<>();
        for (PackageInfo pInfo: pm.getInstalledPackages(0)) {
            if ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            list.add(pInfo);
        }
        for (int i=0; i < list.size(); i++) {
            for (int j=0; j < list.size(); j++) {
                String first = list.get(i).applicationInfo.loadLabel(pm).toString();
                String second = list.get(j).applicationInfo.loadLabel(pm).toString();
                if (first.compareToIgnoreCase(second) < 0) {
                    PackageInfo temp = list.get(i);
                    list.set(i, list.get(j));
                    list.set(j, temp);
                }
            }
        }
        return list;
    }

    private void updateAppPrefs(CheckBoxPreference preference) {
        String prefPackageName = preference.getKey();
        String appsList = getList();
        if (appsList != null) {
            if (preference.isChecked())
                if (!appsList.contains(prefPackageName)) appsList += prefPackageName + " ";
            else appsList = appsList.replace(prefPackageName + " ", "");
        }
        else appsList = prefPackageName + " ";
        amendList(appsList);
    }

    private void filterApps(String query) {
        if (query != null) {
            mExecutor.execute(() -> {
                for (Pair<String, String> pkg: mList) {
                    mHandler.post(() -> {
                        Preference pref = findPreference(pkg.first);
                        if (pref != null) pref.setVisible(query.isEmpty() ||
                            pkg.second.toLowerCase().contains(query.toLowerCase()));
                    });
                }
            });
        }
    }

    private void resetPrefs() {
        String appsList = getList();
        if(appsList != null) {
            amendList(null);
            for (String key: appsList.split(" ")) {
                CheckBoxPreference checkBox = (CheckBoxPreference) findPreference(key);
                if (checkBox != null) {
                    checkBox.setChecked(false);
                    putBoolean(key, false);
                }
            }
        }
    }

    private String getList() {
        return Settings.System.getString(mResolver, GAMINGMODE_APPS);
    }

    private void amendList(String newList) {
        Settings.System.putString(mResolver, GAMINGMODE_APPS, newList);
    }

    private void putBoolean(String key, boolean state) {
        mEditor.putBoolean(key, state).apply();
    }
}
