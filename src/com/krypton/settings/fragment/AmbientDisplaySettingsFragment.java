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
import android.os.UserHandle;
import android.provider.Settings;

import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;

import com.android.settings.R;

import com.krypton.settings.preference.SettingColorPickerPreference;

public class AmbientDisplaySettingsFragment extends BaseFragment {

    private static final String TAG = "AmbientDisplaySettingsFragment";
    private static final String AOD_SCHEDULE_KEY = "always_on_display_schedule";
    private Preference mAODPref;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.ambient_display_settings);
        mAODPref = findPreference(AOD_SCHEDULE_KEY);
        updateAlwaysOnSummary();
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof SettingColorPickerPreference) {
            String[] attrs = ((SettingColorPickerPreference) preference).getSettingAttrs();
            final DialogFragment fragment = new ColorPickerFragment(attrs[0], attrs[1], Integer.parseInt(attrs[2]));
            fragment.setTargetFragment(this, 0);
            fragment.show(getParentFragmentManager(), TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAlwaysOnSummary();
    }

    private void updateAlwaysOnSummary() {
        if (mAODPref == null) return;
        int mode = Settings.Secure.getIntForUser(getActivity().getContentResolver(),
                Settings.Secure.DOZE_ALWAYS_ON_AUTO_MODE, 0, UserHandle.USER_CURRENT);
        switch (mode) {
            case 0:
                mAODPref.setSummary(R.string.disabled);
                break;
            case 1:
                mAODPref.setSummary(R.string.night_display_auto_mode_twilight);
                break;
            case 2:
                mAODPref.setSummary(R.string.night_display_auto_mode_custom);
                break;
        }
    }
}
