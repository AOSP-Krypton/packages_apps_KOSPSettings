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

import static android.os.FileUtils.S_IRGRP;
import static android.os.FileUtils.S_IROTH;
import static android.os.FileUtils.S_IRUSR;
import static android.os.FileUtils.S_IWGRP;
import static android.os.FileUtils.S_IWUSR;
import static android.provider.Settings.System.FOD_ICON;
import static android.provider.Settings.System.FOD_ICON_TINT_COLOR;
import static android.provider.Settings.System.FOD_ICON_TINT_MODE;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.krypton.settings.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

public class FODIconFragment extends BaseFragment implements FODItemAdapter.Callback {
    private static final String TAG = "FODIconFragment";
    private static final File ICON_DIR = new File("/data/system/fod");
    private static final String[] MIME_TYPES = new String[] {
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/svg"
    };
    private final ExecutorService mExecutor;
    private final Handler mHandler;
    private final ActivityResultLauncher<String[]> mActivityResultLauncher;
    private Context mContext;
    private ArrayList<Drawable> mIconsList;
    private ImageView mPreview;
    private FODItemAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private int mInitialSize;

    public FODIconFragment() {
        mHandler = new Handler(Looper.getMainLooper());
        mExecutor = Executors.newCachedThreadPool();
        mActivityResultLauncher = registerForActivityResult(
            new OpenMultipleDocuments(), list -> loadAndCopySelectedIconsAsync(list));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
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
        final int tintMode = Utils.getSettingInt(mContext, FOD_ICON_TINT_MODE);
        if (tintMode == 2) {
            mPreview.setColorFilter(Utils.getSettingInt(mContext, FOD_ICON_TINT_COLOR, -1));
        } else if (tintMode == 1) {
            mPreview.setColorFilter(com.android.settingslib.Utils
                .getColorAccentDefaultColor(mContext));
        }
        mRecyclerView = view.findViewById(R.id.item_grid);
        mAdapter = new FODItemAdapter(mContext, FOD_ICON, R.dimen.fod_icon_button_padding);
        mAdapter.registerCallback(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new GridLayoutManager(mContext, 6, VERTICAL, false));
        loadIconsListAsync();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fod_icon_picker_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_icon) {
            if (mActivityResultLauncher != null) {
                try {
                    mActivityResultLauncher.launch(MIME_TYPES);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(mContext, R.string.cannot_resolve_activity,
                        Toast.LENGTH_LONG).show();
                }
            }
            return true;
        } else if (item.getItemId() == R.id.clear_icons) {
            if (Utils.getSettingInt(mContext, FOD_ICON) >= mInitialSize) {
                Utils.applySetting(mContext, FOD_ICON, 0);
            }
            clearAndReloadIconsAsync();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSelectedItemChanged(int newIndex) {
        if (mIconsList != null && newIndex < mIconsList.size()) {
            mPreview.setImageDrawable(mIconsList.get(
                newIndex).getConstantState().newDrawable());
        }
    }

    @Override
    public boolean onItemLongClicked(int index) {
        if (index >= mInitialSize) {
            deleteAndRenameFilesAsync(index);
            return true;
        } else {
            return false;
        }
    }

    private void loadIconsListAsync() {
        mExecutor.execute(() -> {
            // Loading built-in icons
            final TypedArray array = getResources().obtainTypedArray(R.array.config_fodIcons);
            mInitialSize = array.length();
            mIconsList = new ArrayList<>(mInitialSize);
            mAdapter.setDrawablesList(mIconsList);
            for (int i = 0; i < mInitialSize; i++) {
                mIconsList.add(array.getDrawable(i));
                mHandler.post(() -> mAdapter.notifyDataSetChanged());
            }
            array.recycle();
            // Loading user selected icons if any
            final File[] files = ICON_DIR.listFiles();
            Arrays.sort(files);
            for (File file: files) {
                try (FileInputStream in = new FileInputStream(file)) {
                    mIconsList.add(Drawable.createFromStream(in, null));
                    mHandler.post(() -> mAdapter.notifyDataSetChanged());
                } catch(IOException e) {
                    Log.e(TAG, "IOException when loading file " + file.getAbsolutePath(), e);
                }
            }
            mHandler.post(() -> onSelectedItemChanged(
                Utils.getSettingInt(mContext, FOD_ICON)));
        });
    }

    private void loadAndCopySelectedIconsAsync(final List<Uri> list) {
        if (list == null) {
            return;
        }
        mExecutor.execute(() -> {
            final ContentResolver resolver = mContext.getContentResolver();
            int currSize = mIconsList.size();
            mIconsList.ensureCapacity(currSize + list.size());
            for (Uri uri: list) {
                File out = getFileAtIndex(currSize);
                try (InputStream inStream = resolver.openInputStream(uri);
                        FileOutputStream outStream = new FileOutputStream(out)) {
                    FileUtils.copy(inStream, outStream);
                    FileUtils.setPermissions(out, S_IRUSR | S_IWUSR |
                        S_IRGRP | S_IWGRP | S_IROTH, -1, -1);
                    mIconsList.add(Drawable.createFromPath(out.getAbsolutePath()));
                    mHandler.post(() -> mAdapter.notifyDataSetChanged());
                    currSize++;
                } catch(IOException e) {
                    Log.e(TAG, "IOException when handling uri " + uri.toString(), e);
                }
            }
        });
    }

    private void deleteAndRenameFilesAsync(int index) {
        mExecutor.execute(() -> {
            final File fileAtIndex = getFileAtIndex(index);
            if (fileAtIndex.delete()) {
                if (index < (mIconsList.size() - 1)) {
                    /**
                     * Selected icon is not the last in the list,
                     * so we have to rename the rest of the files to
                     * keep it in sync with mIconsList
                     */
                    for (int i = (index + 1); i < mIconsList.size(); i++) {
                        File src = getFileAtIndex(i);
                        File dst = getFileAtIndex(i - 1);
                        src.renameTo(dst);
                    }
                }
                mIconsList.remove(index);
                int selectedIndex = Utils.getSettingInt(mContext, FOD_ICON);
                if (selectedIndex >= mInitialSize) {
                    Utils.applySetting(mContext, FOD_ICON, selectedIndex - 1);
                }
                mHandler.post(() -> {
                    mAdapter.notifyDataSetChanged();
                    onSelectedItemChanged(Utils.getSettingInt(
                        mContext, FOD_ICON));
                });
            } else {
                Log.e(TAG, "Failed to delete file " + fileAtIndex.getAbsolutePath());
            }
        });
    }

    private void clearAndReloadIconsAsync() {
        mExecutor.execute(() -> {
            final File[] files = ICON_DIR.listFiles();
            for (File file: files) {
                if (!file.delete()) {
                    Log.e(TAG, "Failed to delete file " + file.getAbsolutePath());
                }
            }
            if (Utils.getSettingInt(mContext, FOD_ICON) >= mInitialSize) {
                Utils.applySetting(mContext, FOD_ICON, 0);
            }
            for (int i = (mIconsList.size() - 1); i >= mInitialSize; i--) {
                mIconsList.remove(i);
                mHandler.post(() -> mAdapter.notifyDataSetChanged());
            }
            mHandler.post(() -> onSelectedItemChanged(
                Utils.getSettingInt(mContext, FOD_ICON)));
        });
    }

    private File getFileAtIndex(int index) {
        return new File(ICON_DIR, String.valueOf(index));
    }
}
