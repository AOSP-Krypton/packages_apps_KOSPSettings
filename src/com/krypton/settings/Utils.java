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

package com.krypton.settings;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import androidx.core.graphics.ColorUtils;

import java.util.regex.Pattern;

public class Utils {
    private static final String TAG = "KryptonSettingsUtils";
    public static final String TYPE_GLOBAL = "global";
    public static final String TYPE_SYSTEM = "system";
    public static final String TYPE_SECURE = "secure";
    public static final Pattern HEX_PATTERN = Pattern.compile("[0-9A-F]+");

    public static boolean isEmpty(String str) {
        return str == null || str.equals("");
    }

    public static boolean applySetting(Context context, String key, boolean value) {
        return applySetting(context, TYPE_SYSTEM, key, value);
    }

    public static boolean applySetting(Context context, String type, String key, boolean value) {
        return applySetting(context, type, key, value ? 1 : 0);
    }

    public static boolean applySetting(Context context, String key, int value) {
        return applySetting(context, TYPE_SYSTEM, key, value);
    }

    public static boolean applySetting(Context context, String type, String key, int value) {
        if (isEmpty(key)) {
            Log.e(TAG, "key is empty, skipping");
            return false;
        }
        if (isEmpty(type)) {
            type = TYPE_SYSTEM;
        }
        if (type.equals(TYPE_SYSTEM)) {
            return Settings.System.putInt(context.getContentResolver(), key, value);
        } else if (type.equals(TYPE_SECURE)) {
            return Settings.Secure.putInt(context.getContentResolver(), key, value);
        } else if (type.equals(TYPE_GLOBAL)) {
            return Settings.Global.putInt(context.getContentResolver(), key, value);
        } else {
            return false;
        }
    }

    public static boolean getSettingBoolean(Context context, String key) {
        return getSettingBoolean(context, TYPE_SYSTEM, key);
    }

    public static boolean getSettingBoolean(Context context, String key, int def) {
        return getSettingBoolean(context, TYPE_SYSTEM, key, def);
    }

    public static boolean getSettingBoolean(Context context, String type, String key) {
        return getSettingInt(context, type, key) == 1;
    }

    public static boolean getSettingBoolean(Context context, String type, String key, int def) {
        return getSettingInt(context, type, key, def) == 1;
    }

    public static int getSettingInt(Context context, String key) {
        return getSettingInt(context, TYPE_SYSTEM, key, 0);
    }

    public static int getSettingInt(Context context, String key, int def) {
        return getSettingInt(context, TYPE_SYSTEM, key, def);
    }

    public static int getSettingInt(Context context, String type, String key) {
        return getSettingInt(context, type, key, 0);
    }

    public static int getSettingInt(Context context, String type, String key, int def) {
        if (isEmpty(key)) {
            Log.e(TAG, "key is empty, returning default value");
            return 0;
        }
        if (isEmpty(type)) {
            type = TYPE_SYSTEM;
        }
        if (type.equals(TYPE_SYSTEM)) {
            return Settings.System.getInt(context.getContentResolver(), key, def);
        } else if (type.equals(TYPE_SECURE)) {
            return Settings.Secure.getInt(context.getContentResolver(), key, def);
        } else if (type.equals(TYPE_GLOBAL)) {
            return Settings.Global.getInt(context.getContentResolver(), key, def);
        } else {
            return 0;
        }
    }

    public static String getStringFromSettings(Context context, String key) {
        return getStringFromSettings(context, TYPE_SYSTEM, key);
    }

    public static String getStringFromSettings(Context context, String type, String key) {
        if (isEmpty(key)) {
            Log.e(TAG, "key is empty, returning default value");
            return null;
        }
        if (isEmpty(type)) {
            type = TYPE_SYSTEM;
        }
        if (type.equals(TYPE_SYSTEM)) {
            return Settings.System.getString(context.getContentResolver(), key);
        } else if (type.equals(TYPE_SECURE)) {
            return Settings.Secure.getString(context.getContentResolver(), key);
        } else if (type.equals(TYPE_GLOBAL)) {
            return Settings.Global.getString(context.getContentResolver(), key);
        } else {
            return null;
        }
    }

    public static void putStringInSettings(Context context, String key, String value) {
        putStringInSettings(context, TYPE_SYSTEM, key, value);
    }

    public static void putStringInSettings(Context context, String type, String key, String value) {
        if (isEmpty(key)) {
            Log.e(TAG, "key is empty, skipping");
            return;
        }
        if (isEmpty(type)) {
            type = TYPE_SYSTEM;
        }
        if (type.equals(TYPE_SYSTEM)) {
            Settings.System.putString(context.getContentResolver(), key, value);
        } else if (type.equals(TYPE_SECURE)) {
            Settings.Secure.putString(context.getContentResolver(), key, value);
        } else if (type.equals(TYPE_GLOBAL)) {
            Settings.Global.putString(context.getContentResolver(), key, value);
        }
    }

    public static void sleepThread(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Log.e(TAG, "thread interrupted while sleep", e);
        }
    }

    public static Uri getUri(String type, String key) {
        if (isEmpty(key)) {
            Log.e(TAG, "key is empty, returning null");
            return null;
        }
        if (isEmpty(type)) {
            type = TYPE_SYSTEM;
        }
        if (type.equals(TYPE_SYSTEM)) {
            return Settings.System.getUriFor(key);
        } else if (type.equals(TYPE_SECURE)) {
            return Settings.Secure.getUriFor(key);
        } else if (type.equals(TYPE_GLOBAL)) {
            return Settings.Global.getUriFor(key);
        } else {
            return null;
        }
    }

    public static int HSVToColor(float hue, float sat, float val) {
        return Color.HSVToColor(new float[] {hue, sat, val});
    }

    public static int HSLToColor(float hue, float sat, float lum) {
        return ColorUtils.HSLToColor(new float[] {hue, sat, lum});
    }
}
