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

import static android.provider.Settings.System.GAMINGMODE_ENABLED;
import static android.provider.Settings.System.GAMINGMODE_APPS;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public class GamingModeController {

    private static String TAG = "GamingModeController";
    private Context mContext;
    private ArrayList<String> mList;
    private ContentResolver mResolver;

    public GamingModeController(Context context) {
        mContext = context;
        mList = new ArrayList<>();
        mResolver = mContext.getContentResolver();
        if (Settings.System.getInt(mResolver, GAMINGMODE_ENABLED, -1) == -1) {
            Settings.System.putInt(mResolver, GAMINGMODE_ENABLED, 0);
        }
        getEnabledAppsList();
    }

    private void getEnabledAppsList() {
        String list = Settings.System.getString(mResolver, GAMINGMODE_APPS);
        if (list != null) {
            for (String packageName: list.split(" ")) {
                Log.d(TAG, packageName);
                mList.add(packageName);
            }
        }
    }

    public void notifyAppOpened(String packageName) {
        if (isAppInEnabledList(packageName)) {
            Toast.makeText(mContext, "Gaming Mode Enabled", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isEnabled() {
        return Settings.System.getInt(mResolver, GAMINGMODE_ENABLED, -1) == 1 ? true : false;
    }

    public boolean isAppInEnabledList(String packageName) {
        if (mList != null) {
            for (String name: mList) {
                if (packageName.equals(name)) {
                    return true;
                }
            }
        }
        else {
            Log.d(TAG, "Enabled apps list is null");
        }
        return false;
    }
}
