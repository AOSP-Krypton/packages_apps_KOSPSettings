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
import static android.provider.Settings.System.GAMINGMODE_LOCK_BRIGHTNESS;
import static android.provider.Settings.System.GAMINGMODE_RESTORE_BRIGHTNESS;
import static android.provider.Settings.System.GAMINGMODE_ENABLED;
import static android.provider.Settings.System.GAMINGMODE_RINGERMODE;
import static android.provider.Settings.System.GAMINGMODE_TOAST;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.Toast;

public class GamingModeController {
    private Context mContext;
    private ContentResolver mResolver;
    private AudioManager mAudioManager;
    private BrightnessObserver mBrightnessObserver;
    private Handler mHandler;
    private String mList;
    private boolean mIsActive = false;
    private boolean mRestoredBrightness = false;
    private boolean mChangedBrightnessMode = false;
    private int mOldBrightness = -1;
    private int mPrevRingerMode;

    public GamingModeController(Context context) {
        mContext = context;
        mResolver = mContext.getContentResolver();
        mHandler = new Handler(Looper.getMainLooper());
        mBrightnessObserver = new BrightnessObserver(mHandler);
        mBrightnessObserver.observe();
        mAudioManager = mContext.getSystemService(AudioManager.class);
    }

    private class BrightnessObserver extends ContentObserver {
        BrightnessObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mResolver.registerContentObserver(Settings.System.getUriFor(SCREEN_BRIGHTNESS),
                false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateBrightness();
        }

        public void updateBrightness() {
            if (mIsActive) {
                if (mRestoredBrightness) {
                    mRestoredBrightness = false;
                } else {
                    int brightness = getInt(SCREEN_BRIGHTNESS, 100);
                    putInt(GAMINGMODE_BRIGHTNESS, brightness);
                }
            }
        }
    }

    public void notifyAppOpened(String packageName) {
        if (isActivatedForApp(packageName)) {
            putInt(GAMINGMODE_ACTIVE, 1);
            mIsActive = true;
            mPrevRingerMode = mAudioManager.getRingerModeInternal();
            switch (getInt(GAMINGMODE_RINGERMODE)) {
                case 1:
                    mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);
                    break;
                case 2:
                    mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
                    break;
            }
            if (getBool(GAMINGMODE_LOCK_BRIGHTNESS) &&
                    (getInt(SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL) ==
                        SCREEN_BRIGHTNESS_MODE_AUTOMATIC)) {
                putInt(SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
                mChangedBrightnessMode = true;
            }
            if (getBool(GAMINGMODE_TOAST))
                Toast.makeText(mContext, "Gaming Mode Enabled", Toast.LENGTH_SHORT).show();
            adjustBrightness();
        }
        else if (!isActivatedForApp(packageName) && mIsActive){
            putInt(GAMINGMODE_ACTIVE, 0);
            mIsActive = false;
            if (getInt(GAMINGMODE_RINGERMODE) != 0) {
                mAudioManager.setRingerModeInternal(mPrevRingerMode);
            }
            if (mChangedBrightnessMode) {
                putInt(SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
                mChangedBrightnessMode = false;
            }
            if (getBool(GAMINGMODE_TOAST))
                Toast.makeText(mContext, "Gaming Mode Disabled", Toast.LENGTH_SHORT).show();
            adjustBrightness();
        }
    }

    public boolean isEnabled() {
        return getBool(GAMINGMODE_ENABLED);
    }

    private boolean isActivatedForApp(String packageName) {
        updateList();
        return mList != null && mList.contains(packageName);
    }

    public void notifyPackageRemoved(String packageName) {
        if (isActivatedForApp(packageName)) removePackage(packageName);
    }

    private void removePackage(String packageName) {
        Settings.System.putString(mResolver,
            GAMINGMODE_APPS, mList.replace(packageName + " ", ""));
    }

    private void updateList() {
        mList = Settings.System.getString(mResolver, GAMINGMODE_APPS);
    }

    private void adjustBrightness() {
        if (getBool(GAMINGMODE_RESTORE_BRIGHTNESS)) {
            if (mIsActive) {
                if (!mRestoredBrightness) {
                    int storedBrightness = getInt(GAMINGMODE_BRIGHTNESS);
                    if (storedBrightness != -1) {
                        mRestoredBrightness = true;
                        mOldBrightness = getInt(SCREEN_BRIGHTNESS, 100);
                        putInt(SCREEN_BRIGHTNESS, storedBrightness);
                    }
                }
            } else if (mOldBrightness != -1) {
                putInt(SCREEN_BRIGHTNESS, mOldBrightness);
                mOldBrightness = -1;
            }
        }
    }

    private int getInt(String key, int def) {
        return Settings.System.getInt(mResolver, key, def);
    }

    private int getInt(String key) {
        return getInt(key, -1);
    }

    private boolean getBool(String key) {
        return getInt(key) == 1 ? true : false;
    }

    private void putInt(String key, int value) {
        Settings.System.putInt(mResolver, key, value);
    }
}
