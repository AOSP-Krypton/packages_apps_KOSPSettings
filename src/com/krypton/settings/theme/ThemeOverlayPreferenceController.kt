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
 * limitations under the License.
 */

package com.krypton.settings.theme

import android.content.Context
import android.content.om.IOverlayManager
import android.content.om.OverlayInfo
import android.content.pm.PackageManager
import android.os.RemoteException
import android.os.ServiceManager
import android.os.UserHandle
import android.provider.Settings
import android.util.Log

import androidx.preference.ListPreference
import androidx.preference.Preference

import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.krypton.settings.KryptonBasePreferenceController

import org.json.JSONException
import org.json.JSONObject

/**
 * [Preference] controller for theme customisation overlays.
 *
 * @param context the [Context] for this controller.
 * @param key the preference key.
 * @param categoryPackageMap a [Map] of an overlays category to it's target package.
 */
class ThemeOverlayPreferenceController(
    context: Context,
    key: String,
    private val categoryPackageMap: Map<String, String>
) : KryptonBasePreferenceController(context, key),
    Preference.OnPreferenceChangeListener {

    private val overlayManager = IOverlayManager.Stub.asInterface(
        ServiceManager.getService(Context.OVERLAY_SERVICE))
    private val packageManager = context.packageManager
    private val defaultLabel = context.getString(R.string.overlay_option_device_default)

    /**
     * [MutableMap] of overlay label to a list of [OverlayInfo]s with same label.
     */
    private var overlayInfos: MutableMap<String, MutableList<OverlayInfo>> = getOverlayInfos()

    override fun getAvailabilityStatus() = AVAILABLE

    override fun updateState(preference: Preference) {
        val listPreference = preference as ListPreference
        var labels = mutableListOf<String>()

        var selectedLabel = defaultLabel

        overlayInfos = getOverlayInfos()
        overlayInfos.forEach { label, list ->
            labels.add(label)
            if (list.any { it.isEnabled() }) {
                selectedLabel = label
            }
        }
        // Sort the labels lexicographically.
        labels.sort()
        // Keep default one at top always
        labels.add(0, defaultLabel)

        listPreference.entries = Array(labels.size) { labels[it] }
        listPreference.entryValues = listPreference.entries
        listPreference.value = selectedLabel
        listPreference.summary = selectedLabel
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        // Map of overlay category to overlay package stored as a [JSONObject] flattened to string
        val overlayPackageJson: String? = Settings.Secure.getStringForUser(
                mContext.contentResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                UserHandle.USER_CURRENT)
        try {
            val jsonObject = overlayPackageJson?.let { JSONObject(it) } ?: JSONObject()
            newValue?.let { value ->
                if (value == defaultLabel) {
                    // User selected default, delete all elements for
                    // the categories of this preference.
                    categoryPackageMap.keys.forEach { category ->
                        jsonObject.remove(category)
                    }
                    return@let
                }
                overlayInfos[value as String]?.let {
                    categoryPackageMap.forEach { category, target ->
                        val overlayInfo = it.find { oi -> oi.targetPackageName == target }
                        if (overlayInfo == null) {
                            // There is no overlay for this category, delete entry
                            // if present so that system default will be used for it.
                            jsonObject.remove(category)
                        } else {
                            jsonObject.put(category, overlayInfo.packageName)
                        }
                    }
                }
            }
            Settings.Secure.putStringForUser(mContext.contentResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                jsonObject.toString(), UserHandle.USER_CURRENT)
        } catch (e: JSONException) {
            Log.e(TAG, "JSONException while processing json, ${e.message}")
        }
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun getOverlayInfos(): MutableMap<String, MutableList<OverlayInfo>> {
        // A map of overlay name to all the OverlayInfo's with the same name
        val filteredInfos = mutableMapOf<String, MutableList<OverlayInfo>>()
        try {
            categoryPackageMap.forEach { category, target ->
                val overlays = overlayManager.getOverlayInfosForTarget(
                        target, UserHandle.USER_SYSTEM) as List<OverlayInfo>
                overlays.filter {
                    // Filter based on category.
                    it.category == category
                }.forEach {
                    try {
                        val label = packageManager.getApplicationInfo(it.packageName,
                            0).loadLabel(packageManager).toString()
                        if (!filteredInfos.containsKey(label)) {
                            filteredInfos.put(label, mutableListOf(it))
                        } else {
                            filteredInfos[label]!!.add(it)
                        }
                    } catch (_: PackageManager.NameNotFoundException) {}
                }
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "RemoteException while getting overlay infos")
        }
        return filteredInfos
    }

    companion object {
        private const val TAG = "ThemeOverlayPreferenceController"
    }
}
