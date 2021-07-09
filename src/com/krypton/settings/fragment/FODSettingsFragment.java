/*
 * Copyright (C) 2021 AOSP-Krypton Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use mContext file except in compliance with the License.
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

package com.krypton.settings.fragment;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.provider.Settings.System.FOD_ANIM;
import static android.provider.Settings.System.FOD_ANIM_ALWAYS_ON;
import static android.provider.Settings.System.FOD_ICON;
import static android.provider.Settings.System.FOD_RECOGNIZING_ANIMATION;
import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.krypton.settings.Utils;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FODSettingsFragment extends Fragment
        implements FODItemAdapter.SelectedItemChangeListener {
    private Context mContext;
    private ImageView mFODIconPreview, mFODAnimPreview;
    private AnimationDrawable mAnimationDrawable;
    private ExecutorService mExecutor;
    private Handler mHandler;
    private RecyclerView mFODIconsRecyclerView, mFODAnimsRecyclerView;
    private FODItemAdapter mFODIconsAdapter, mFODAnimsAdapter;
    private Switch mFODAnimSwitch, mFODAnimAlwaysOnSwitch;
    private ArrayList<Drawable> mAnimationList;
    private boolean mIsLoading;

    private final Runnable mStopAnimationRunnable = () -> {
        if (mAnimationDrawable != null) {
            mFODAnimPreview.clearAnimation();
            mAnimationDrawable.stop();
            mFODAnimPreview.setImageDrawable(null);
            mAnimationDrawable = null;
        }
    };

    public FODSettingsFragment() {
        super(R.layout.fod_settings_layout);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        mExecutor = Executors.newFixedThreadPool(3);
        loadFODAnimsAsync();
        final FragmentActivity activity = getActivity();
        activity.setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        activity.getActionBar().setTitle(R.string.fod_settings_title);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mFODIconPreview = (ImageView) view.findViewById(R.id.fod_icon_preview);
        previewFODIcon(Utils.getSettingInt(mContext, FOD_ICON));
        mFODAnimPreview = (ImageView) view.findViewById(R.id.fod_anim_preview);

        mFODAnimSwitch = (Switch) view.findViewById(R.id.fod_animation_switch);
        mFODAnimAlwaysOnSwitch = (Switch) view.findViewById(R.id.fod_animation_always_on_switch);

        mFODIconsRecyclerView = (RecyclerView) view.findViewById(R.id.fod_icons_recyclerview);
        mFODIconsAdapter = new FODItemAdapter(mContext, FOD_ICON, R.dimen.fod_icon_button_padding);
        mFODIconsAdapter.registerSelectedItemChangedListener(this);
        mFODIconsRecyclerView.setAdapter(mFODIconsAdapter);
        mFODIconsRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, HORIZONTAL, false));

        mFODAnimsRecyclerView = (RecyclerView) view.findViewById(R.id.fod_anims_recyclerview);
        mFODAnimsAdapter = new FODItemAdapter(mContext, FOD_ANIM, R.dimen.fod_anim_button_padding);
        mFODAnimsAdapter.registerSelectedItemChangedListener(this);
        mFODAnimsRecyclerView.setAdapter(mFODAnimsAdapter);
        mFODAnimsRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, HORIZONTAL, false));

        if (Utils.getSettingBoolean(mContext, FOD_RECOGNIZING_ANIMATION)) {
            mFODAnimSwitch.setChecked(true);
        } else {
            mFODAnimAlwaysOnSwitch.setEnabled(false);
            mFODAnimsRecyclerView.setVisibility(View.GONE);
        }
        if (Utils.getSettingBoolean(mContext, FOD_ANIM_ALWAYS_ON)) {
            mFODAnimAlwaysOnSwitch.setChecked(true);
        }

        loadFODResourcesAsync(com.krypton.settings.R.array.config_fodIcons, mFODIconsAdapter);
        loadFODResourcesAsync(com.krypton.settings.R.array.config_fodAnimPreviews, mFODAnimsAdapter);
    }

    @Override
    public void onStop() {
        mFODIconsAdapter.unregisterSelectedItemChangedListener();
        mExecutor.shutdownNow();
        super.onStop();
    }

    @Override
    public void onSelectedItemChanged(String key, int newIndex) {
        if (key.equals(FOD_ICON)) {
            previewFODIcon(newIndex);
        } else if (key.equals(FOD_ANIM)) {
            previewFODAnim(newIndex);
        }
    }

    private void loadFODResourcesAsync(int resId, FODItemAdapter adapter) {
        mExecutor.execute(() -> {
            final TypedArray array = getResources().obtainTypedArray(resId);
            final int size = array.length();
            final ArrayList<Drawable> list = new ArrayList<>(size);
            adapter.setDrawablesList(list);
            for (int i = 0; i < size; i++) {
                list.add(array.getDrawable(i));
                mHandler.post(() -> adapter.notifyDataSetChanged());
            }
            array.recycle();
        });
    }

    private void loadFODAnimsAsync() {
        mExecutor.execute(() -> {
            mIsLoading = true;
            final TypedArray array = getResources().obtainTypedArray(com.krypton.settings.R.array.config_fodAnims);
            final int size = array.length();
            mAnimationList = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                mAnimationList.add(array.getDrawable(i));
            }
            array.recycle();
            mIsLoading = false;
        });
    }

    private void previewFODIcon(int index) {
        final TypedArray array = getResources().obtainTypedArray(com.krypton.settings.R.array.config_fodIcons);
        mFODIconPreview.setImageDrawable(array.getDrawable(index));
        array.recycle();
    }

    private void previewFODAnim(int index) {
        if (mIsLoading) {
            Toast.makeText(mContext, R.string.loading_animations, Toast.LENGTH_LONG).show();
            return;
        }
        if (mHandler.hasCallbacks(mStopAnimationRunnable)) {
            mHandler.removeCallbacks(mStopAnimationRunnable);
            mStopAnimationRunnable.run();
        }
        mFODAnimPreview.setImageDrawable(mAnimationList.get(index));
        mAnimationDrawable = (AnimationDrawable) mFODAnimPreview.getDrawable();
        // Assuming equal frame durations
        int dur = mAnimationDrawable.getDuration(0) * mAnimationDrawable.getNumberOfFrames();
        mAnimationDrawable.start();
        mHandler.postDelayed(mStopAnimationRunnable, 5 * dur);
    }

    public void toggleFODAnim(View view) {
        boolean checked = ((Switch) view).isChecked();
        Utils.applySetting(mContext, FOD_RECOGNIZING_ANIMATION, checked);
        mFODAnimsRecyclerView.setVisibility(checked ? View.VISIBLE : View.GONE);
        mFODAnimAlwaysOnSwitch.setEnabled(checked);
    }

    public void setFODAnimationAlwaysOn(View view) {
        Utils.applySetting(mContext, FOD_ANIM_ALWAYS_ON, mFODAnimAlwaysOnSwitch.isChecked());
    }
}
