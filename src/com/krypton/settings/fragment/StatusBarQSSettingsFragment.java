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
package com.krypton.settings.fragment;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;

import com.android.settings.R;
import com.android.settings.Utils;
import com.krypton.settings.preference.SettingEditTextPreference;

public class StatusBarQSSettingsFragment extends BaseFragment {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.statusbar_qs_settings);
        Context context = getContext();
        if (!Utils.isBatteryPresent(context)) {
            getPreferenceScreen().removePreferenceRecursively(
                "statusbar_battery_category_title");
        }
        SettingEditTextPreference unitTextSizePreference = (SettingEditTextPreference) findPreference(
            "statusbar_network_traffic_unit_text_size_preference");
        SettingEditTextPreference rateTextScaleFactorPreference = (SettingEditTextPreference) findPreference(
            "statusbar_network_traffic_rate_text_scale_factor_preference");
        try {
            Resources res = context.getPackageManager().getResourcesForApplication("com.android.systemui");
            int resId = res.getIdentifier("network_traffic_unit_text_default_size", "dimen", "com.android.systemui");
            if (resId != 0) {
                unitTextSizePreference.setSettingDefault((int) res.getDimension(resId));
            }
            resId = res.getIdentifier("network_traffic_rate_text_default_scale_factor", "dimen", "com.android.systemui");
            if (resId != 0) {
                TypedValue value = new TypedValue();
                res.getValue(resId, value, true);
                rateTextScaleFactorPreference.setSettingDefault((int) (value.getFloat() * 10));
            }
        } catch(NameNotFoundException e) {
            // Do nothing
        }
    }
}
