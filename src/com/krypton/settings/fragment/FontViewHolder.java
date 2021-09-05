/*
 * Copyright (C) 2013 The OmniROM Project
 *               2021 AOSP-Krypton Project
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

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.krypton.settings.R;

public class FontViewHolder extends RecyclerView.ViewHolder {
    private final CheckBox mCheckBox;
    private final TextView mFontPreview;

    public FontViewHolder(View view) {
        super(view);
        mCheckBox = view.findViewById(R.id.checkBox);
        mFontPreview = view.findViewById(R.id.fontPreview);
    }

    public CheckBox getCheckBox() {
        return mCheckBox;
    }

    public TextView getFontPreview() {
        return mFontPreview;
    }
}
