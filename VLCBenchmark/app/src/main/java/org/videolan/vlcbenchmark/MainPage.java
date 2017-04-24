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
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;

import org.json.JSONException;
import org.videolan.vlcbenchmark.service.BenchService;
import org.videolan.vlcbenchmark.service.BenchServiceDispatcher;
import org.videolan.vlcbenchmark.service.FAILURE_STATES;
import org.videolan.vlcbenchmark.service.ServiceActions;
import org.videolan.vlcbenchmark.tools.DialogInstance;
import org.videolan.vlcbenchmark.tools.FormatStr;
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

    private TextView percentText = null;
    private TextView textLog = null;
    private ProgressBar progressBar = null;

    private Toolbar toolbar = null;

    private boolean hasDownloaded = false;
    private boolean hasChecked = false;
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
        if (hasDownloaded) {
            Fragment fragment = new MainPageFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_page_fragment_holder, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            Fragment fragment = new MainPageDownloadFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_page_fragment_holder, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    public void setDialogFragment(CurrentTestFragment fragment) {
        currentTestFragment = fragment;
        if (currentTestFragment != null && currentTestFragment.getView() != null) {
            View view = currentTestFragment.getView();
            progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
            textLog = (TextView) view.findViewById(R.id.current_sample);
            percentText = (TextView) view.findViewById(R.id.percentText);
            if (currentTestFragment.getTag().equals("Download dialog")) {
                textLog.setText(getResources().getString(R.string.dialog_text_downloading));
            } else if (currentTestFragment.getTag().equals("Current test")) {
                textLog.setText(getResources().getString(R.string.dialog_text_testing));
            }
        } else {
            progressBar = null;
            textLog = null;
            percentText = null;
        }
    }

    public boolean getHasChecked() {
        return hasChecked;
    }

    @Override
    protected void setupUiMembers(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main_page);

        Intent intent = new Intent(this, BenchService.class);
        intent.putExtra("action", ServiceActions.SERVICE_CHECKFILES);
        this.startService(intent);

        BottomNavigationView bottomNavigationView;
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_bar);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.home_nav:
                                setUpHomeFragment();
                                break;
                            case R.id.results_nav:
                                if (findViewById(R.id.main_page_fragment_holder) != null) {
                                    MainPageResultListFragment fragment = new MainPageResultListFragment();
                                    toolbar.setTitle(getResources().getString(R.string.results_page));
                                    getSupportFragmentManager().beginTransaction()
                                            .replace(R.id.main_page_fragment_holder, fragment)
                                            .addToBackStack(null)
                                            .commit();
                                }
                                break;
                            case R.id.settings_nav:
                                if (findViewById(R.id.main_page_fragment_holder) != null) {
                                    SettingsFragment fragment = new SettingsFragment();
                                    toolbar.setTitle(getResources().getString(R.string.settings_page));
                                    getSupportFragmentManager().beginTransaction()
                                            .replace(R.id.main_page_fragment_holder, fragment)
                                            .addToBackStack(null)
                                            .commit();
                                }
                                break;
                        }
                        return true;
                    }
                }
        );
        setUpHomeFragment();
    }

    private void setUpHomeFragment() {
        if (findViewById(R.id.main_page_fragment_holder) != null) {
            Fragment fragment;
            if (hasDownloaded) {
                fragment = new MainPageFragment();
            } else {
                fragment = new MainPageDownloadFragment();
            }
            toolbar.setTitle(getResources().getString(R.string.app_name));
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_page_fragment_holder, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    protected void resetUiToDefault() {
        if (currentTestFragment != null) {
            progressBar.setProgress(0);
            progressBar.setMax(100);
            percentText.setText(R.string.default_percent_value);
            textLog.setText("");
        }
    }

    public void cancelBench() {
        running = false;
    }

    @Override
    protected void updateUiOnServiceDone() {
    }

    @Override
    public void stepFinished(String message) {
    }

    //todo percent of bar relative to all test, not just specific loop;
    @Override
    public void updatePercent(double percent, long bitRate) {
        if (currentTestFragment != null) {
            String strPercent;
            progressBar.setProgress((int) Math.round(percent));
            if (bitRate == BenchServiceDispatcher.NO_BITRATE) {
                strPercent = FormatStr.format2Dec(percent) + "%%";
                percentText.setText(strPercent);
            }
            else {
                strPercent = FormatStr.format2Dec(percent) + "%% (" + bitRateToString(bitRate) + ")";
                percentText.setText(strPercent);
            }
        }
    }

    public void displayDialog(DialogInstance dialog) {
        dialog.display(this);
    }

    public void resetDownload() {
        hasDownloaded = false;
        hasChecked = false;
    }

    private String bitRateToString(long bitRate) {
        if (bitRate <= 0)
            return "0 bps";

        double powOf10 = Math.round(Math.log10(bitRate));

        if (powOf10 < 3)
            return FormatStr.format2Dec(bitRate) + "bps";
        else if (powOf10 >= 3 && powOf10 < 6)
            return FormatStr.format2Dec(bitRate / 1_000d) + "kbps";
        else if (powOf10 >= 6 && powOf10 < 9)
            return FormatStr.format2Dec(bitRate / 1_000_000d) + "mbps";
        return FormatStr.format2Dec(bitRate / 1_000_000_000d) + "gbps";
    }

    @Override
    public void failure(FAILURE_STATES reason, Exception exception) {
        if (currentTestFragment != null ) {
            currentTestFragment.dismiss();
        }
        new AlertDialog.Builder(this).setTitle("Error during download").setMessage(exception.getMessage()).setNeutralButton("ok", null).show();
    }

    @Override
    protected void initVlcProgress(int totalNumberOfElements) {
        if (currentTestFragment != null) {
            progressBar.setProgress(0);
            progressBar.setMax(totalNumberOfElements);
            progressBar.setMax(totalNumberOfElements);
        }
    }

    @Override
    protected void onFileTestStarted(String fileName) {
        if (currentTestFragment != null) {
            textLog.setText(fileName);
        }
    }

    @Override
    protected void onSingleTestFinished(String testName, boolean succeeded, int fileIndex, int numberOfFiles, int testNumber, int loopNumber, int numberOfLoops) {
        if (currentTestFragment != null) {
            progressBar.incrementProgressBy(1);
            if (numberOfLoops != 1)
                percentText.setText(String.format(
                        getResources().getString(R.string.progress_text_format_loop),
                        progressBar.getProgress() * 100.0 / progressBar.getMax(), fileIndex,
                        numberOfFiles, testNumber, loopNumber, numberOfLoops));
            else
                percentText.setText(
                        String.format(getResources().getString(R.string.progress_text_format),
                                progressBar.getProgress() * 100.0 / progressBar.getMax(), fileIndex,
                                numberOfFiles, testNumber));
        }
    }

    @Override
    protected void onVlcCrashed(String errorMessage, final Runnable continueTesting) {
    }

    @Override
    protected void onTestsFinished(List<TestInfo>[] results) {
        ArrayList<TestInfo> testResult = TestInfo.mergeTests(results);
        String name;
        if(currentTestFragment != null) {
            currentTestFragment.dismiss();
        }
        try {
            name = JsonHandler.save((testResult));
            Intent intent = new Intent(MainPage.this, ResultPage.class);
            intent.putExtra("name", name);
            running = false;
            startActivityForResult(intent, RequestCodes.RESULTS);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to save test : " + e.toString());
        }
    }

    @Override
    public void doneDownload() {
        if (currentTestFragment != null) {
            currentTestFragment.dismiss();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.RESULTS) {
            resetUiToDefault();
        }
    }

    @Override
    protected void onSaveUiData(Bundle saveInstanceState) {
    }

    @Override
    protected void onRestoreUiData(Bundle saveInstanceState) {
        super.onRestoreInstanceState(saveInstanceState);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
