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
import android.widget.Toast;

public class GamingModeController {

    private Context mContext;
    private ContentResolver mResolver;

    public GamingModeController(Context context) {
        mContext = context;
        mResolver = mContext.getContentResolver();
        if (Settings.System.getInt(mResolver, GAMINGMODE_ENABLED, -1) == -1) {
            Settings.System.putInt(mResolver, GAMINGMODE_ENABLED, 0);
        }
    }

    public void notifyAppOpened(String packageName) {
        if (isActivatedForApp(packageName)) {
            Toast.makeText(mContext, "Gaming Mode Enabled", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isEnabled() {
        return Settings.System.getInt(mResolver, GAMINGMODE_ENABLED, -1) == 1 ? true : false;
    }

    public boolean isActivatedForApp(String packageName) {
        return Settings.System.getString(mResolver, GAMINGMODE_APPS).contains(packageName);
    }
}
