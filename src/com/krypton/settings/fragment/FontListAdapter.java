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
import android.content.FontInfo;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class FontListAdapter extends Adapter<FontViewHolder> {
    private static final String TAG = "FontListAdapter";
    private static final boolean DEBUG = true;
    private final Context mContext;
    private final String mDefaultFont;
    private final List<Integer> mCheckedIndices;
    private Callback mCallback;
    private List<FontInfo> mFontList;
    private String mFont;

    public FontListAdapter(Context context, String def) {
        mContext = context;
        mDefaultFont = mFont = def;
        mCheckedIndices = new ArrayList<>();
        logD("default font = " + mDefaultFont);
    }

    @Override
    public FontViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new FontViewHolder(LayoutInflater.from(mContext).inflate(
            R.layout.font_preview_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(final FontViewHolder viewHolder, final int position) {
        logD("onBindViewHolder, position = " + position);
        logD("mCheckedIndices = " + mCheckedIndices);
        final View item = viewHolder.itemView;
        final FontInfo fontInfo = mFontList.get(position);
        final View.OnClickListener clickListener = v -> {
            logD("onClick, position = " + position);
            // One or more items are checked in the list
            if (!mCheckedIndices.isEmpty()) {
                // First item should not be checkable
                if (position == 0) {
                    return;
                }
                boolean checked = false;
                // Clicked view is already selected, deselect it
                if (item.isSelected()) {
                    checked = false;
                    notifyItemChanged(position);
                    mCheckedIndices.remove(new Integer(position));
                    // Update entire list to reveal checkboxes
                    if (mCheckedIndices.isEmpty()) {
                        notifyDataSetChanged();
                    }
                } else {
                    checked = true;
                    logD("onClick, checking item at position = " + position);
                    mCheckedIndices.add(position);
                    notifyItemChanged(position);
                }
                if (mCallback != null) {
                    mCallback.onItemChecked(fontInfo.fontName, checked);
                }
            } else {
                // Deselect previous one and select the current one
                final int index = mFontList.indexOf(mFont);
                logD("index of item = " + index);
                notifyItemChanged(index == -1 ? 0 : index);
                mFont = fontInfo.fontName;
                notifyItemChanged(position);
                if (mCallback != null) {
                    mCallback.onItemClicked(fontInfo);
                }
            }
        };
        final View.OnLongClickListener longClickListener = v -> {
            logD("onLongClick, position = " + position);
            // First item should not be checkable
            if (position == 0) {
                return false;
            }
            // No items are checked, select the current one
            if (mCheckedIndices.isEmpty()) {
                mCheckedIndices.add(position);
                logD("onLongClick, checking item at position = " + position);
                item.setSelected(true);
                notifyDataSetChanged();
                if (mCallback != null) {
                    mCallback.onItemChecked(fontInfo.fontName, true);
                }
                return true;
            }
            return false;
        };
        logD("selected font = " + mFont);
        logD("fontInfo name = " + fontInfo.fontName);
        item.setSelected(mCheckedIndices.contains(position));
        item.setOnClickListener(clickListener);
        item.setOnLongClickListener(longClickListener);
        viewHolder.getFontPreview().setText(fontInfo.fontName);
        viewHolder.getFontPreview().setTypeface(Typeface.createFromFile(fontInfo.fontPath));
        // If any item is checked, hide the checkbox
        if (!mCheckedIndices.isEmpty()) {
            viewHolder.getCheckBox().setVisibility(View.INVISIBLE);
        } else {
            viewHolder.getCheckBox().setChecked(mFont.equals(fontInfo.fontName));
            viewHolder.getCheckBox().setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return mFontList == null ? 0 : mFontList.size();
    }

    public void setFontList(List<FontInfo> list) {
        mFontList = list;
    }

    public void registerCallback(Callback callback) {
        mCallback = callback;
    }

    public void unregisterCallback() {
        mCallback = null;
    }

    public void onItemsRemoved(final List<String> list) {
        logD("onItemsRemoved, list = " + list);
        mCheckedIndices.clear();
        if (list != null && list.contains(mFont)) {
            mFont = mDefaultFont;
        }
    }

    private static void logD(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }

    interface Callback {
        void onItemClicked(FontInfo fontInfo);
        void onItemChecked(String font, boolean isChecked);
    }
}
