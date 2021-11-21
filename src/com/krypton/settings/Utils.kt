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

package com.krypton.settings

import android.content.Context
import android.graphics.Color
import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal

import androidx.core.graphics.ColorUtils

import java.util.regex.Pattern

object Utils {
    val HEX_PATTERN = Pattern.compile("[0-9a-fA-F]+")

    fun HSVToColor(hue: Float, sat: Float, value: Float): Int = Color.HSVToColor(floatArrayOf(hue, sat, value))

    fun HSLToColor(hue: Float, sat: Float, lum: Float) = ColorUtils.HSLToColor(floatArrayOf(hue, sat, lum))

    // color values are always negative, use a - sign to make it positive
    fun colorToHex(color: Int) = String.format("#%06X", (0xFFFFFF and color))

    /**
     * Checks if the device has udfps.
     * @param context context for obtaining an instance FingerprintManager service.
     * @return true is udfps is present.
     */
    fun hasUDFPS(context: Context): Boolean {
        val fingerprintManager = context.getSystemService(FingerprintManager::class.java)
        val props: List<FingerprintSensorPropertiesInternal> = fingerprintManager.getSensorPropertiesInternal()
        return props.size == 1 && props[0].isAnyUdfpsType()
    }
}
