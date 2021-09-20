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
import static android.os.FileUtils.S_IRWXU;
import static android.os.ParcelFileDescriptor.MODE_READ_WRITE;
import static android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.FontInfo;
import android.content.IFontService;
import android.content.IFontServiceCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.ttf.TTFParser;

public class FontPickerFragment extends BaseFragment implements FontListAdapter.Callback {
    private static final String TAG = "FontPickerFragment";
    private static final boolean DEBUG = false;
    private static final String[] FONT_MIME_TYPE = new String[] {"font/ttf"};
    private static final File sCacheDir = new File(Environment.getDataDirectory(), "cache/font_cache");
    private static final int sMenuItemAddFontId = 1; 
    private static final int sMenuItemDeleteFontId = 2; 
    private final ActivityResultLauncher<String[]> mActivityResultLauncher;
    private final Handler mHandler;
    private final IFontService mFontService;
    private final TTFParser mTTFParser;
    private final List<FontInfo> mFontList;
    private final List<String> mFontsToRemove;
    private Context mContext;
    private FontListAdapter mAdapter;
    private Menu mMenu;
    private AlertDialog mCopyingFontsDialog, mDeletingFontsDialog;

    private final IFontServiceCallback mCallback = new IFontServiceCallback.Stub() {
        @Override
        public void onFontsAdded(final List<FontInfo> list) {
            logD("onFontsAdded");
            mHandler.post(() -> {
                if (list != null) {
                    // Remove the default font to add it back
                    // to the top later after sorting the list
                    mFontList.remove(0);
                    list.stream()
                        .filter(fontInfo -> !mFontList.contains(fontInfo))
                        .forEach(fontInfo -> mFontList.add(fontInfo));
                    Collections.sort(mFontList);
                    mFontList.add(0, FontInfo.getDefaultFontInfo());
                    mAdapter.notifyDataSetChanged();
                    if (!sCacheDir.delete()) {
                        Log.e(TAG, "Failed to delete cache dir " + sCacheDir.getAbsolutePath());
                    }
                }
                dismissCopyingFontsDialog();
                // Unregister callback since it does not serve any purpose
                try {
                    logD("unregistering callback");
                    mFontService.unregisterCallback(this);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to unregister callback", e);
                }
            });
        }

        @Override
        public void onFontsRemoved(final List<FontInfo> list) {
            logD("onFontsRemoved");
            mHandler.post(() -> {
                if (list != null) {
                    list.forEach(fontInfo -> mFontList.remove(fontInfo));
                    mAdapter.onItemsRemoved(list.stream()
                        .map(fontInfo -> fontInfo.fontName)
                        .collect(Collectors.toList()));
                    mAdapter.notifyDataSetChanged();
                }
                mFontsToRemove.clear();
                showAddMenuItem();
                dismissDeletingFontsDialog();
                // Unregister callback since it does not serve any purpose
                try {
                    logD("unregistering callback");
                    mFontService.unregisterCallback(this);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to unregister callback", e);
                }
            });
        }
    };

