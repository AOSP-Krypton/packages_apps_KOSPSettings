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

import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

import java.util.regex.Pattern;

public class Utils {
    private static final String TAG = "KryptonSettingsUtils";
    public static final Pattern HEX_PATTERN = Pattern.compile("[0-9A-F]+");

    public static int HSVToColor(float hue, float sat, float val) {
        return Color.HSVToColor(new float[] {hue, sat, val});
    }

    public static int HSLToColor(float hue, float sat, float lum) {
        return ColorUtils.HSLToColor(new float[] {hue, sat, lum});
    }
}
