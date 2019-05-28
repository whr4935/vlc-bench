/*
 *****************************************************************************
 * MainPage.java
 *****************************************************************************
 * Copyright © 2016-2018 VLC authors and VideoLAN
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

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import kotlin.Unit;

public class MainPage extends VLCWorkerModel implements
        MainPageFragment.IMainPageFragment {

    private static final String TAG = MainPage.class.getName();

    private Toolbar toolbar = null;

    private int mMenuItemId = 0;
    private Fragment currentPageFragment;
    private ProgressDialog progressDialog;
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

    public void startProgressDialog() {
        ProgressDialog dialog = new ProgressDialog();
        dialog.setCancelable(false);
        dialog.setTitle(R.string.dialog_title_testing);
        dialog.setCancelCallback(this::cancelBench);
        dialog.show(getSupportFragmentManager(), "Benchmark");
        progressDialog = dialog; //TODO redundant with setDialogFragment called from the fragment
    }

    protected boolean setCurrentFragment(int itemId) {
        Fragment fragment;
        if (findViewById(R.id.main_page_fragment_holder) != null) {
            switch (itemId) {
                case R.id.home_nav:
                    fragment = new MainPageFragment();
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
                    .commit();
            mMenuItemId = itemId;
            currentPageFragment = fragment;
        }
        return true;
    }

    @Override
    protected void setupUiMembers(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main_page);

        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        bottomNavigationView = findViewById(R.id.bottom_navigation_bar);
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
            if (model.getRunning()) {
                startProgressDialog();
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
    public Unit cancelBench() {
        model.setRunning(false);
        progressDialog.dismiss();
        progressDialog = null;
        Log.i(TAG, "Benchmark was stopped by the user");
        return Unit.INSTANCE;
    }

    @Override
    protected void updateProgress(double progress, String progressText, String sampleName) {
        if (progressDialog != null) {
            progressDialog.updateProgress(progress, progressText, sampleName);
        }
    }

    @Override
    public void dismissDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("MENU_ITEM_ID", mMenuItemId);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        if (model.getRunning() && progressDialog == null) {
            startProgressDialog();
        }
        super.onResume();
    }
}
