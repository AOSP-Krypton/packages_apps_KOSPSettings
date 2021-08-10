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
import android.content.om.IOverlayManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.graphics.Color;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.settings.R;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class MiscellaneousSettingsFragment extends BaseFragment implements OnPreferenceChangeListener {
    private static final String PREF_RGB_ACCENT_PICKER = "rgb_accent_picker";
    private ColorPickerPreference rgbAccentPicker;
    private Context mContext;
    private IOverlayManager mOverlayManager;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.miscellaneous_settings);
        mContext = getContext();
        mOverlayManager = IOverlayManager.Stub.asInterface(
            ServiceManager.getService(Context.OVERLAY_SERVICE));
        rgbAccentPicker = (ColorPickerPreference) findPreference(PREF_RGB_ACCENT_PICKER);
        String colorVal = Settings.Secure.getStringForUser(mContext.getContentResolver(),
                Settings.Secure.ACCENT_COLOR, UserHandle.USER_CURRENT);
        int color = (colorVal == null)
                ? Color.WHITE
                : Color.parseColor("#" + colorVal);
        rgbAccentPicker.setNewPreviewColor(color);
        rgbAccentPicker.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == rgbAccentPicker) {
            int color = (Integer) objValue;
            String hexColor = String.format("%08X", (0xFFFFFFFF & color));
            Settings.Secure.putStringForUser(mContext.getContentResolver(),
                        Settings.Secure.ACCENT_COLOR,
                        hexColor, UserHandle.USER_CURRENT);
            try {
                 mOverlayManager.reloadAssets("com.android.settings", UserHandle.USER_CURRENT);
                 mOverlayManager.reloadAssets("com.android.systemui", UserHandle.USER_CURRENT);
             } catch (RemoteException ignored) {
             }
            return true;
        }
        return false;
    }
}
