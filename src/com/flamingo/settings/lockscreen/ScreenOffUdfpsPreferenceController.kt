/*
 * Copyright (C) 2022 PixelExperience
 * Copyright (C) 2022 Flamingo-OS Project
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

package com.flamingo.settings.lockscreen

import android.content.Context
import android.hardware.display.AmbientDisplayConfiguration
import com.android.settings.Utils
import com.android.settings.core.BasePreferenceController

@Suppress("DEPRECATION")
class ScreenOffUdfpsPreferenceController(
    context: Context,
    preferenceKey: String
) : BasePreferenceController(context, preferenceKey) {

    private val fingerprintManager: android.hardware.fingerprint.FingerprintManager? =
        Utils.getFingerprintManagerOrNull(context)
    private val ambientDisplayConfig = AmbientDisplayConfiguration(context)

    override fun getAvailabilityStatus(): Int {
        if (fingerprintManager == null) {
            return UNSUPPORTED_ON_DEVICE
        }
        val isUdfps = fingerprintManager.sensorPropertiesInternal.find { it.isAnyUdfpsType } != null
        return when {
            !isUdfps ||
                !fingerprintManager.isHardwareDetected ||
                ambientDisplayConfig.udfpsLongPressSensorType().isBlank() -> UNSUPPORTED_ON_DEVICE
            !fingerprintManager.hasEnrolledFingerprints() -> CONDITIONALLY_UNAVAILABLE
            else -> AVAILABLE
        }
    }
}
