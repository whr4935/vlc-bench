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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;

import org.json.JSONException;
import org.videolan.vlcbenchmark.service.BenchServiceDispatcher;
import org.videolan.vlcbenchmark.service.FAILURE_STATES;
import org.videolan.vlcbenchmark.tools.JsonHandler;
import org.videolan.vlcbenchmark.tools.TestInfo;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by noeldu_b on 7/11/16.
 */
public class MainPage extends VLCWorkerModel implements
        CurrentTestFragment.TestView,
        MainPageFragment.IMainPageFragment,
        MainPageDownloadFragment.IMainPageDownloadFragment {

    private TextView percentText = null;
    private TextView textLog = null;
    private ProgressBar progressBar = null;

    private Toolbar toolbar = null;
    private BottomNavigationView bottomNavigationView = null;

    private static final String PROGRESS_TEXT_FORMAT = "%.2f %% | file %d/%d | test %d";
    private static final String PROGRESS_TEXT_FORMAT_LOOPS = PROGRESS_TEXT_FORMAT + " | loop %d/%d";

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
        if (fragment != null) {
            View view = currentTestFragment.getView();
            progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
            textLog = (TextView) view.findViewById(R.id.current_sample);
            percentText = (TextView) view.findViewById(R.id.percentText);
            if (currentTestFragment.getTag().equals("Download dialog")) {
                textLog.setText("Downloading ...");
            } else if (currentTestFragment.getTag().equals("Current test")) {
                textLog.setText("Testing ...");
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

        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_bar);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.home_nav:
                                if (findViewById(R.id.main_page_fragment_holder) != null) {
                                    Fragment fragment;
                                    if (hasDownloaded) {
                                        fragment = new MainPageFragment();
                                    } else {
                                        fragment = new MainPageDownloadFragment();
                                    }
                                    getSupportActionBar().setTitle("VLC Benchmark");
                                    getSupportFragmentManager().beginTransaction()
                                            .replace(R.id.main_page_fragment_holder, fragment)
                                            .addToBackStack(null)
                                            .commit();
                                }
                                break;
                            case R.id.results_nav:
                                if (findViewById(R.id.main_page_fragment_holder) != null) {
                                    MainPageResultListFragment fragment = new MainPageResultListFragment();
                                    getSupportActionBar().setTitle("Results");
                                    getSupportFragmentManager().beginTransaction()
                                            .replace(R.id.main_page_fragment_holder, fragment)
                                            .addToBackStack(null)
                                            .commit();
                                }
                                break;
                            case R.id.settings_nav:
                                if (findViewById(R.id.main_page_fragment_holder) != null) {
                                    SettingsFragment fragment = new SettingsFragment();
                                    getSupportActionBar().setTitle("Settings");
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
            progressBar.setProgress((int) Math.round(percent));
            if (bitRate == BenchServiceDispatcher.NO_BITRATE)
                percentText.setText(String.format("%.2f %%", percent));
            else
                percentText.setText(String.format("%.2f %% (%s)", percent, bitRateToString(bitRate)));
        }
    }

    private String bitRateToString(long bitRate) {
        if (bitRate <= 0)
            return "0 bps";

        double powOf10 = Math.round(Math.log10(bitRate));

        if (powOf10 < 3)
            return String.format("%l bps", bitRate);
        else if (powOf10 >= 3 && powOf10 < 6)
            return String.format("%.2f kbps", bitRate / 1_000d);
        else if (powOf10 >= 6 && powOf10 < 9)
            return String.format("%.2f mbps", bitRate / 1_000_000d);
        return String.format("%.2f gbps", bitRate / 1_000_000_000d);
    }

    @Override
    public void failure(FAILURE_STATES reason, Exception exception) {
        if (currentTestFragment != null ) {
            currentTestFragment.dismiss();
        }//todo find some way not to spawn the dialog if no wifi
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
                percentText.setText(String.format(PROGRESS_TEXT_FORMAT_LOOPS, progressBar.getProgress() * 100.0 / progressBar.getMax(), fileIndex, numberOfFiles, testNumber,
                        loopNumber, numberOfLoops));
            else
                percentText.setText(String.format(PROGRESS_TEXT_FORMAT, progressBar.getProgress() * 100.0 / progressBar.getMax(), fileIndex, numberOfFiles, testNumber));
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
            startActivityForResult(intent, RequestCodes.RESULTS);
        } catch (JSONException e) {
            Log.e("VLCBenchmark", "Failed to save test : " + e.toString());
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
