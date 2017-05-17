/*****************************************************************************
 * VideoPlayerActivity.java
 *****************************************************************************
 * Copyright © 2011-2014 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.vlcbenchmark;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;

import org.json.JSONException;
import org.videolan.vlcbenchmark.service.BenchService;
import org.videolan.vlcbenchmark.service.FAILURE_STATES;
import org.videolan.vlcbenchmark.service.ServiceActions;
import org.videolan.vlcbenchmark.tools.DialogInstance;
import org.videolan.vlcbenchmark.tools.JsonHandler;
import org.videolan.vlcbenchmark.tools.TestInfo;

import java.util.ArrayList;
import java.util.List;

public class MainPage extends VLCWorkerModel implements
        CurrentTestFragment.TestView,
        MainPageFragment.IMainPageFragment,
        MainPageDownloadFragment.IMainPageDownloadFragment,
        SettingsFragment.ISettingsFragment {

    private static final String TAG = MainPage.class.getName();

    private Toolbar toolbar = null;

    private boolean hasDownloaded = false;
    private boolean hasChecked = false;
    private int mMenuItemId = 0;
    private CurrentTestFragment currentTestFragment = null;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    public void setFilesChecked(boolean hasChecked) {
        this.hasChecked = hasChecked;
        if (!hasChecked) {
            setFilesDownloaded(false);
        }
    }

    public void setFilesDownloaded(boolean hasDownloaded) {
        this.hasDownloaded = hasDownloaded;
        if (currentTestFragment != null) {
            currentTestFragment.dismiss();
        }
        setCurrentFragment(R.id.home_nav);
    }

    public void setDialogFragment(CurrentTestFragment fragment) {
        currentTestFragment = fragment;
    }

    public void startCurrentTestFragment() {
        CurrentTestFragment fragment = new CurrentTestFragment( );
        fragment.setCancelable(false);
        fragment.show(getSupportFragmentManager(), "Current test");
        currentTestFragment = fragment; //TODO redundant with setDialogFragment called from the fragment
    }

    public boolean getHasChecked() {
        return hasChecked;
    }

    private boolean setCurrentFragment(int itemId) {
        Fragment fragment;
        if (findViewById(R.id.main_page_fragment_holder) != null) {
            switch (itemId) {
                case R.id.home_nav:
                    if (hasDownloaded) {
                        fragment = new MainPageFragment();
                    } else {
                        fragment = new MainPageDownloadFragment();
                    }
                    toolbar.setTitle(getResources().getString(R.string.app_name));
                    break;
                case R.id.results_nav:
                    fragment = new MainPageResultListFragment();
                    toolbar.setTitle(getResources().getString(R.string.results_page));
                    break;
                case R.id.settings_nav:
                    fragment = new SettingsFragment();
                    toolbar.setTitle(getResources().getString(R.string.settings_page));
                    break;
                default:
                    return false;
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_page_fragment_holder, fragment)
                    .addToBackStack(null)
                    .commit();
            mMenuItemId = itemId;
        }
        return true;
    }

    @Override
    protected void setupUiMembers(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main_page);
        if (savedInstanceState == null) {
            Intent intent = new Intent(this, BenchService.class);
            intent.putExtra("action", ServiceActions.SERVICE_CHECKFILES);
            this.startService(intent);
        } else {
            hasDownloaded = savedInstanceState.getBoolean("HAS_DOWNLOADED");
            hasChecked = savedInstanceState.getBoolean("HAS_CHECKED");
        }

        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_bar);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        return setCurrentFragment(item.getItemId());
                    }
                }
        );
        if (savedInstanceState == null) {
            setCurrentFragment(R.id.home_nav);
        } else {
            mMenuItemId = savedInstanceState.getInt("MENU_ITEM_ID");
            bottomNavigationView.setSelectedItemId(mMenuItemId);
            setCurrentFragment(mMenuItemId);
            if (running) {
                startCurrentTestFragment();
            }
        }
    }

    public void cancelBench() {
        running = false;
        Log.i(TAG, "Benchmark was stopped by the user");
    }

    //todo percent of bar relative to all test, not just specific loop;
    @Override
    public void updatePercent(double percent, long bitRate) {
        if (currentTestFragment != null) {
            currentTestFragment.updatePercent(percent, bitRate);
        }
    }

    public void displayDialog(DialogInstance dialog) {
        dialog.display(this);
    }

    public void resetDownload() {
        hasDownloaded = false;
        hasChecked = false;
    }

    @Override
    public void failure(FAILURE_STATES reason, Exception exception) {
        if (currentTestFragment != null ) {
            currentTestFragment.dismiss();
        }
        new AlertDialog.Builder(this).setTitle("Error during download").setMessage(exception.getMessage()).setNeutralButton("ok", null).show();
    }

    @Override
    protected void updateTestProgress(String testName, int fileIndex, int numberOfFiles, int testNumber, int loopNumber, int numberOfLoops) {
        if (currentTestFragment != null) {
            currentTestFragment.updateTestProgress(testName, fileIndex, numberOfFiles, testNumber, loopNumber, numberOfLoops);
        }
    }

    @Override
    protected void onVlcCrashed(String errorMessage, final Runnable continueTesting) {
    }

    @Override //TODO change to dismissDialog
    public void doneDownload() {
        if (currentTestFragment != null) {
            currentTestFragment.dismiss();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("MENU_ITEM_ID", mMenuItemId);
        savedInstanceState.putBoolean("HAS_DOWNLOADED", hasDownloaded);
        savedInstanceState.putBoolean("HAD_CHECKED", hasChecked);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onStart() {
        if (running && currentTestFragment == null) {
            startCurrentTestFragment();
        }
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    //TO handle

    @Override //BenchService method
    public void stepFinished(String message) {
    }
}
