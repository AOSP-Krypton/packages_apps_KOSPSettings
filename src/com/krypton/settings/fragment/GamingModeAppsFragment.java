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

import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.ApplicationInfo.FLAG_SYSTEM;
import static android.provider.Settings.System.GAMINGMODE_APPS;
import static com.android.settings.search.actionbar.SearchMenuController.MENU_SEARCH;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class GamingModeAppsFragment extends SettingsPreferenceFragment {
    private static final String mSeparator = "|";
    private HashMap<String, String> mList;
    private Context mContext;
    private ContentResolver mResolver;
    private PackageManager mPm;
    private PreferenceScreen mScreen;
    private SharedPreferences mSharedPrefs;
    private Editor mEditor;
    private ExecutorService mExecutor;
    private Handler mHandler;
    private SearchView mSearchView;
    private String mEnabledApps;

    private final OnPreferenceClickListener mListener = preference -> {
        String key = preference.getKey();
        if (key != null) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            putBoolean(key, checked);
            updateAppsList(key, checked);
            return true;
        } else {
            return false;
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.gamingmode_apps_screen);
        mContext = getContext();
        mResolver = mContext.getContentResolver();
        mHandler = new Handler(Looper.getMainLooper());
        mExecutor = Executors.newSingleThreadExecutor();
        mScreen = getPreferenceScreen();
        mSharedPrefs = mContext.getSharedPreferences(mContext.getPackageName(), MODE_PRIVATE);
        mEditor = mSharedPrefs.edit();
        mPm = mContext.getPackageManager();
        mList = new HashMap<>();
        mEnabledApps = getString(GAMINGMODE_APPS);
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
        mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
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
            final List<PackageInfo> list = mPm.getInstalledPackages(0);
            for (int i = 0, size = list.size(); i < size;) {
                if ((list.get(i).applicationInfo.flags & FLAG_SYSTEM) != 0) {
                    list.remove(i);
                    size--;
                } else {
                    i++;
                }
            }
            list.sort(new Comparator<PackageInfo>() {
                @Override
                public int compare(PackageInfo pInfo1, PackageInfo pInfo2) {
                    return pInfo1.applicationInfo.loadLabel(mPm).toString()
                        .compareToIgnoreCase(pInfo2.applicationInfo.loadLabel(mPm).toString());
                }
            });
            for (PackageInfo pInfo: list) {
                String key = pInfo.packageName;
                String name = pInfo.applicationInfo.loadLabel(mPm).toString();
                final CheckBoxPreference checkBox = new CheckBoxPreference(mContext);
                checkBox.setIcon(pInfo.applicationInfo.loadIcon(mPm));
                checkBox.setTitle(name);
                checkBox.setKey(key);
                checkBox.setChecked(mSharedPrefs.getBoolean(key, false));
                checkBox.setOnPreferenceClickListener(mListener);
                mHandler.post(() -> mScreen.addPreference(checkBox));
                mList.put(key, name);
            }
        });
    }

    private void updateAppsList(String key, boolean checked) {
        if (mEnabledApps != null) {
            if (checked && !mEnabledApps.contains(key)) {
                mEnabledApps += key + mSeparator;
            } else if (!checked && mEnabledApps.contains(key)) {
                mEnabledApps = mEnabledApps.replace(key + mSeparator, "");
            }
        } else if (checked) {
            mEnabledApps = key + mSeparator;
        }
        putString(GAMINGMODE_APPS, mEnabledApps);
    }

    private void filterApps(String query) {
        if (query != null) {
            mExecutor.execute(() -> {
                for (String key: mList.keySet()) {
                    Preference pref = findPreference(key);
                    if (pref != null) {
                        boolean match = query.isEmpty() ||
                            mList.get(key).toLowerCase().contains(query.toLowerCase());
                        mHandler.post(() -> pref.setVisible(match));
                    }
                }
            });
        }
    }

    private void resetPrefs() {
        if (mEnabledApps != null) {
            for (String key: mEnabledApps.split(mSeparator)) {
                CheckBoxPreference checkBox = (CheckBoxPreference) findPreference(key);
                if (checkBox != null) {
                    checkBox.setChecked(false);
                    putBoolean(key, false);
                }
            }
        }
        putString(GAMINGMODE_APPS, null);
    }

    private void putBoolean(String key, boolean state) {
        mEditor.putBoolean(key, state).apply();
    }

    private String getString(String key) {
        return Settings.System.getString(mResolver, key);
    }

    private void putString(String key, String value) {
        Settings.System.putString(mResolver, key, value);
    }
}
