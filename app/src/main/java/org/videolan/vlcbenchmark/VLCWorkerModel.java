/*
 *****************************************************************************
 * VLCWorkerModel.java
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

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import org.json.JSONException;
import org.videolan.vlcbenchmark.api.RetrofitInstance;
import org.videolan.vlcbenchmark.benchmark.BenchmarkViewModel;
import org.videolan.vlcbenchmark.benchmark.TestTypes;
import org.videolan.vlcbenchmark.tools.CrashHandler;
import org.videolan.vlcbenchmark.tools.DialogInstance;
import org.videolan.vlcbenchmark.tools.FormatStr;
import org.videolan.vlcbenchmark.tools.GoogleConnectionHandler;
import org.videolan.vlcbenchmark.tools.JsonHandler;
import org.videolan.vlcbenchmark.tools.MediaInfo;
import org.videolan.vlcbenchmark.tools.ProgressSaver;
import org.videolan.vlcbenchmark.tools.ScreenshotValidator;
import org.videolan.vlcbenchmark.tools.StorageManager;
import org.videolan.vlcbenchmark.tools.TestInfo;
import org.videolan.vlcbenchmark.tools.Util;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Main class of the project.
 * This class handle the whole logic/algorithm side of the application.
 * <p>
 * It extends from Activity yet it doesn't touch anything related to UI except {@link VLCWorkerModel#onActivityResult(int, int, Intent)}
 * and at some specific points the termination of the activity.
 * <p>
 * This class cannot be instantiated directly it requires to be extended and to implement all of its abstract method,
 * its in those methods only that the UI part of the activity can be handled.
 * <p>
 * This architecture allows the UI and logical part to be independent from one an other.
 */
public abstract class VLCWorkerModel extends AppCompatActivity {

    private final static String TAG = "VLCWorkerModel";

    private Intent mData = null;
    private int mResultCode = -2;

    protected BenchmarkViewModel model;

    private static final String BENCH_ACTIVITY = "org.videolan.vlc.gui.video.benchmark.BenchActivity";
    private static final String EXTRA_TIMESTAMPS = "extra_benchmark_timestamps";
    private static final String EXTRA_ACTION_QUALITY = "extra_benchmark_action_quality";
    private static final String EXTRA_ACTION_PLAYBACK = "extra_benchmark_action_playback";
    private static final String EXTRA_SCREENSHOT_DIR = "extra_benchmark_screenshot_dir";
    private static final String EXTRA_ACTION = "extra_benchmark_action";
    private static final String EXTRA_BENCHMARK = "extra_benchmark";
    private static final String EXTRA_HARDWARE = "extra_benchmark_disable_hardware";
    private static final String EXTRA_FROM_START = "from_start";
    private static final String SCREENSHOT_NAMING = "Screenshot_";
    private static final String SHARED_PREFERENCE = "org.videolab.vlc.gui.video.benchmark.UNCAUGHT_EXCEPTIONS";
    private static final String SHARED_PREFERENCE_STACK_TRACE = "org.videolab.vlc.gui.video.benchmark.STACK_TRACE";

    private static final String STATE_RUNNING = "STATE_RUNNING";
    private static final String STATE_TEST_FILES = "STATE_TEST_FILES";
    private static final String STATE_TEST_INDEX = "STATE_TEST_INDEX";
    private static final String STATE_FILE_INDEX = "STATE_FILE_INDEX";
    private static final String STATE_LOOP_NUMBER = "STATE_LOOP_NUMBER";
    private static final String STATE_TEST_RESULTS= "STATE_TEST_RESULTS";
    private static final String STATE_LAST_TEST_INFO = "STATE_LAST_TEST_INFO";
    private static final String STATE_LOOP_TOTAL = "STATE_LOOP_TOTAL";

    protected abstract boolean setCurrentFragment(int itemId);

    public abstract void dismissDialog();

    /**
     * Is called during the {@link VLCWorkerModel#onCreate(Bundle)}.
     */
    protected abstract void setupUiMembers(Bundle savedInstanceState);

