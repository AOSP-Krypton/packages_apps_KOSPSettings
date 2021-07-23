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

import static android.provider.Settings.System.GAMINGMODE_APPS;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.krypton.settings.Utils;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

public class GamingModeAppsFragment extends BaseFragment {
    private Context mContext;
    private ExecutorService mExecutor;
    private Handler mHandler;
    private PackageManager mPM;
    private RecyclerView mRecyclerView;
    private GamingModeAppsListAdapter mAdapter;
    private SearchView mSearchView;
    private List<AppInfo> mSortedAppsList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        mPM = mContext.getPackageManager();
        mHandler = new Handler(Looper.getMainLooper());
        mExecutor = Executors.newFixedThreadPool(2);
        setHasOptionsMenu(true);
        getActivity().setTitle(R.string.select_apps_title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.gamingmode_apps_picker, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mRecyclerView = view.findViewById(R.id.gamingmode_apps_recyclerview);
        mAdapter = new GamingModeAppsListAdapter(mContext);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        loadAppsListAsync(mPM.getInstalledPackages(0));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.gamingmode_apps_menu, menu);
        MenuItem item = menu.findItem(R.id.search_apps_menu);
        mSearchView = (SearchView) item.getActionView();
        mSearchView.setQueryHint(getString(R.string.string_search_app));
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterAppsAsync(newText);
                return true;
            }
        });
    }

    @Override
    public void onStop() {
        mExecutor.shutdownNow();
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.reset_button) {
            Utils.putStringInSettings(mContext, GAMINGMODE_APPS, "");
            mAdapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadAppsListAsync(final List<PackageInfo> list) {
        mExecutor.execute(() -> {
            list.sort((pInfo1, pInfo2) -> compare(pInfo1.applicationInfo.loadLabel(mPM),
                pInfo2.applicationInfo.loadLabel(mPM)));
            mSortedAppsList = new ArrayList<>(list.size() / 2);
            mAdapter.updateList(mSortedAppsList);
            for (PackageInfo pInfo: list) {
                if (!pInfo.applicationInfo.isSystemApp()) {
                    mSortedAppsList.add(new AppInfo(
                        pInfo.applicationInfo.loadIcon(mPM),
                        pInfo.applicationInfo.loadLabel(mPM),
                        pInfo.packageName));
                    mHandler.post(() -> mAdapter.notifyDataSetChanged());
                }
            }
        });
    }

    private void filterAppsAsync(CharSequence query) {
        mExecutor.execute(() -> {
            final ArrayList<AppInfo> newList = new ArrayList<>();
            for (AppInfo appInfo: mSortedAppsList) {
                if (appInfo.filter(query)) {
                    newList.add(appInfo);
                }
            }
            mAdapter.updateList(newList);
            mHandler.post(() -> mAdapter.notifyDataSetChanged());
        });
    }

    private int compare(CharSequence label1, CharSequence label2) {
        return label1.toString().compareToIgnoreCase(label2.toString());
    }
}
