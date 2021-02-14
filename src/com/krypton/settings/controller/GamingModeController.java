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

import static android.provider.Settings.System.GAMINGMODE_ACTIVE;
import static android.provider.Settings.System.GAMINGMODE_APPS;
import static android.provider.Settings.System.GAMINGMODE_BRIGHTNESS;
import static android.provider.Settings.System.GAMINGMODE_ENABLED;
import static android.provider.Settings.System.GAMINGMODE_RINGERMODE;
import static android.provider.Settings.System.GAMINGMODE_TOAST;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;
import android.widget.Toast;

public class GamingModeController {

    private Context mContext;
    private ContentResolver mResolver;
    private AudioManager mAudioManager;

    public GamingModeController(Context context) {
        mContext = context;
        mResolver = mContext.getContentResolver();
        mAudioManager = mContext.getSystemService(AudioManager.class);
        setDefaults();
    }

    public void notifyAppOpened(String packageName) {
        if (isActivatedForApp(packageName)) {
            setActive(1);
            switch (getRingerMode()) {
                case 1:
                    mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);
                    break;
                case 2:
                    mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
                    break;
            }
            if (Settings.System.getInt(mResolver, GAMINGMODE_BRIGHTNESS, -1) == 1) {
                if (Settings.System.getInt(mResolver, SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL) == SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    Settings.System.putInt(mResolver, SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
                    Settings.System.putInt(mResolver, GAMINGMODE_BRIGHTNESS, 2);
                }
            }
            if (showToast()) Toast.makeText(mContext, "Gaming Mode Enabled", Toast.LENGTH_SHORT).show();
        }
        else if (!isActivatedForApp(packageName) && isActive()){
            setActive(0);
            if (getRingerMode() != 0) {
                mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
            }
            if (Settings.System.getInt(mResolver, GAMINGMODE_BRIGHTNESS, -1) == 2) {
                Settings.System.putInt(mResolver, SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
                Settings.System.putInt(mResolver, GAMINGMODE_BRIGHTNESS, 1);
            }
            if (showToast()) Toast.makeText(mContext, "Gaming Mode Disabled", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean showToast() {
        return Settings.System.getInt(mResolver, GAMINGMODE_TOAST, -1) == 1 ? true : false;
    }

    public boolean isEnabled() {
        return Settings.System.getInt(mResolver, GAMINGMODE_ENABLED, -1) == 1 ? true : false;
    }

    private void setEnabled(int status) {
        Settings.System.putInt(mResolver, GAMINGMODE_ENABLED, status);
    }

    private boolean isActive() {
        return Settings.System.getInt(mResolver, GAMINGMODE_ACTIVE, -1) == 1 ? true : false;
    }

    private void setActive(int status) {
        Settings.System.putInt(mResolver, GAMINGMODE_ACTIVE, status);
    }

    private int getRingerMode() {
        return Settings.System.getInt(mResolver, GAMINGMODE_RINGERMODE, -1);
    }

    private void setRingerMode(int mode) {
        Settings.System.putInt(mResolver, GAMINGMODE_RINGERMODE, mode);
    }

    private boolean isActivatedForApp(String packageName) {
        return Settings.System.getString(mResolver, GAMINGMODE_APPS).contains(packageName);
    }

    private void setDefaults() {
        if (Settings.System.getInt(mResolver, GAMINGMODE_ACTIVE, -1) == -1) {
            setActive(0);
        }
        if (Settings.System.getInt(mResolver, GAMINGMODE_ENABLED, -1) == -1) {
            setEnabled(0);
        }
        if (Settings.System.getInt(mResolver, GAMINGMODE_BRIGHTNESS, -1) == -1) {
            Settings.System.putInt(mResolver, GAMINGMODE_BRIGHTNESS, 0);
        }
        if (Settings.System.getInt(mResolver, GAMINGMODE_TOAST, -1) == -1) {
            Settings.System.putInt(mResolver, GAMINGMODE_TOAST, 0);
        }
        if (getRingerMode() == -1) {
            setRingerMode(0);
        }
    }
}
