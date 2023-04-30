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

package com.kosp.settings.miscellaneous

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.database.ContentObserver
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.provider.Settings.Secure.FORCE_FULLSCREEN_CUTOUT_APPS

import androidx.annotation.GuardedBy
import androidx.lifecycle.lifecycleScope
import androidx.preference.forEach

import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settingslib.widget.AppSwitchPreference

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CutoutForceFullscreenSettings : SettingsPreferenceFragment() {

    private val settingsObserver = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            lifecycleScope.launch {
                update()
            }
        }
    }

    private val mutex = Mutex()
    @GuardedBy("mutex")
    private val appInfos = mutableMapOf<String, AppInfo>()

    override fun getMetricsCategory(): Int = MetricsEvent.KOSP

    override fun onStart() {
        super.onStart()
        requireContext().contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(FORCE_FULLSCREEN_CUTOUT_APPS),
            false,
            settingsObserver,
            UserHandle.USER_CURRENT
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.cutout_force_fullscreen_settings)
        lifecycleScope.launch {
            loadAppInfos()
        }
    }

    private suspend fun loadAppInfos() {
        val context = requireContext()
        val pm = context.packageManager
        withContext(Dispatchers.Default) {
            mutex.withLock {
                val selectedApps = getSelectedApps(context)
                val loadedAppInfos = pm.getInstalledPackages(PackageInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
                    .filter {
                        !it.applicationInfo.isSystemApp
                    }.map {
                        it.packageName to AppInfo(
                            it.packageName,
                            it.applicationInfo.loadLabel(pm).toString(),
                            it.applicationInfo.loadIcon(pm),
                            selectedApps.contains(it.packageName)
                        )
                    }
                appInfos.putAll(loadedAppInfos)
                val preferences = appInfos.map { (_, appInfo) ->
                    createPreference(context, appInfo)
                }.sortedBy {
                    it.title.toString()
                }
                withContext(Dispatchers.Main) {
                    preferences.forEach {
                        preferenceScreen.addPreference(it)
                    }
                }
            }
        }
    }

    private suspend fun update() {
        mutex.withLock {
            withContext(Dispatchers.Default) {
                val selectedApps = getSelectedApps(requireContext())
                appInfos.forEach { (packageName, appInfo) ->
                    appInfo.isChecked = selectedApps.contains(packageName)
                }
            }
            preferenceScreen.forEach {
                if (it is AppSwitchPreference) {
                    it.isChecked = appInfos[it.key]?.isChecked == true
                }
            }
        }
    }

    private fun createPreference(context: Context, prefAppInfo: AppInfo) =
        AppSwitchPreference(context, null).apply {
            key = prefAppInfo.packageName
            title = prefAppInfo.label
            icon = prefAppInfo.icon
            isChecked = prefAppInfo.isChecked
            setOnPreferenceChangeListener { _, newValue ->
                lifecycleScope.launch(Dispatchers.Default) {
                    val settingValue = mutex.withLock {
                        appInfos.filter { (packageName, appInfo) ->
                            if (packageName == prefAppInfo.packageName) {
                                newValue as Boolean
                            } else {
                                appInfo.isChecked
                            }
                        }.keys.joinToString(",")
                    }
                    Settings.Secure.putStringForUser(
                        context.contentResolver,
                        FORCE_FULLSCREEN_CUTOUT_APPS,
                        settingValue,
                        UserHandle.USER_CURRENT
                    )
                }
                return@setOnPreferenceChangeListener true
            }
        }

    override fun onStop() {
        context?.contentResolver?.unregisterContentObserver(settingsObserver)
        super.onStop()
    }
}

private fun getSelectedApps(context: Context): Set<String> =
    Settings.Secure.getStringForUser(
        context.contentResolver,
        FORCE_FULLSCREEN_CUTOUT_APPS,
        UserHandle.USER_CURRENT
    )?.split(",")?.toSet() ?: emptySet()

private data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    var isChecked: Boolean,
)