    public FontPickerFragment() {
        mHandler = new Handler(Looper.getMainLooper());
        mFontService = IFontService.Stub.asInterface(
                ServiceManager.getService("dufont"));
        mActivityResultLauncher = registerForActivityResult(
            new OpenMultipleDocuments(), list -> transferSelectedFonts(list));
        mTTFParser = new TTFParser();
        mFontList = new ArrayList<>();
        mFontsToRemove = new ArrayList<>();
        loadFonts();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        getActivity().setTitle(R.string.font_picker_title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.font_picker_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        logD("onViewCreated");
        FontInfo fontInfo = FontInfo.getDefaultFontInfo();
        try {
            fontInfo = mFontService.getFontInfo();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException when querying current font info", e);
            toast(R.string.unable_to_get_current_font);
        }
        mAdapter = new FontListAdapter(mContext, fontInfo.fontName, mFontList);
        mAdapter.registerCallback(this);
        final RecyclerView recyclerView = view.findViewById(R.id.fontList);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mMenu = menu;
        showAddMenuItem();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case sMenuItemAddFontId:
                if (mActivityResultLauncher != null) {
                    try {
                        mActivityResultLauncher.launch(FONT_MIME_TYPE);
                    } catch (ActivityNotFoundException e) {
                        toast(R.string.cannot_resolve_activity);
                    }
                }
                return true;
            case sMenuItemDeleteFontId:
                if (!mFontsToRemove.isEmpty()) {
                    try {
                        mFontService.registerCallback(mCallback);
                        mFontService.removeFonts(mFontsToRemove);
                        showDeletingFontsDialog();
                    } catch (RemoteException e) {
                        Log.e(TAG, "RemoteException while removing fonts", e);
                        dismissDeletingFontsDialog();
                        toast(R.string.unable_to_remove_fonts);
                    }
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClicked(final FontInfo fontInfo) {
        try {
            logD("applying font " + fontInfo);
            mFontService.applyFont(fontInfo);
            toast(R.string.reboot_to_apply_font);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException when applying font " + fontInfo, e);
            toast(R.string.unable_to_apply_font);
        }
    }

    @Override
    public void onItemChecked(String font, boolean isChecked) {
        if (isChecked) {
            mFontsToRemove.add(font);
        } else {
            mFontsToRemove.remove(font);
        }
        final int size = mFontsToRemove.size();
        if (size == 0) {
            showAddMenuItem();
        } else if (size == 1) {
            showDeleteMenuItem();
        }
    }

    private void showAddMenuItem() {
        mMenu.clear();
        mMenu.add(Menu.NONE, sMenuItemAddFontId, Menu.NONE, null)
            .setTitle(R.string.add_fonts)
            .setIcon(R.drawable.ic_add_24dp)
            .setShowAsAction(SHOW_AS_ACTION_IF_ROOM);
    }

    private void showDeleteMenuItem() {
        mMenu.clear();
        mMenu.add(Menu.NONE, sMenuItemDeleteFontId, Menu.NONE, null)
            .setTitle(R.string.delete_fonts)
            .setIcon(R.drawable.ic_delete)
            .setShowAsAction(SHOW_AS_ACTION_IF_ROOM);
    }

    private void loadFonts() {
        try {
            final Map<String, FontInfo> map = mFontService.getAllFonts();
            logD("loadFonts, map = " + map);
            mFontList.addAll(map.values());
            Collections.sort(mFontList);
            logD("loadFonts, list = " + mFontList);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException upon fetching fonts from service", e);
        } finally {
            mFontList.add(0, FontInfo.getDefaultFontInfo());
        }
    }

    private void transferSelectedFonts(final List<Uri> list) {
        if (list == null) {
            toast(R.string.no_fonts_to_add);
            return;
        }
        logD("transferSelectedFonts, list = " + list);
        if (!makeFontCacheDir()) {
            Log.e(TAG, "Unable to create font cache dir " + sCacheDir.getAbsolutePath());
            toast(R.string.unable_to_create_cache);
            return;
        }
        try {
            logD("registering callback " + mCallback);
            mFontService.registerCallback(mCallback);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register callback", e);
        }
        showCopyingFontsDialog();
        final ContentResolver contentResolver = mContext.getContentResolver();
        final Map<String, ParcelFileDescriptor> map = new HashMap<>(list.size());
        list.forEach(uri -> {
            try {
                InputStream inStream = contentResolver.openInputStream(uri);
                final TrueTypeFont ttf = mTTFParser.parse(inStream);
                inStream.close();
                final String font = ttf.getName();
                final File ttfCache = new File(sCacheDir, font.concat(".ttf"));
                inStream = contentResolver.openInputStream(uri);
                final FileOutputStream outStream = new FileOutputStream(ttfCache);
                FileUtils.copy(inStream, outStream);
                FileUtils.setPermissions(ttfCache, S_IRWXU | S_IRGRP, -1, -1);
                inStream.close();
                outStream.close();
                map.put(font, ParcelFileDescriptor.open(ttfCache, MODE_READ_WRITE));
            } catch(IOException e) {
                Log.e(TAG, "IOException when handling uri " + uri.toString(), e);
                toast(R.string.unable_to_add_fonts);
            }
        });
        logD("transferSelectedFonts, map = " + map);
        if (!map.isEmpty()) {
            try {
                mFontService.addFonts(map);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException upon adding fonts", e);
                dismissCopyingFontsDialog();
                toast(R.string.unable_to_add_fonts);
            }
        } else {
            dismissCopyingFontsDialog();
        }
    }

    private void showCopyingFontsDialog() {
        logD("showCopyingFontsDialog");
        if (mCopyingFontsDialog == null) {
            mCopyingFontsDialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.copying_fonts)
                .setMessage(R.string.do_not_close)
                .setCancelable(false)
                .setView(LayoutInflater.from(mContext).inflate(
                    R.layout.progress_layout, null, false))
                .show();
        } else {
            mCopyingFontsDialog.show();
        }
    }

    private void dismissCopyingFontsDialog() {
        logD("dismissCopyingFontsDialog");
        if (mCopyingFontsDialog != null) {
            mCopyingFontsDialog.dismiss();
        }
    }

    private void showDeletingFontsDialog() {
        logD("showDeletingFontsDialog");
        if (mDeletingFontsDialog == null) {
            mDeletingFontsDialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.deleting_fonts)
                .setMessage(R.string.do_not_close)
                .setCancelable(false)
                .setView(LayoutInflater.from(mContext).inflate(
                    R.layout.progress_layout, null, false))
                .show();
        } else {
            mDeletingFontsDialog.show();
        }
    }

    private void dismissDeletingFontsDialog() {
        logD("dismissDeletingFontsDialog");
        if (mDeletingFontsDialog != null) {
            mDeletingFontsDialog.dismiss();
        }
    }

    private boolean makeFontCacheDir() {
        boolean dirExist = sCacheDir.isDirectory();
        if (!dirExist) {
            dirExist = sCacheDir.mkdirs();
        }
        return dirExist && FileUtils.setPermissions(sCacheDir,
            S_IRWXU | S_IRGRP, -1, -1) == 0;
    }

    private void toast(int msgId) {
        Toast.makeText(mContext, msgId, Toast.LENGTH_LONG).show();
    }

    private static void logD(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
}
