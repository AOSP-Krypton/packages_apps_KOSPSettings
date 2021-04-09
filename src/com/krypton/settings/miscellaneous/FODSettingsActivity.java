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
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Point;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.android.settings.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FODSettingsActivity extends Activity {

    private ContentResolver mResolver;
    private ImageView mFodIconPreview, mFodAnimPreview;
    private AnimationDrawable mAnimationDrawable;
    private ExecutorService mExecutor;
    private Handler mHandler;
    private Resources mResources;
    private LinearLayout mFodIconsGrid, mFodAnimsGrid;
    private HorizontalScrollView mFodAnimsContainer;
    private boolean updatedAnim = false;
    private int width, columnCount;
    private int strokeWidth;
    private int cyan, black;

    private Runnable mStopAnimationRunnable = () -> {
        if (mAnimationDrawable != null) {
            mAnimationDrawable.stop();
            mFodAnimPreview.setBackground(null);
            updatedAnim = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mExecutor = Executors.newFixedThreadPool(2);
        mHandler = new Handler(Looper.getMainLooper());
        setContentView(R.layout.fod_settings_layout);
        updateLayout();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateLayout() {
        Point size = new Point();
        ActionBar actionBar;
        boolean animsEnabled;

        mResources = getResources();
        mResolver = getContentResolver();

        animsEnabled = getInt(2) == 1;

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(mResources.getString(R.string.fod_settings_title));

        black = mResources.getColor(R.color.color_black);
        cyan = mResources.getColor(R.color.color_cyan);

        mFodIconPreview = (ImageView) findViewById(R.id.fod_icon_preview);
        mFodAnimPreview = (ImageView) findViewById(R.id.fod_anim_preview);
        mFodIconPreview.setScaleType(ScaleType.CENTER_INSIDE);
        mFodAnimPreview.setScaleType(ScaleType.CENTER_INSIDE);

        ((Switch) findViewById(R.id.fod_animation_switch)).setChecked(animsEnabled);

        mFodIconsGrid = (LinearLayout) findViewById(R.id.fod_icons_preview);
        mFodAnimsGrid = (LinearLayout) findViewById(R.id.fod_anims_preview);
        mFodAnimsContainer = (HorizontalScrollView) findViewById(R.id.fod_anims_container);
        if (!animsEnabled) mFodAnimsContainer.removeView(mFodAnimsGrid);
        columnCount = mResources.getInteger(R.integer.config_fodSettingsColumns);
        getDisplay().getRealSize(size);
        width = (size.x / columnCount);

        strokeWidth = mResources.getDimensionPixelSize(R.dimen.stroke_width);

        mExecutor.execute(() ->
            setGrid(com.krypton.settings.R.array.config_fodIcons, 0));
        mExecutor.execute(() ->
            setGrid(com.krypton.settings.R.array.config_fodAnimPreviews, 1));
    }

    private void setGrid(int arrayId, int type) {
        int padding = mResources.getDimensionPixelSize(type == 0 ?
            R.dimen.button_padding : R.dimen.button_padding_anim);
        TypedArray array = mResources.obtainTypedArray(arrayId);
        LinearLayout grid = type == 0 ? mFodIconsGrid : mFodAnimsGrid;
        int savedIndex = getInt(type);
        for (int i = 0; i < array.length(); i++) {
            ImageButton button = new ImageButton(this);
            Drawable img = array.getDrawable(i);
            boolean enabled = (i == savedIndex);
            setButtonPressed(button, enabled);
            if (enabled && type == 0) {
                setFodIcon(img);
            }
            button.setImageDrawable(img);
            button.setScaleType(ScaleType.CENTER_CROP);
            button.setPaddingRelative(padding, padding, padding, padding);
            button.setOnClickListener((v) -> {
                int prevIndex = getInt(type);
                int currIndex = grid.indexOfChild(v);
                if (currIndex != prevIndex) {
                    putInt(type, currIndex);
                    setButtonPressed(v, true);
                    setButtonPressed(grid.getChildAt(prevIndex), false);
                }
                if (type == 0) {
                    setFodIcon(((ImageButton) v).getDrawable());
                } else {
                    setFodAnim(grid.indexOfChild(v));
                }
            });
            mHandler.post(() -> grid.addView(button, width, width));
        }
        array.recycle();
    }

    private void setFodIcon(Drawable icon) {
        mHandler.post(() -> {
            mFodIconPreview.setImageDrawable(icon);
        });
    }

    private void setFodAnim(int pos) {
        if (mHandler.hasCallbacks(mStopAnimationRunnable)) {
            mHandler.removeCallbacks(mStopAnimationRunnable);
            mHandler.post(mStopAnimationRunnable);
        }

        TypedArray array = mResources.obtainTypedArray(com.krypton.settings.R.array.config_fodAnims);
        mHandler.post(() -> {
            mFodAnimPreview.setBackgroundResource(array.getResourceId(pos, 0));
            mAnimationDrawable = (AnimationDrawable) mFodAnimPreview.getBackground();
            updatedAnim = true;
        });

        mExecutor.execute(() -> {
            while (!updatedAnim) {
                try {
                    Thread.sleep(1);
                } catch (Exception e) {}
            }

            int dur = 0;
            for (int k = 0; k < mAnimationDrawable.getNumberOfFrames(); k++) {
                dur += mAnimationDrawable.getDuration(k);
            }

            mHandler.post(() -> mAnimationDrawable.start());
            mHandler.postDelayed(mStopAnimationRunnable, 5*dur);
        });
    }

    public void toggleFodAnim(View view) {
        boolean checked = ((Switch) view).isChecked();
        putInt(2, checked ? 1 : 0);
        if (checked) {
            mFodAnimsContainer.addView(mFodAnimsGrid);
        } else if (mFodAnimsContainer.indexOfChild(mFodAnimsGrid) != -1) {
            mFodAnimsContainer.removeView(mFodAnimsGrid);
        }
    }

    private int getInt(int type) {
        return Settings.System.getInt(mResolver, getKey(type), 0);
    }

    private void putInt(int type, int value) {
        Settings.System.putInt(mResolver, getKey(type), value);
    }

    private String getKey(int type) {
        switch (type) {
            case 0:
                return Settings.System.FOD_ICON;
            case 1:
                return Settings.System.FOD_ANIM;
            case 2:
                return Settings.System.FOD_RECOGNIZING_ANIMATION;
        }
        return null;
    }

    private void setButtonPressed(View button, boolean pressed) {
        GradientDrawable bg = (GradientDrawable) mResources.getDrawable(R.drawable.rounded_rectangle, null);
        bg.setStroke(strokeWidth, pressed ? cyan : black);
        ((ImageButton) button).setBackground(bg);
    }
}
