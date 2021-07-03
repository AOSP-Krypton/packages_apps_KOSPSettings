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
import android.provider.Settings;
import android.util.Log;

public class Utils {

    private static final String TAG = "KryptonSettingsUtils";
    public static final String TYPE_GLOBAL = "global";
    public static final String TYPE_SYSTEM = "system";
    public static final String TYPE_SECURE = "secure";

    public static boolean isEmpty(String str) {
        return str == null || str.equals("");
    }

    public static boolean applySetting(Context context, String type, String key, boolean value) {
        return applySetting(context, type, key, value ? 1 : 0);
    }

    public static boolean applySetting(Context context, String type, String key, int value) {
        if (isEmpty(type) || Utils.isEmpty(key)) {
            Log.e(TAG, "settingKey or settingNamespace attribute is empty, skipping");
            return false;
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

    public static boolean getSettingBoolean(Context context, String type, String key) {
        return getSettingInt(context, type, key) == 1;
    }

    public static boolean getSettingBoolean(Context context, String type, String key, int def) {
        return getSettingInt(context, type, key, def) == 1;
    }

    public static int getSettingInt(Context context, String type, String key) {
        return getSettingInt(context, type, key, 0);
    }

    public static int getSettingInt(Context context, String type, String key, int def) {
        if (isEmpty(type) || isEmpty(key)) {
            Log.e(TAG, "type or key is empty, returning default value");
            return 0;
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
}
