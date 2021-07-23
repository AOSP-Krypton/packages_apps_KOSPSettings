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

import static android.provider.Settings.System.FOD_ICON;
import static android.provider.Settings.System.FOD_ICON_TINT_COLOR;
import static android.provider.Settings.System.FOD_ICON_TINT_MODE;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.krypton.settings.Utils;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FODIconFragment extends BaseFragment implements FODItemAdapter.Callback {
    private Context mContext;
    private ExecutorService mExecutor;
    private Handler mHandler;
    private ImageView mPreview;
    private FODItemAdapter mAdapter;
    private RecyclerView mRecyclerView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        mExecutor = Executors.newSingleThreadExecutor();
        getActivity().setTitle(R.string.fod_item_picker_title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fod_icon_picker_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mPreview = view.findViewById(R.id.preview);
        final int setting = Utils.getSettingInt(mContext, FOD_ICON_TINT_MODE);
        if (setting == 2) {
            mPreview.setColorFilter(Utils.getSettingInt(mContext, FOD_ICON_TINT_COLOR, -1));
        } else if (setting == 1) {
            mPreview.setColorFilter(com.android.settingslib.Utils
                .getColorAccentDefaultColor(mContext));
        }
        onSelectedItemChanged(Utils.getSettingInt(mContext, FOD_ICON));
        mRecyclerView = view.findViewById(R.id.item_grid);
        mAdapter = new FODItemAdapter(mContext, FOD_ICON, R.dimen.fod_icon_button_padding);
        mAdapter.registerCallback(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new GridLayoutManager(mContext, 5, VERTICAL, false));
        loadIconsListAsync();
    }

    @Override
    public void onStop() {
        mExecutor.shutdownNow();
        super.onStop();
    }

    @Override
    public void onSelectedItemChanged(int newIndex) {
        final TypedArray array = getResources().obtainTypedArray(R.array.config_fodIcons);
        mPreview.setImageDrawable(array.getDrawable(newIndex));
        array.recycle();
    }

    private void loadIconsListAsync() {
        mExecutor.execute(() -> {
            final TypedArray array = getResources().obtainTypedArray(R.array.config_fodIcons);
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
}
