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
 * limitations under the License
 */

package com.krypton.settings.miscellaneous;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Point;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;

import com.android.settings.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FODIconPicker extends Activity {

    private ContentResolver mResolver;
    private GridLayout mGridLayout;
    private GradientDrawable bgGrey;
    private GradientDrawable bgCyan;
    private ExecutorService mExecutor;
    private Handler mHandler;
    private int columnCount, rowCount;
    private int len;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mExecutor = Executors.newSingleThreadExecutor();
        mHandler = new Handler(Looper.getMainLooper());
        createLayout();
        setContentView(mGridLayout);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createLayout() {
        int width, padding;
        Point size = new Point();
        ActionBar actionBar;
        ScaleType center = ScaleType.CENTER_CROP;
        Resources res;
        TypedArray array;

        res = getResources();
        mResolver = getContentResolver();
        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(res.getString(R.string.fod_icon_settings_page_title));
        padding = res.getDimensionPixelSize(R.dimen.default_button_padding);
        array = res.obtainTypedArray(R.array.config_fodIcons);
        bgGrey = (GradientDrawable) res.getDrawable(R.drawable.rounded_rectangle_grey_border, null);
        bgCyan = (GradientDrawable) res.getDrawable(R.drawable.rounded_rectangle_cyan_border, null);
        columnCount = res.getInteger(R.integer.config_fodIconPickerColumns);
        len = array.length();
        mGridLayout = new GridLayout(this);
        mGridLayout.setColumnCount(columnCount);
        rowCount = (len / columnCount);
        if ((len % columnCount) != 0) {
            rowCount = (len / columnCount) + 1;
        }
        mGridLayout.setRowCount(rowCount);
        getDisplay().getRealSize(size);
        width = (size.x / columnCount);

        mExecutor.execute(() -> {
            int index = 0;
            int savedIndex = getInt();
            out:
            for (int i = 0; i < rowCount; i++) {
                for (int j = 0; j < columnCount; j++) {
                    if (index == len) {
                        break out;
                    }
                    ImageButton button = new ImageButton(this);
                    setButtonPressed(button, index == savedIndex);
                    button.setImageDrawable(array.getDrawable(index));
                    button.setScaleType(center);
                    button.setPaddingRelative(padding, padding, padding, padding);
                    button.setOnClickListener((View view) -> {
                        int prevIndex = getInt();
                        int currIndex = mGridLayout.indexOfChild(view);
                        if (currIndex != prevIndex) {
                            putInt(currIndex);
                            setButtonPressed(view, true);
                            setButtonPressed(mGridLayout.getChildAt(prevIndex), false);
                        }
                    });
                    mHandler.post(() -> mGridLayout.addView(button, width, width));
                    index++;
                }
            }
        });
    }

    private int getInt() {
        return Settings.System.getInt(mResolver, Settings.System.FOD_ICON, 0);
    }

    private void putInt(int value) {
        Settings.System.putInt(mResolver, Settings.System.FOD_ICON, value);
    }

    private void setButtonPressed(View button, boolean pressed) {
        ((ImageButton) button).setBackground(pressed ? bgCyan : bgGrey);
    }
}
