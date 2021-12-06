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
 * limitations under the License.
 */
package com.krypton.settings.fragment.theme

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.fonts.FontFamilyUpdateRequest
import android.graphics.fonts.FontFileUpdateRequest
import android.graphics.fonts.FontFileUtil
import android.graphics.fonts.FontManager
import android.graphics.fonts.FontStyle
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.preference.Preference

import com.android.internal.util.krypton.KryptonUtils
import com.android.settings.R
import com.android.settingslib.core.AbstractPreferenceController
import com.krypton.settings.fragment.KryptonDashboardFragment

import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ThemeSettingsFragment: KryptonDashboardFragment() {

    private val activityResultLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var fontManager: FontManager
    private var customFontPreference: Preference? = null

    init {
        activityResultLauncher = registerForActivityResult(OpenDocument()) {
            if (it != null) parseFont(it)
        }
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        fontManager = context!!.getSystemService(FontManager::class.java)
        customFontPreference = findPreference<Preference>(CUSTOM_FONT_PREF_KEY)?.also {
            it.setOnPreferenceClickListener {
                showFontPickerActivity()
                true
            }
        }
    }

    override protected fun getPreferenceScreenResId() = R.xml.theme_settings

    override protected fun createPreferenceControllers(
        context: Context
    ): List<AbstractPreferenceController> {
        val isAospLauncherInstalled = KryptonUtils.isPackageInstalled(
            context, TARGET_LAUNCHER, false /** ignoreState */
        )
        val isAospThemePickerInstalled = KryptonUtils.isPackageInstalled(
            context, TARGET_THEME_PICKER, false /** ignoreState */
        )
        return listOf(
            CustomFontPreferenceController(context, CUSTOM_FONT_PREF_KEY),
            ThemeOverlayPreferenceController(context,
                "font_list_preference",
                mapOf(OVERLAY_CATEGORY_FONT to TARGET_ANDROID),
            ),
            ThemeOverlayPreferenceController(context,
                "icon_pack_list_preference",
                mutableMapOf(
                    OVERLAY_CATEGORY_ICON_ANDROID to TARGET_ANDROID,
                    OVERLAY_CATEGORY_ICON_SYSUI to TARGET_SYSUI,
                    OVERLAY_CATEGORY_ICON_SETTINGS to TARGET_SETTINGS,
                ).also {
                    // Conditionally add launcher and themepicker
                    if (isAospLauncherInstalled) it.put(
                        OVERLAY_CATEGORY_ICON_LAUNCHER, TARGET_LAUNCHER)
                    if (isAospThemePickerInstalled) it.put(
                        OVERLAY_CATEGORY_ICON_THEME_PICKER, TARGET_THEME_PICKER)
                },
            )
        )
    }

    override protected fun getLogTag() = TAG

    private fun showFontPickerActivity() {
        try {
            activityResultLauncher.launch(FONT_MIME_TYPE)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.cannot_resolve_activity, Toast.LENGTH_LONG).show()
        }
    }

    private fun parseFont(uri: Uri) {
        val parcelFileDescriptor = context!!.contentResolver.openFileDescriptor(uri, "r" /** RO mode */)
        try {
            FileInputStream(parcelFileDescriptor.fileDescriptor).use Main@ { inStream -> 
                inStream.channel.use { fileChannel ->
                    val byteBuffer: MappedByteBuffer = fileChannel.map(
                        FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())
                    val postScriptName = FontFileUtil.getPostScriptName(byteBuffer, 0)
                    if (postScriptName == null) {
                        Toast.makeText(context, R.string.invalid_font_file, Toast.LENGTH_LONG).show()
                        return@Main
                    }
                    updateFontFamily(parcelFileDescriptor, postScriptName)
                }
            }
        } catch (ex: IOException) {
            Log.e(TAG, "IOException while parsing font, ${ex.message}")
        }
    }

    private fun updateFontFamily(fontFd: ParcelFileDescriptor, postScriptName: String) {
        val fontFileUpdateRequest = FontFileUpdateRequest(fontFd, byteArrayOf())

        val fontRegular = FontFamilyUpdateRequest.Font.Builder(postScriptName, FontStyle()).build()
        val fontMedium = FontFamilyUpdateRequest.Font.Builder(postScriptName, FontStyle(
            FontStyle.FONT_WEIGHT_MEDIUM, FontStyle.FONT_SLANT_UPRIGHT)).build()

        val fontFamilyRegular = FontFamilyUpdateRequest.FontFamily.Builder(
            CUSTOM_FONT_FAMILY_REGULAR_NAME, listOf(fontRegular)).build()
        val fontFamilyMedium = FontFamilyUpdateRequest.FontFamily.Builder(
            CUSTOM_FONT_FAMILY_MEDIUM_NAME, listOf(fontMedium)).build()
        
        val fontFamilyUpdateRequest = FontFamilyUpdateRequest.Builder()
            .addFontFileUpdateRequest(fontFileUpdateRequest)
            .addFontFamily(fontFamilyRegular)
            .addFontFamily(fontFamilyMedium)
            .build()

        val result = fontManager.updateFontFamily(fontFamilyUpdateRequest,
            fontManager.fontConfig.configVersion)
        if (result != FontManager.RESULT_SUCCESS) {
            Log.e(TAG, "result code = $result")
            Toast.makeText(context, R.string.failed_to_update_font, Toast.LENGTH_LONG).show()
            return
        }
        customFontPreference?.setSummary(postScriptName)
    }
 
    companion object {
        private const val TAG = "ThemeSettingsFragment"

        private const val OVERLAY_CATEGORY_FONT = "android.theme.customization.font"
        private const val OVERLAY_CATEGORY_ICON_ANDROID = "android.theme.customization.icon_pack.android"
        private const val OVERLAY_CATEGORY_ICON_SYSUI = "android.theme.customization.icon_pack.systemui"
        private const val OVERLAY_CATEGORY_ICON_SETTINGS = "android.theme.customization.icon_pack.settings"
        private const val OVERLAY_CATEGORY_ICON_LAUNCHER = "android.theme.customization.icon_pack.launcher"
        private const val OVERLAY_CATEGORY_ICON_THEME_PICKER = "android.theme.customization.icon_pack.themepicker"

        private const val TARGET_ANDROID = "android"
        private const val TARGET_SYSUI = "com.android.systemui"
        private const val TARGET_SETTINGS = "com.android.settings"
        private const val TARGET_LAUNCHER = "com.android.launcher3"
        private const val TARGET_THEME_PICKER = "com.android.wallpaper"

        private const val CUSTOM_FONT_PREF_KEY = "custom_font_preference"
        const val CUSTOM_FONT_FAMILY_REGULAR_NAME = "custom-font"
        private const val CUSTOM_FONT_FAMILY_MEDIUM_NAME = "custom-font-medium"
        private val FONT_MIME_TYPE = arrayOf("font/ttf")
    }
}
