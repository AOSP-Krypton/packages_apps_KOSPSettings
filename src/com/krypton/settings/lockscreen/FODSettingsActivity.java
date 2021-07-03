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

package com.krypton.settings.lockscreen;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.provider.Settings.System.FOD_ANIM;
import static android.provider.Settings.System.FOD_ANIM_ALWAYS_ON;
import static android.provider.Settings.System.FOD_ICON;
import static android.provider.Settings.System.FOD_RECOGNIZING_ANIMATION;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
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
import com.krypton.settings.Utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FODSettingsActivity extends Activity {
    private ImageView mFODIconPreview, mFODAnimPreview;
    private AnimationDrawable mAnimationDrawable;
    private ExecutorService mExecutor;
    private Handler mHandler;
    private LinearLayout mFODIconsGrid, mFODAnimsGrid;
    private HorizontalScrollView mFODIconsContainer, mFODAnimsContainer;
    private Switch mFODAnimSwitch, mFODAnimAlwaysOnSwitch;
    private int width, columnCount;
    private int strokeWidth, cyan, black;

    private final Runnable mStopAnimationRunnable = () -> {
        if (mAnimationDrawable != null) {
            mAnimationDrawable.stop();
            mFODAnimPreview.setBackground(null);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        mExecutor = Executors.newCachedThreadPool();
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
        final Resources res = getResources();
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(res.getString(R.string.fod_settings_title));

        mFODIconPreview = (ImageView) findViewById(R.id.fod_icon_preview);
        mFODIconPreview.setScaleType(ScaleType.CENTER_INSIDE);
        mFODAnimPreview = (ImageView) findViewById(R.id.fod_anim_preview);
        mFODAnimPreview.setScaleType(ScaleType.CENTER_INSIDE);

        mFODAnimSwitch = (Switch) findViewById(R.id.fod_animation_switch);
        mFODAnimAlwaysOnSwitch = (Switch) findViewById(R.id.fod_animation_always_on_switch);

        mFODIconsGrid = (LinearLayout) findViewById(R.id.fod_icons_preview);
        mFODAnimsGrid = (LinearLayout) findViewById(R.id.fod_anims_preview);
        mFODIconsContainer = (HorizontalScrollView) findViewById(R.id.fod_icons_container);
        mFODAnimsContainer = (HorizontalScrollView) findViewById(R.id.fod_anims_container);

        if (Utils.getSettingBoolean(this, Utils.TYPE_SYSTEM, FOD_RECOGNIZING_ANIMATION)) {
            mFODAnimSwitch.setChecked(true);
        } else {
            mFODAnimAlwaysOnSwitch.setEnabled(false);
            mFODAnimsContainer.removeView(mFODAnimsGrid);
        }

        final Point size = new Point();
        columnCount = res.getInteger(R.integer.config_fodSettingsColumns);
        getDisplay().getRealSize(size);
        width = (size.x / columnCount);

        strokeWidth = res.getDimensionPixelSize(R.dimen.stroke_width);
        black = res.getColor(R.color.color_black);
        cyan = res.getColor(R.color.color_cyan);

        final Future iconsSet = mExecutor.submit(() -> setFODIconGrid());
        final Future animsSet = mExecutor.submit(() -> setFODAnimGrid());
        mExecutor.execute(() -> {
            while (!(iconsSet.isDone() && animsSet.isDone())) {
                Utils.sleepThread(10);
            }
            int iconIndex = Utils.getSettingInt(this, Utils.TYPE_SYSTEM, FOD_ICON);
            if (iconIndex > columnCount) {
                mHandler.post(() -> mFODIconsContainer.smoothScrollTo(iconIndex * width, 0));
            }
            int animIndex = Utils.getSettingInt(this, Utils.TYPE_SYSTEM, FOD_ANIM);
            if (animIndex > columnCount) {
                mHandler.post(() -> mFODAnimsContainer.smoothScrollTo(animIndex * width, 0));
            }
        });
    }

    private void setFODIconGrid() {
        int padding = getResources().getDimensionPixelSize(R.dimen.button_padding);
        final TypedArray array = getResources().obtainTypedArray(com.krypton.settings.R.array.config_fodIcons);
        for (int i = 0; i < array.length(); i++) {
            FODIconButton button = new FODIconButton(i, padding, array.getDrawable(i));
            mHandler.post(() -> mFODIconsGrid.addView(button, width, width));
        }
        array.recycle();
    }

    private void setFODAnimGrid() {
        int padding = getResources().getDimensionPixelSize(R.dimen.button_padding_anim);
        final TypedArray array = getResources().obtainTypedArray(com.krypton.settings.R.array.config_fodAnimPreviews);
        for (int i = 0; i < array.length(); i++) {
            FODAnimButton button = new FODAnimButton(i, padding, array.getDrawable(i));
            mHandler.post(() -> mFODAnimsGrid.addView(button, width, width));
        }
        array.recycle();
    }

    private void previewFODAnim(int index) {
        if (mHandler.hasCallbacks(mStopAnimationRunnable)) {
            mHandler.removeCallbacks(mStopAnimationRunnable);
            mHandler.post(mStopAnimationRunnable);
        }
        TypedArray array = getResources().obtainTypedArray(com.krypton.settings.R.array.config_fodAnims);
        mFODAnimPreview.setBackgroundResource(array.getResourceId(index, 0));
        array.recycle();
        mAnimationDrawable = (AnimationDrawable) mFODAnimPreview.getBackground();
        mExecutor.execute(() -> {
            // Assuming equal frame durations
            int dur = mAnimationDrawable.getDuration(0) * mAnimationDrawable.getNumberOfFrames();
            mHandler.post(() -> mAnimationDrawable.start());
            mHandler.postDelayed(mStopAnimationRunnable, 5 * dur);
        });
    }

    public void toggleFODAnim(View view) {
        boolean checked = ((Switch) view).isChecked();
        Utils.applySetting(this, Utils.TYPE_SYSTEM, FOD_RECOGNIZING_ANIMATION, checked);
        if (checked) {
            mFODAnimsContainer.addView(mFODAnimsGrid);
        } else if (mFODAnimsContainer.indexOfChild(mFODAnimsGrid) != -1) {
            mFODAnimsContainer.removeView(mFODAnimsGrid);
        }
        mFODAnimAlwaysOnSwitch.setEnabled(checked);
    }

    public void setFODAnimationAlwaysOn(View view) {
        Utils.applySetting(this, Utils.TYPE_SYSTEM,
            FOD_ANIM_ALWAYS_ON, mFODAnimAlwaysOnSwitch.isChecked());
    }

    private abstract class CustomButton extends ImageButton {
        final Context mContext = FODSettingsActivity.this;
        final int mIndex;

        CustomButton(int index, int padding, Drawable drawable) {
            super(FODSettingsActivity.this);
            mIndex = index;
            setPadding(padding, padding, padding, padding);
            setImageDrawable(drawable);
            setScaleType(ScaleType.CENTER_CROP);
        }

        void setChecked(boolean checked) {
            GradientDrawable bg = (GradientDrawable) mContext.getResources().getDrawable(R.drawable.rounded_rectangle, null);
            bg.setStroke(strokeWidth, checked ? cyan : black);
            setBackground(bg);
        }

        boolean onSelected() {
            int prevIndex = Utils.getSettingInt(mContext, Utils.TYPE_SYSTEM, getKey());
            if (mIndex != prevIndex) {
                setChecked(true);
                return true;
            }
            return false;
        }

        abstract String getKey();
    }

    private final class FODIconButton extends CustomButton {

        FODIconButton(int index, int padding, Drawable drawable) {
            super(index, padding, drawable);
            setOnClickListener(v -> onSelected());
            boolean isSelected = mIndex == Utils.getSettingInt(mContext, Utils.TYPE_SYSTEM, getKey());
            setChecked(isSelected);
            if (isSelected) {
                mHandler.post(() -> mFODIconPreview.setImageDrawable(getDrawable()));
            }
        }

        @Override
        String getKey() {
            return FOD_ICON;
        }

        @Override
        boolean onSelected() {
            if (super.onSelected()) {
                int prevIndex = Utils.getSettingInt(mContext, Utils.TYPE_SYSTEM, getKey());
                mFODIconPreview.setImageDrawable(getDrawable());
                ((CustomButton) mFODIconsGrid.getChildAt(prevIndex)).setChecked(false);
                Utils.applySetting(mContext, Utils.TYPE_SYSTEM, getKey(), mIndex);
            }
            return true;
        }
    }

    private final class FODAnimButton extends CustomButton {

        FODAnimButton(int index, int padding, Drawable drawable) {
            super(index, padding, drawable);
            setOnClickListener(v -> onSelected());
            setChecked(mIndex == Utils.getSettingInt(mContext,
                Utils.TYPE_SYSTEM, getKey()));
        }

        @Override
        String getKey() {
            return FOD_ANIM;
        }

        @Override
        boolean onSelected() {
            if (super.onSelected()) {
                int prevIndex = Utils.getSettingInt(mContext, Utils.TYPE_SYSTEM, getKey());
                ((CustomButton) mFODAnimsGrid.getChildAt(prevIndex)).setChecked(false);
                Utils.applySetting(mContext, Utils.TYPE_SYSTEM, getKey(), mIndex);
            }
            previewFODAnim(mIndex);
            return true;
        }
    }
}
