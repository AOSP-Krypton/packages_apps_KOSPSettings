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

    static final int MODE_DISABLED = 0;
    static final int MODE_NIGHT = 1;
    static final int MODE_TIME = 2;
    static final int MODE_MIXED_SUNSET = 3;
    static final int MODE_MIXED_SUNRISE = 4;

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
            default:
            case MODE_DISABLED:
                mAODPref.setSummary(R.string.disabled);
                break;
            case MODE_NIGHT:
                mAODPref.setSummary(R.string.night_display_auto_mode_twilight);
                break;
            case MODE_TIME:
                mAODPref.setSummary(R.string.night_display_auto_mode_custom);
                break;
            case MODE_MIXED_SUNSET:
                mAODPref.setSummary(R.string.always_on_display_schedule_mixed_sunset);
                break;
            case MODE_MIXED_SUNRISE:
                mAODPref.setSummary(R.string.always_on_display_schedule_mixed_sunrise);
                break;
        }
    }
}
