/*
 * Copyright (C) 2021-2022 AOSP-Krypton Project
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
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal

object Utils {
    private const val BOOL_RES_TYPE = "bool"
    private const val SYSTEMUI_PACKAGE = "com.android.systemui"

    /**
     * Checks if the device has udfps.
     * @param context context for obtaining an instance FingerprintManager service.
     * @return true is udfps is present.
     */
    @Suppress("DEPRECATION")
    fun hasUDFPS(context: Context): Boolean {
        val fingerprintManager = context.getSystemService(android.hardware.fingerprint.FingerprintManager::class.java)
        val props: List<FingerprintSensorPropertiesInternal> = fingerprintManager.getSensorPropertiesInternal()
        return props.size == 1 && props[0].isAnyUdfpsType()
    }

    /**
     * Get value of a systemui resource.
     *
     * @param context context for obtaining for resources for systemui.
     * @param resName the name of the resource.
     * @param def the default value to return if resource is not found.
     */
    fun getBoolSysUIResource(
        context: Context,
        resName: String,
        def: Boolean = false
    ): Boolean {
        val res = context.packageManager.getResourcesForApplication(SYSTEMUI_PACKAGE)
        return res.getIdentifier(resName, BOOL_RES_TYPE, SYSTEMUI_PACKAGE)
            .takeIf { resId -> resId != 0 }
            ?.let { res.getBoolean(it) } ?: def
    }
}