    /**
     * Called to update the test dialog.
     * @param progress benchmark progress percentage
     * @param progressText text recaping the progress state of the benchmark:
     *                     file index, loop number, etc
     * @param sampleName the name of the test (ex : screenshot software, ...)
     */
    protected abstract void updateProgress(double progress, String progressText, String sampleName);

    /**
     * Initialization of the Activity.
     *request permissions to read on the external storage.
     *
     * @param savedInstanceState saved state bundle
     */
    @Override
    final protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CrashHandler.setCrashHandler();
        model = ViewModelProviders.of(this).get(BenchmarkViewModel.class);

        StorageManager.INSTANCE.setStoragePreference(this);
        setupUiMembers(savedInstanceState);

        RetrofitInstance.init(getString(R.string.build_api_address));
        GoogleConnectionHandler.getInstance();
    }

    /**
     * Entry point of the class.
     * Calling this method will initiate benchmark variables and then start vlc.
     *
     * @param numberOfTests number of repetition of all the tests. must be 1 or 3 other values are ignored.
     * @param previousTest testResults of a previous interrupted benchmark, or null if starting from scratch.
     */
    @UiThread
    final public void launchTests(int numberOfTests, List<TestInfo>[] previousTest) {
        if (previousTest == null) {
            if (numberOfTests == 1) {
                model.setLoopTotal(1);
                model.testResults = new ArrayList[]{new ArrayList<TestInfo>()};
            } else if (numberOfTests == 3) {
                model.setLoopTotal(3);
                model.testResults = new ArrayList[]{new ArrayList<MediaInfo>(), new ArrayList<MediaInfo>(), new ArrayList<MediaInfo>()};
            } else {
                Log.e(TAG, "Wrong number of tests to start: " + numberOfTests);
                return;
            }
            model.setFileIndex(0);
            model.setLoopNumber(0);
        } else {
            model.testResults = previousTest;
            model.setLoopTotal(model.testResults.length);
            int i = 0;
            while (i < model.getLoopTotal() && model.testResults[i].size() != 0)
                i += 1;
            model.setLoopNumber(i - 1);
            model.setFileIndex(model.testResults[model.getLoopNumber()].size());
        }
        String name = model.getTestFiles().get(model.getFileIndex()).getName();
        int screenshotNumber = model.getTestFiles().get(model.getFileIndex()).getTimestamps().size();
        model.lastTestInfo = new TestInfo(name, model.getLoopNumber(), screenshotNumber);
        model.testIndex = TestTypes.SOFTWARE_SCREENSHOT;
        MediaInfo currentFile = model.getTestFiles().get(model.getFileIndex());
        try {
            createIntentForVlc(currentFile, new OnIntentCreatedListener() {
                @Override
                public void onItentCreated(Intent intent) {
                    /* In case of failure due to an invalid file, stop benchmark, and display download page */
                    if (intent == null) {
                        dismissDialog();
                        Log.e(TAG, "launchTests: " + getString(R.string.dialog_text_invalid_file ));
                        new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_invalid_file).display(VLCWorkerModel.this);
                        setCurrentFragment(R.id.home_nav);
                        return;
                    }
                    model.setRunning(true);
                    startActivityForResult(intent, Constants.RequestCodes.VLC);
                }
            });

        } catch (ActivityNotFoundException e) {
            new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_vlc_failed).display(this);
            Log.e(TAG, "launchTests: Failed to start VLC");
        }
    }

    /**
     * This method is called when local media files were checked and validated
     * The files set by the method are those that the benchmark is going to test with VLC
     * @param files list of metadata for all the video/media to test.
     */
    public void setBenchmarkFiles(List<MediaInfo> files) {
        model.testFiles = files;
        model.testIndex = TestTypes.SOFTWARE_SCREENSHOT;
    }

    /**
     * This method creates a new intent that corresponds with VLC's BenchActivity launch protocol.
     * @param currentFile metadata about the current file
     * @return a new Intent
     */
    private void createIntentForVlc(MediaInfo currentFile, OnIntentCreatedListener listener) {
        try {
            StorageManager.INSTANCE.checkFileSumAsync(new File(currentFile.getLocalUrl()), currentFile.getChecksum(), valid -> {
                if (!valid) {
                    Log.e(TAG, "createIntentForVlc: Invalid file");
                    listener.onItentCreated(null);
                }

                File mediaFile = new File(currentFile.getLocalUrl());
                Uri uri = FileProvider.getUriForFile(VLCWorkerModel.this, BuildConfig.APPLICATION_ID + ".benchmark.VLCBenchmarkFileProvider", mediaFile);
                Intent vlcIntent = new Intent(Intent.ACTION_VIEW);
                vlcIntent.setPackage(getString(R.string.vlc_package_name));
                vlcIntent.setComponent(new ComponentName(getString(R.string.vlc_package_name), BENCH_ACTIVITY));
                vlcIntent.setDataAndTypeAndNormalize(uri, "video/*");
                vlcIntent.putExtra(EXTRA_BENCHMARK, true);
                vlcIntent.putExtra(EXTRA_ACTION, model.getTestIndex().isScreenshot( ) ? EXTRA_ACTION_QUALITY : EXTRA_ACTION_PLAYBACK);
                vlcIntent.putExtra(EXTRA_HARDWARE, model.getTestIndex().isSoftware());
                if (model.getTestIndex().isScreenshot()) {
                    vlcIntent.putExtra(EXTRA_TIMESTAMPS, (Serializable) currentFile.getTimestamps());
                    vlcIntent.putExtra(EXTRA_SCREENSHOT_DIR, StorageManager.INSTANCE.getFolderStr(StorageManager.INSTANCE.screenshotFolder));
                }
                vlcIntent.putExtra(EXTRA_FROM_START, true);
                vlcIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (model.getTestIndex().isSoftware() && model.getTestIndex().isScreenshot())
                    Log.d(TAG, "onActivityResult: ===========================================================================================================" );
                Log.i(TAG, "Testing: " + currentFile.getName());
                Log.i(TAG, "Testing mode: " + ( model.getTestIndex().isSoftware() ? "Software - " : "Hardware - " )
                        + (model.getTestIndex().isScreenshot() ? "Quality" : "Playback"));

                listener.onItentCreated(vlcIntent);
            });
        } catch (Exception e) {
            Log.e(TAG, "createIntentForVlc: " + e.toString() );
            listener.onItentCreated(null);
        }
    }

    public interface OnIntentCreatedListener {
        void onItentCreated(Intent intent);
    }

    /**
     * Re-entry point, in this method we receive directly from VLC its
     * result code along with an Intent giving extra information about the result of VLC.
     * <p>
     * This method will be called a each end of a test and will therefor handle the launch of the next tests.
     * It also handle that case were crashed without notice by checking if the Intent in argument if null and if so
     * by getting the String describing the crashed VLC had by reading into VLC's shared preferences.
     * <p>
     * This method also calls a number of abstract method to allow the UI to update itself.
     *
     * @param requestCode the code we gave to VLC to launch itself.
     * @param resultCode  the code on which VLC finished.
     * @param data an Intent describing additional information and data about the test.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) { //TODO refactor all this, lots of useless stuff
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.RequestCodes.VLC) {
            Log.i(TAG, "onActivityResult: resultCode: " + resultCode );
             mData = data;
             mResultCode = resultCode;
        } else if (requestCode == Constants.RequestCodes.GOOGLE_CONNECTION) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_page_fragment_holder);
            if (fragment != null) {
                fragment.onActivityResult(requestCode, resultCode, data);
            } else {
                Log.e(TAG, "onActivityResult: GOOGLE_CONNECTION: fragment is null");
            }
        }
    }

    /**
     * Small factoring function.
     *
     * @param data   Intent contained results from VLC.
     * @param resultCode Result code from VLC
     */
    private void fillCurrentTestInfo(Intent data, int resultCode) {
        if ( resultCode != Constants.ResultCodes.RESULT_OK) {
            String errorMessage;
            switch (resultCode) {
                case Constants.ResultCodes.RESULT_CANCELED:
                    errorMessage = getString(R.string.result_canceled);
                    break;
                case Constants.ResultCodes.RESULT_NO_HW:
                    errorMessage = getString(R.string.result_no_hw);
                    break;
                case Constants.ResultCodes.RESULT_CONNECTION_FAILED:
                    errorMessage = getString(R.string.result_connection_failed);
                    break;
                case Constants.ResultCodes.RESULT_PLAYBACK_ERROR:
                    errorMessage = getString(R.string.result_playback_error);
                    break;
                case Constants.ResultCodes.RESULT_HARDWARE_ACCELERATION_ERROR:
                    errorMessage = getString(R.string.result_hardware_acceleration_error);
                    break;
                case Constants.ResultCodes.RESULT_VIDEO_TRACK_LOST:
                    errorMessage = getString(R.string.result_video_track_lost);
                    break;
                case Constants.ResultCodes.RESULT_VLC_CRASH:
                    if (data != null && data.hasExtra("Error")) {
                        errorMessage = data.getStringExtra("Error");
                    } else if (data != null) {
                        errorMessage = getString(R.string.result_vlc_crash);
                    } else {
                        try {
                            Context packageContext = createPackageContext(getString(R.string.vlc_package_name), 0);
                            SharedPreferences preferences = packageContext.getSharedPreferences(SHARED_PREFERENCE, Context.MODE_PRIVATE);
                            errorMessage = preferences.getString(SHARED_PREFERENCE_STACK_TRACE, null);
                        } catch (PackageManager.NameNotFoundException e) {
                            errorMessage = e.getMessage();
                        }
                    }
                    break;
                default:
                    errorMessage = getString(R.string.result_unknown);
                    break;
            }
            Log.i(TAG, "fillCurrentTestInfo: error: " + errorMessage);
            model.lastTestInfo.vlcCrashed(model.getTestIndex().isSoftware(), model.getTestIndex().isScreenshot(), errorMessage);
        } else if (!model.getTestIndex().isScreenshot()) {
            model.lastTestInfo.setBadFrames(data.getIntExtra("number_of_dropped_frames", 0), model.getTestIndex().isSoftware());
            model.lastTestInfo.setWarningNumber(data.getIntExtra("late_frames", 0), model.getTestIndex().isSoftware());
        }
    }

    /**
     * This method is called once a screenshot test is finished.
     * It spawns a new thread that will iterates over the screenshots
     * and check their existence and validity.
     * <p>
     * Every time said conditions are not met a counter is incremented.
     * At the end of the Thread we update call {@link TestInfo#setBadScreenshot(double, boolean, []integer, []string)} with said number
     * and call {@link VLCWorkerModel#launchNextTest()} on the UI thread.
     */
    private void testScreenshot() {
        final String screenshotFolder = StorageManager.INSTANCE.getFolderStr(StorageManager.INSTANCE.screenshotFolder);
        final int numberOfScreenshot = model.getTestFiles().get(model.getFileIndex()).getColors().size();
        final List<int[]> colors = model.getTestFiles().get(model.getFileIndex()).getColors();

        new Thread() {
            @Override
            public void run() {
                int badScreenshots = 0;
                ArrayList<Integer> indexList = new ArrayList<>();
                ArrayList<String> screenList = new ArrayList<>();
                ArrayList<Integer> scoreList = new ArrayList<>();
                for (int i = 0; i < numberOfScreenshot; i++) {
                    String filePath = screenshotFolder + "/" + SCREENSHOT_NAMING + i + ".png";
                    File file = new File(filePath);
                    Pair<Boolean, Integer> res = ScreenshotValidator.validateScreenshot(filePath, colors.get(i));
                    if (res != null && !res.first) {
                        badScreenshots++;
//                        String fileName = (model.getFileIndex() + 1 ) + "_" + model.testIndex.toString();
//                        fileName += "_" + i + ".png";
//                        File renamedFile = new File(screenshotFolder + "/" + fileName);
//                        if (file.renameTo(renamedFile)) {
//                            indexList.add(i);
//                            screenList.add(fileName);
//                            scoreList.add(res.second);
//                        }
                    } else {
//                        if (!file.delete())
//                            Log.e(TAG, "Failed to delete screenshots");
                    }
                    String fileName = (model.getFileIndex() + 1 ) + "_" + model.testIndex.toString();
                    fileName += "_" + i + ".png";
                    File renamedFile = new File(screenshotFolder + "/" + fileName);
                    Log.w(TAG, "run: " + fileName);
                    if (file.renameTo(renamedFile)) {
                        Log.w(TAG, "run: adding");
                        indexList.add(i);
                        screenList.add(fileName);
                        scoreList.add(res.second);
                    }
                }
                model.lastTestInfo.setBadScreenshot(
                        100.0 * badScreenshots / numberOfScreenshot,
                        model.getTestIndex().isSoftware(), indexList.toArray(new Integer[0]),
                        screenList.toArray(new String[0]), scoreList.toArray(new Integer[0]));
                runOnUiThread(() -> launchNextTest());
            }
        }.start();
    }

    /**
     * This method increment all the counters relatives to the type of test we will do,
     * such as the type of test we should do, the index of the file we're testing and
     * on what loop are we.
     * <p>
     * If we reached the end of the tests we then calculate the average score for hardware and software
     * call the abstract method {@link VLCWorkerModel#onTestsFinished(List[])} and return
     * <p>
     * Otherwise we launch VLC's BenchActivity with the counters' new values.
     */
    private void launchNextTest() {
        if (model.getRunning()) {
            if (model.getTestIndex() == TestTypes.HARDWARE_PLAYBACK) {
                model.testResults[model.getLoopNumber()].add(model.lastTestInfo);
                model.setFileIndex(model.getFileIndex() + 1);
                if (model.getFileIndex() >= model.getTestFiles().size()) {
                    model.setLoopNumber(model.getLoopNumber() + 1);
                    model.setFileIndex(0);
                }
                if (model.getLoopNumber() >= model.getLoopTotal()) {
                    onTestsFinished(model.testResults);
                    return;
                }
                ProgressSaver.save(this, model.testResults);
            }
            model.setTestIndex(model.getTestIndex().next());
            if (model.testIndex.ordinal() == 0) {
                String name = model.getTestFiles().get(model.getFileIndex()).getName();
                int screenshotNumber = model.getTestFiles().get(model.getFileIndex()).getTimestamps().size();
                model.lastTestInfo = new TestInfo(name, model.getLoopNumber(), screenshotNumber);
            }
            MediaInfo currentFile = model.getTestFiles().get(model.getFileIndex());
            createIntentForVlc(currentFile, intent -> {
                if (intent == null) {
                    dismissDialog();
                    model.setRunning(false);
                    new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_invalid_file).display(VLCWorkerModel.this);
                    setCurrentFragment(R.id.home_nav);
                    return;
                }
                // Add delay for vlc to finish correctly
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    if (model.getRunning()) {
                        startActivityForResult(intent, Constants.RequestCodes.VLC);
                    }
                }, 4000);
            });

        } else {
            Log.e(TAG, "launchNextTest was called but running is false.");
        }
    }

    void startResultPage(String name) {
        if (name == null) {
            new DialogInstance(R.string.dialog_title_oups, R.string.dialog_text_save_failure)
                    .display(this);
            return;
        }
        Intent intent = new Intent(VLCWorkerModel.this, ResultPage.class);
        intent.putExtra("name", name);
        intent.putExtra("fromBench", true);
        startActivityForResult(intent, Constants.RequestCodes.RESULTS);
    }

    private void onTestsFinished(List<TestInfo>[] results) {
        ArrayList<TestInfo> finalResults = TestInfo.mergeTests(results);
        dismissDialog();
        model.setRunning(false);
        ProgressSaver.discard(this);
        Util.runInBackground(() -> {
            String savedName = null;
            try {
                savedName = JsonHandler.save(finalResults);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to save test : " + e.toString());
            }
            final String name = savedName;
            Util.runInUiThread(() -> startResultPage(name));
        });

    }

    private int getMaxValue() {
        return model.getTestFiles().size() * 4 * model.getLoopTotal();
    }

    private double getProgressValue() {
        return ((model.getTestFiles().size() * 4 * (model.getLoopNumber())) +
                (model.getFileIndex() * 4) +
                (model.getTestIndex().ordinal() + 1)) /
                (double)getMaxValue() * 100d;
    }

    private String progressToString() {
        if (model.getLoopTotal() > 1) {
            return String.format(
                    getResources().getString(R.string.progress_text_format_loop),
                    FormatStr.INSTANCE.format2Dec(getProgressValue()), model.getFileIndex() + 1,
                    model.getTestFiles().size(), model.getTestIndex(),
                    model.getLoopNumber() + 1, model.getLoopTotal());
        } else {
            return String.format(
                    getResources().getString(R.string.progress_text_format),
                    FormatStr.INSTANCE.format2Dec(getProgressValue()), model.getFileIndex() + 1,
                    model.getTestFiles().size(), model.getTestIndex());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (model.getRunning()) {
            String name = model.getTestFiles().get(model.getFileIndex()).getName();
            updateProgress(getProgressValue(), progressToString(), name);

            // -2 isn't return by vlc-android
            // small hack to stop the benchmark from restarting vlc if there is a
            // configuration change between onResume and vlc starting
            if (mResultCode != -2) {
                fillCurrentTestInfo(mData, mResultCode);
                /* case where no screenshots */
                /* if screenshots, launchNextTest called from Screenshot Validation thread */
                if (model.getTestIndex().isScreenshot() && mResultCode == Constants.ResultCodes.RESULT_OK) {
                    mResultCode = -2;
                    testScreenshot();
                } else {
                    mResultCode = -2;
                    launchNextTest();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (model.getRunning()) {
            outState.putBoolean(STATE_RUNNING, model.getRunning());
            outState.putSerializable(STATE_TEST_FILES, (Serializable) model.getTestFiles());
            outState.putInt(STATE_TEST_INDEX, model.getTestIndex().ordinal());
            outState.putInt(STATE_FILE_INDEX, model.getFileIndex());
            outState.putInt(STATE_LOOP_NUMBER, model.getLoopNumber());
            outState.putInt(STATE_LOOP_TOTAL, model.getLoopTotal());
            outState.putSerializable(STATE_TEST_RESULTS, model.getTestResults());
            outState.putSerializable(STATE_LAST_TEST_INFO, model.getLastTestInfo());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        List<MediaInfo> testFiles;
        List<TestInfo> testResults[];
        TestInfo lastTestInfo;

        model.setRunning(savedInstanceState.getBoolean(STATE_RUNNING));
        model.setTestIndex(TestTypes.values()[savedInstanceState.getInt(STATE_TEST_INDEX)]);
        model.setFileIndex(savedInstanceState.getInt(STATE_FILE_INDEX));
        model.setLoopNumber(savedInstanceState.getInt(STATE_LOOP_NUMBER));
        model.setLoopTotal(savedInstanceState.getInt(STATE_LOOP_TOTAL));

        testFiles = (List<MediaInfo>)savedInstanceState.getSerializable(STATE_TEST_FILES);
        if (testFiles != null) {
            model.setTestFiles(testFiles);
        }
        testResults = (List<TestInfo>[])savedInstanceState.getSerializable(STATE_TEST_RESULTS);
        if (testResults != null) {
            model.setTestResults(testResults);
        }
        lastTestInfo = (TestInfo)savedInstanceState.getSerializable(STATE_LAST_TEST_INFO);
        if (lastTestInfo != null) {
            model.setLastTestInfo(lastTestInfo);
        }
    }
}
