/*****************************************************************************
 * MainPage.java
 *****************************************************************************
 * Copyright © 2016-2017 VLC authors and VideoLAN
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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.videolan.vlcbenchmark.service.BenchService;
import org.videolan.vlcbenchmark.service.FAILURE_STATES;
import org.videolan.vlcbenchmark.service.ServiceActions;
import org.videolan.vlcbenchmark.tools.DialogInstance;

public class MainPage extends VLCWorkerModel implements
        CurrentTestFragment.TestView,
        MainPageFragment.IMainPageFragment,
        MainPageDownloadFragment.IMainPageDownloadFragment,
        SettingsFragment.ISettingsFragment {

    private static final String TAG = MainPage.class.getName();

    private Toolbar toolbar = null;

    /**
     * hasDownloaded is used to see what to display
     * between MainPageDownloadFragment and MainPageFragment
     */
    private boolean hasDownloaded = false;
    private boolean hasChecked = false;
    private int mMenuItemId = 0;
    private CurrentTestFragment currentTestFragment = null;
    private BottomNavigationView bottomNavigationView = null;

    /* TV input handling */
    private int navigationIndex;
    private final static int[] navigationIds = {R.id.home_nav, R.id.results_nav, R.id.settings_nav};

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
        setCurrentFragment(R.id.home_nav);
    }

    /**
     * After successful download, removes download dialog and sets MainPageFragment
     * @param hasDownloaded boolean on download success
     */
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
                case R.id.spinner_layout:
                    fragment = new SpinnerFragment();
                    toolbar.setTitle(R.string.app_name);
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
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_bar);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        return setCurrentFragment(item.getItemId());
                    }
                }
        );
        if (savedInstanceState == null) {
            setCurrentFragment(R.id.spinner_layout);
        } else {
            mMenuItemId = savedInstanceState.getInt("MENU_ITEM_ID");
            bottomNavigationView.setSelectedItemId(mMenuItemId);
            if (running) {
                startCurrentTestFragment();
            }
        }
    }

    /**
     * dispatchKeyEvent is an override to integrate the bottomNavigationView in
     * the input flow on AndroidTV and allow to interact with it. It isn't handled natively.
     * @param event remote control key event
     * @return boolean event consumed
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        View focus = getCurrentFocus();
        boolean ret = super.dispatchKeyEvent(event);
        if (focus == null) {
            Log.e(TAG, "Failed to get current focus");
            return ret;
        }
        /* When bottom_navigation_bar gets focus, we use the inputs left and right to move
        * inside an array of fragments id. These fragments are the three principal home, results, settings,
        * updating the current fragment as we go along.
        */
        if (event.getAction() == KeyEvent.ACTION_UP && focus.getId() == R.id.bottom_navigation_bar) {
            bottomNavigationView.setItemBackgroundResource(R.drawable.bottom_navigation_view_item_background_tv);
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    if (navigationIndex + 1>= 0 && navigationIndex + 1 < navigationIds.length) {
                        navigationIndex += 1;
                    }
                    bottomNavigationView.setSelectedItemId(navigationIds[navigationIndex]);
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    if (navigationIndex - 1 >= 0 && navigationIndex - 1 < navigationIds.length) {
                        navigationIndex -= 1;
                    }
                    bottomNavigationView.setSelectedItemId(navigationIds[navigationIndex]);
                    break;
                default:
                    break;
            }
        } else if (focus.getId() != R.id.bottom_navigation_bar) {
            bottomNavigationView.setItemBackgroundResource(R.drawable.bottom_navigation_view_item_background);
        }
        return ret;
    }

    /**
     * Sets the running boolean to false, indicating to the UI that the current test dialog
     * is no longer needed.
     */
    public void cancelBench() {
        running = false;
        Log.i(TAG, "Benchmark was stopped by the user");
    }

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
        new AlertDialog.Builder(this).setTitle(R.string.dialog_text_download_error).setMessage(exception.getMessage()).setNeutralButton(R.string.dialog_btn_ok, null).show();
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
    public void dismissDialog() {
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
}
