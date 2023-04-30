/*
 * Copyright (C) 2021-2023 AOSP-Krypton Project
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

package com.kosp.settings.theme

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.fonts.FontFamilyUpdateRequest
import android.graphics.fonts.FontFileUpdateRequest
import android.graphics.fonts.FontFileUtil
import android.graphics.fonts.FontManager
import android.graphics.fonts.FontStyle
import android.net.Uri
import android.os.FileUtils
import android.os.ParcelFileDescriptor
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import android.widget.Toast

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen

import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.core.lifecycle.Lifecycle

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.json.JSONException
import org.json.JSONObject

class CustomFontPreferenceController(
    context: Context,
    key: String,
    host: ThemeSettings?,
    lifecycle: Lifecycle?,
) : BasePreferenceController(context, key),
    LifecycleEventObserver {

    private val activityResultLauncher: ActivityResultLauncher<Array<String>>?
    private val fontManager = context.getSystemService(FontManager::class.java)

    private lateinit var coroutineScope: CoroutineScope

    private var preference: Preference? = null

    init {
        activityResultLauncher = host?.registerForActivityResult(OpenDocument()) {
            if (it != null) {
                coroutineScope.launch {
                    updateFont(it)
                }
            }
        }
        lifecycle?.addObserver(this)
    }

    override fun onStateChanged(owner: LifecycleOwner, event: Event) {
        if (event == Event.ON_CREATE) {
            coroutineScope = CoroutineScope(Dispatchers.Main)
        } else if (event == Event.ON_DESTROY) {
            coroutineScope.cancel()
        }
    }

    override fun getAvailabilityStatus(): Int {
        val overlayPackageJson: String? = Settings.Secure.getStringForUser(
            mContext.contentResolver,
            Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
            UserHandle.USER_CURRENT,
        )
        try {
            val jsonObject = if (overlayPackageJson == null) {
                JSONObject()
            } else {
                JSONObject(overlayPackageJson)
            }
            val isCustomFontMode = jsonObject.optString(
                OVERLAY_CATEGORY_FONT) == CUSTOM_FONT_OVERLAY_PACKAGE
            return if (isCustomFontMode) {
                AVAILABLE
            } else {
                DISABLED_DEPENDENT_SETTING
            }
        } catch (e: JSONException) {
            Log.e(TAG, "JSONException while parsing json, ${e.message}")
        }
        return CONDITIONALLY_UNAVAILABLE
    }

    override fun getSummary(): CharSequence? {
        val regularFontFamily = fontManager.fontConfig.fontFamilies.find {
            it.name?.contains(CUSTOM_FONT_FAMILY_REGULAR_NAME) == true
        }
        return if (regularFontFamily == null) {
            mContext.getString(R.string.overlay_option_device_default)
        } else {
            regularFontFamily.fontList.first().postScriptName
        }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference<Preference>(preferenceKey)?.also {
            it.setOnPreferenceClickListener {
                showFontPickerActivity()
                true
            }
        }
        screen.findPreference<Preference>(FOOTER_PREF_KEY)?.setEnabled(getAvailabilityStatus() == AVAILABLE)
    }

    private fun showFontPickerActivity() {
        try {
            activityResultLauncher?.launch(FONT_MIME_TYPE)
        } catch (_: ActivityNotFoundException) {
            toast(R.string.cannot_resolve_activity)
        }
    }

    private suspend fun updateFont(uri: Uri) {
        val cacheFile = withContext(Dispatchers.IO) {
            mContext.contentResolver.openFileDescriptor(uri, "r" /** RO mode */).use { fd ->
                runCatching {
                    copyFileToCache(fd)
                }.getOrNull()
            }
        } ?: run {
            toast(R.string.failed_to_copy_file_to_cache)
            return
        }
        withContext(Dispatchers.IO) {
            val result = runCatching {
                cacheFile.inputStream().use Main@ { inStream ->
                    inStream.channel.use { fileChannel ->
                        val byteBuffer: MappedByteBuffer = fileChannel.map(
                            FileChannel.MapMode.READ_ONLY, 0,
                            fileChannel.size(),
                        )
                        val postScriptName = FontFileUtil.getPostScriptName(
                            byteBuffer, 0
                        ) ?: run {
                            coroutineScope.launch {
                                toast(R.string.invalid_font_file)
                            }
                            return@Main
                        }
                        ParcelFileDescriptor.dup(inStream.fd).use {
                            updateFontFamily(it, postScriptName)
                        }
                    }
                }
                cacheFile.delete()
                return@runCatching
            }
            if (result.isFailure) {
                Log.e(TAG, "Failed to parse font", result.exceptionOrNull())
            }
        }
    }

    private fun copyFileToCache(pfd: ParcelFileDescriptor): File? =
        FileInputStream(pfd.fileDescriptor).use { inStream ->
            val cacheFile = File(mContext.cacheDir, FONT_CACHE_FILE_NAME)
            cacheFile.outputStream().use { outStream ->
                FileUtils.copy(inStream, outStream)
                outStream.flush()
            }
            cacheFile
        }

    private fun updateFontFamily(fontFd: ParcelFileDescriptor, postScriptName: String) {
        val fontFileUpdateRequest = FontFileUpdateRequest(fontFd, byteArrayOf())

        val fontRegular = FontFamilyUpdateRequest.Font.Builder(
            postScriptName,
            FontStyle(),
        ).build()
        val fontMedium = FontFamilyUpdateRequest.Font.Builder(
            postScriptName,
            FontStyle(
                FontStyle.FONT_WEIGHT_MEDIUM,
                FontStyle.FONT_SLANT_UPRIGHT,
            ),
        ).build()

        val fontFamilyRegular = FontFamilyUpdateRequest.FontFamily.Builder(
            CUSTOM_FONT_FAMILY_REGULAR_NAME,
            listOf(fontRegular),
        ).build()
        val fontFamilyMedium = FontFamilyUpdateRequest.FontFamily.Builder(
            CUSTOM_FONT_FAMILY_MEDIUM_NAME,
            listOf(fontMedium),
        ).build()

        val fontFamilyUpdateRequest = FontFamilyUpdateRequest.Builder()
            .addFontFileUpdateRequest(fontFileUpdateRequest)
            .addFontFamily(fontFamilyRegular)
            .addFontFamily(fontFamilyMedium)
            .build()

        val result = fontManager.updateFontFamily(
            fontFamilyUpdateRequest,
            fontManager.fontConfig.configVersion,
        )
        if (result != FontManager.RESULT_SUCCESS) {
            Log.e(TAG, "Result code = $result")
            toast(R.string.failed_to_update_font)
            return
        }
        coroutineScope.launch {
            preference?.setSummary(postScriptName)
        }
    }

    private fun toast(msgId: Int) {
        Toast.makeText(mContext, msgId, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "CustomFontPreferenceController"

        private const val OVERLAY_CATEGORY_FONT = "android.theme.customization.font"
        private const val CUSTOM_FONT_OVERLAY_PACKAGE = "com.kosp.theme.font.custom"

        private const val FOOTER_PREF_KEY = "custom_font_preference_footer"

        private const val FONT_CACHE_FILE_NAME = "font_cache.ttf"

        private const val CUSTOM_FONT_FAMILY_REGULAR_NAME = "custom-font"
        private const val CUSTOM_FONT_FAMILY_MEDIUM_NAME = "custom-font-medium"
        private val FONT_MIME_TYPE = arrayOf("font/ttf")
    }
}