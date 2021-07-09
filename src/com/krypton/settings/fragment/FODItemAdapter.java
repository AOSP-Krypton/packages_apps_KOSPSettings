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
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.android.settings.R;
import com.krypton.settings.Utils;

import java.util.List;

public class FODItemAdapter extends Adapter<FODViewHolder> {
    private final Context mContext;
    private final String mSettingKey;
    private final int mItemPadding;
    private SelectedItemChangeListener mListener;
    private List<Drawable> mDrawablesList;
    private int mSelectedIndex;

    public FODItemAdapter(Context context, String key, int paddingResId) {
        super();
        mContext = context;
        mSettingKey = key;
        mItemPadding = mContext.getResources().getDimensionPixelSize(paddingResId);
        mSelectedIndex = Utils.getSettingInt(mContext, mSettingKey);
    }

    @Override
    public FODViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new FODViewHolder(LayoutInflater.from(mContext).inflate(
            R.layout.fod_list_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(FODViewHolder viewHolder, final int position) {
        final ImageView imageView = viewHolder.getImageView();
        imageView.setPaddingRelative(mItemPadding, mItemPadding, mItemPadding, mItemPadding);
        imageView.setImageDrawable(mDrawablesList.get(position));
        if (mSelectedIndex == position) {
            imageView.setBackgroundResource(R.drawable.btn_checked);
        } else {
            imageView.setBackground(null);
        }
        imageView.setOnClickListener(v -> {
            mSelectedIndex = position;
            if (mListener != null) {
                mListener.onSelectedItemChanged(mSettingKey, mSelectedIndex);
            }
            Utils.applySetting(mContext, mSettingKey, mSelectedIndex);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return mDrawablesList == null ? 0 : mDrawablesList.size();
    }

    public void setDrawablesList(List<Drawable> list) {
        mDrawablesList = list;
    }

    public void registerSelectedItemChangedListener(SelectedItemChangeListener listener) {
        mListener = listener;
    }

    public void unregisterSelectedItemChangedListener() {
        mListener = null;
    }

    public interface SelectedItemChangeListener {
        public void onSelectedItemChanged(String key, int newIndex);
    }
}
