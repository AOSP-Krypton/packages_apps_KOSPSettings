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

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.development.AccentOverlayCategoryPreferenceController;
import com.android.settings.development.OverlayCategoryPreferenceController;
import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;
import com.krypton.settings.fragment.AccentPickerFragment;
import com.krypton.settings.preference.AccentPickerPreference;

import java.util.ArrayList;
import java.util.List;

public class ThemeSettingsFragment extends DashboardFragment {
    private static final String TAG = "ThemeSettingsFragment";

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof AccentPickerPreference) {
            final AccentPickerFragment fragment = new AccentPickerFragment();
            fragment.setTargetFragment(this, 0);
            fragment.show(getParentFragmentManager(), TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.KRYPTON;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.theme_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>(3);
        controllers.add(new AccentOverlayCategoryPreferenceController(context,
                "android.theme.customization.accent_color"));
        controllers.add(new OverlayCategoryPreferenceController(context,
                "android.theme.customization.adaptive_icon_shape"));
        controllers.add(new OverlayCategoryPreferenceController(context,
                "android.theme.customization.icon_pack"));
        return controllers;
    }
}
