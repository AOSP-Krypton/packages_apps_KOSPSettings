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

import static android.provider.Settings.System.FOD_ANIM;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.krypton.settings.Utils;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FODAnimFragment extends BaseFragment implements FODItemAdapter.Callback {
    private Context mContext;
    private ExecutorService mExecutor;
    private Handler mHandler;
    private ImageView mPreview;
    private ProgressBar mLoadingProgressBar;
    private FODItemAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private ArrayList<Drawable> mAnimsList;
    private AnimationDrawable mCurrAnimation;
    private boolean mIsLoading;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mExecutor = Executors.newFixedThreadPool(2);
        mHandler = new Handler(Looper.getMainLooper());
        loadAnimsAsync();
        getActivity().setTitle(R.string.fod_item_picker_title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fod_anim_picker_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mPreview = view.findViewById(R.id.preview);
        mLoadingProgressBar = view.findViewById(R.id.progress_bar);
        mRecyclerView = view.findViewById(R.id.item_grid);
        mAdapter = new FODItemAdapter(mContext, FOD_ANIM, R.dimen.fod_anim_button_padding);
        mAdapter.registerCallback(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new GridLayoutManager(mContext, 5, VERTICAL, false));
        loadAnimPreviewsAsync();
    }

    @Override
    public void onStop() {
        if (mCurrAnimation != null) {
            mCurrAnimation.stop();
        }
        mExecutor.shutdownNow();
        super.onStop();
    }

    @Override
    public void onSelectedItemChanged(int newIndex) {
        if (mIsLoading) {
            showLoadingToast();
        } else {
            if (mCurrAnimation != null) {
                mCurrAnimation.stop();
                mPreview.setImageDrawable(null);
            }
            mPreview.setImageDrawable(mAnimsList.get(newIndex));
            mCurrAnimation = (AnimationDrawable) mPreview.getDrawable();
            mCurrAnimation.start();
        }
    }

    private void loadAnimsAsync() {
        mExecutor.execute(() -> {
            mIsLoading = true;
            final TypedArray array = getResources().obtainTypedArray(R.array.config_fodAnims);
            final int size = array.length();
            mAnimsList = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                if (mLoadingProgressBar != null) {
                    final int progress = ((i +1) * 100) / size;
                    mHandler.post(() -> mLoadingProgressBar.setProgress(progress));
                }
                mAnimsList.add(array.getDrawable(i));
            }
            array.recycle();
            mHandler.post(() -> {
                mLoadingProgressBar.setVisibility(View.GONE);
                onSelectedItemChanged(Utils.getSettingInt(mContext, FOD_ANIM));
            });
            mIsLoading = false;
        });
    }

    private void loadAnimPreviewsAsync() {
        mExecutor.execute(() -> {
            final TypedArray array = getResources().obtainTypedArray(R.array.config_fodAnimPreviews);
            final int size = array.length();
            final ArrayList<Drawable> list = new ArrayList<>(size);
            mAdapter.setDrawablesList(list);
            for (int i = 0; i < size; i++) {
                list.add(array.getDrawable(i));
                mHandler.post(() -> mAdapter.notifyDataSetChanged());
            }
            array.recycle();
        });
        mExecutor.shutdown();
    }

    private void showLoadingToast() {
        Toast.makeText(mContext, R.string.fod_anim_loading_text, Toast.LENGTH_LONG).show();
    }
}
