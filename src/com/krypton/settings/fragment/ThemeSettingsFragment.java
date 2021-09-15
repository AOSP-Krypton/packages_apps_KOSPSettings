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

import android.os.Bundle;

import androidx.preference.Preference;

import com.android.settings.R;
import com.krypton.settings.fragment.AccentPickerFragment;
import com.krypton.settings.preference.AccentPickerPreference;

public class ThemeSettingsFragment extends BaseFragment {
    private static final String TAG = "ThemeSettingsFragment";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.theme_settings);
    }

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
}
