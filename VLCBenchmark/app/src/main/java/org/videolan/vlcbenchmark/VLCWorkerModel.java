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
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.UiThread;
import android.util.Log;

import org.json.JSONException;
import org.videolan.vlcbenchmark.benchmark.BenchmarkViewModel;
import org.videolan.vlcbenchmark.benchmark.TestTypes;
import org.videolan.vlcbenchmark.tools.FormatStr;
import org.videolan.vlcbenchmark.tools.MediaInfo;
import org.videolan.vlcbenchmark.tools.CrashHandler;
import org.videolan.vlcbenchmark.tools.DialogInstance;
import org.videolan.vlcbenchmark.tools.FileHandler;
import org.videolan.vlcbenchmark.tools.GoogleConnectionHandler;
import org.videolan.vlcbenchmark.tools.JsonHandler;
import org.videolan.vlcbenchmark.tools.ProgressSaver;
import org.videolan.vlcbenchmark.tools.ScreenshotValidator;
import org.videolan.vlcbenchmark.tools.TestInfo;
import org.videolan.vlcbenchmark.tools.Util;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

/**
 * Created by penava_b on 16/08/16.
 * <p>
 * Main class of the project.
 * This class handle the whole logic/algorithm side of the application.
 * <p>
 * It launches BenchService and retrieve the List<MediaInfo> from its result;
 * but also handles the launch of VLC's BenchActivity, the retrieval of its results and their interpretation.
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

    private static final String SCREENSHOTS_EXTRA = "org.videolan.vlc.gui.video.benchmark.TIMESTAMPS";
    private static final String BENCH_ACTIVITY = "org.videolan.vlc.gui.video.benchmark.BenchActivity";
    private static final String SCREENSHOT_ACTION = "org.videolan.vlc.gui.video.benchmark.ACTION_SCREENSHOTS";
    private static final String PLAYBACK_ACTION = "org.videolan.vlc.gui.video.benchmark.ACTION_PLAYBACK";
    private static final String INTENT_SCREENSHOT_DIR = "SCREENSHOT_DIR";
    private static final String SCREENSHOT_NAMING = "Screenshot_";
    private static final String SHARED_PREFERENCE = "org.videolab.vlc.gui.video.benchmark.UNCAUGHT_EXCEPTIONS";
    private static final String SHARED_PREFERENCE_STACK_TRACE = "org.videolab.vlc.gui.video.benchmark.STACK_TRACE";

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

        setupUiMembers(savedInstanceState);

        GoogleConnectionHandler.getInstance();
    }

    /**
     * Entry point of the class.
     * Calling this method will result in the creation and launch of {@link org.videolan.vlcbenchmark.service.BenchService}.
     * Launch that will result, if everything goes well, in the call of {@link VLCWorkerModel#doneReceived(List)} which will
     * start VLC.
     *
     * @param numberOfTests number of repetition of all the tests. must be 1 or 3 other values are ignored.
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
        model.testIndex = TestTypes.SOFTWARE_SCREENSHOT;
        Log.w(TAG, "launchTests: " + model.testIndex + " - " + model.testIndex.ordinal());

        MediaInfo currentFile = model.getTestFiles().get(model.getFileIndex());
        try {
            Intent intent = createIntentForVlc(currentFile);
            /* In case of failure due to an invalid file, stop benchmark, and display download page */
            if (intent == null) {
                dismissDialog();
                Log.e(TAG, "launchTests: " + getString(R.string.dialog_text_invalid_file ));
                new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_invalid_file).display(this);
                setCurrentFragment(R.id.home_nav);
                return;
            }
            model.setRunning(true);
            startActivityForResult(intent, Constants.RequestCodes.VLC);
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
        Log.w(TAG, "setBenchmarkFiles: ");
        model.testFiles = files;
        model.testIndex = TestTypes.SOFTWARE_SCREENSHOT;
    }

    /**
     * This method creates a new intent that corresponds with VLC's BenchActivity launch protocol.
     * @param currentFile metadata about the current file
     * @return a new Intent
     */
    private Intent createIntentForVlc(MediaInfo currentFile) {
        boolean validFile = false;
        try {
            validFile = FileHandler.checkFileSum(new File(currentFile.getLocalUrl()), currentFile.getChecksum());
        } catch (Exception e) {
            Log.e(TAG, "createIntentForVlc: " + e.toString() );
        }
        if (!validFile) {
            Log.e(TAG, "createIntentForVlc: Invalid file");
            return null;
        }

        Intent intent = new Intent(model.getTestIndex().isScreenshot() ? SCREENSHOT_ACTION : PLAYBACK_ACTION)
                .setComponent(new ComponentName(getString(R.string.vlc_package_name), BENCH_ACTIVITY))
                .putExtra("item_location", Uri.parse("file://" + currentFile.getLocalUrl()));
        if (model.getTestIndex().isSoftware())
            intent = intent.putExtra("disable_hardware", true);
        if (model.getTestIndex().isScreenshot())
            intent = intent.putExtra(SCREENSHOTS_EXTRA, (Serializable) currentFile.getSnapshot());
        intent.putExtra(INTENT_SCREENSHOT_DIR, FileHandler.getFolderStr(FileHandler.screenshotFolder));
        intent.putExtra("from_start", true);

        if (model.getTestIndex().isSoftware() && model.getTestIndex().isScreenshot())
            Log.d(TAG, "onActivityResult: ===========================================================================================================" );
        Log.i(TAG, "Testing: " + currentFile.getName());
        Log.i(TAG, "Testing mode: " + ( model.getTestIndex().isSoftware() ? "Software - " : "Hardware - " )
            + (model.getTestIndex().isScreenshot() ? "Quality" : "Playback"));
        return intent;
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
             if (model.getTestIndex().ordinal() == 0) {
                 String name = model.getTestFiles().get(model.getFileIndex()).getName();
                 model.lastTestInfo = new TestInfo(name, model.getLoopNumber());
             }
             mData = data;
             mResultCode = resultCode;
        } else if (requestCode == Constants.RequestCodes.GOOGLE_CONNECTION) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_page_fragment_holder);
            fragment.onActivityResult(requestCode, resultCode, data);
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
     * At the end of the Thread we update call {@link TestInfo#setBadScreenshot(double, boolean)} with said number
     * and call {@link VLCWorkerModel#launchTests(int)} on the UI thread.
     */
    private void testScreenshot() {
        final String screenshotFolder = FileHandler.getFolderStr(FileHandler.screenshotFolder);
        final int numberOfScreenshot = model.getTestFiles().get(model.getFileIndex()).getColors().size();
        final List<int[]> colors = model.getTestFiles().get(model.getFileIndex()).getColors();

        new Thread() {
            @Override
            public void run() {
                int badScreenshots = 0;
                for (int i = 0; i < numberOfScreenshot; i++) {
                    String filePath = screenshotFolder + "/" + SCREENSHOT_NAMING + i + ".png";
                    File file = new File(filePath);
                    boolean exists;
                    if (!(exists = file.exists()) ||
                            !ScreenshotValidator.validateScreenshot(filePath, colors.get(i))) {
                        badScreenshots++;
                    }
                    if (exists && !file.delete())
                        Log.e(TAG, "Failed to delete screenshot");
                }
                model.lastTestInfo.setBadScreenshot(100.0 * badScreenshots / numberOfScreenshot, model.getTestIndex().isSoftware());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        launchNextTest();
                    }
                });
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
        Log.w(TAG, "launchNextTest: running: " + model.getRunning());
        if (model.getRunning()) {
            if (model.getTestIndex() == TestTypes.HARDWARE_PLAYBACK) {
                model.testResults[model.getLoopNumber()].add(model.lastTestInfo);
                model.lastTestInfo = null;
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
            Log.w(TAG, "launchNextTest: " + Thread.currentThread().getName());
            Log.w(TAG, "launchNextTest: testIndex: " + model.getTestIndex() + " - " + model.getTestIndex().ordinal());
            model.setTestIndex(model.getTestIndex().next());
            Log.w(TAG, "launchNextTest: testIndex: " + model.getTestIndex() + " - " + model.getTestIndex().ordinal());
            MediaInfo currentFile = model.getTestFiles().get(model.getFileIndex());
            final Intent intent = createIntentForVlc(currentFile);
            if (intent == null) {
                Log.e(TAG, "launchNextTest: " + getString(R.string.dialog_text_invalid_file));
                dismissDialog();
                model.setRunning(false);
                new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_invalid_file).display(this);
                setCurrentFragment(R.id.home_nav);
                return;
            }
            // Add delay for vlc to finish correctly
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (model.getRunning()) {
                        unlockOrientation();
                        startActivityForResult(intent, Constants.RequestCodes.VLC);
                    }
                }
            }, 4000);
        } else {
            Log.e(TAG, "launchNextTest was called but running is false.");
        }
        Log.w(TAG, "launchNextTest: end");
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
        Log.w(TAG, "onTestsFinished: ");
        ArrayList<TestInfo> finalResults = TestInfo.mergeTests(results);
        dismissDialog();
        model.setRunning(false);
        ProgressSaver.discard(this);
        unlockOrientation();
        Util.runInBackground(new Runnable() {
            @Override
            public void run() {
                String savedName = null;
                try {
                    savedName = JsonHandler.save(finalResults);
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to save test : " + e.toString());
                }
                final String name = savedName;
                Util.runInUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startResultPage(name);
                    }
                });
            }
        });

    }

    private String progressToString() {
        int max = model.getTestFiles().size() * 4 * model.getLoopTotal();
        double progress = ((model.getFileIndex() - 1)* 4 * model.getLoopNumber() +
                model.getTestIndex().ordinal() + 1) / (double)max* 100d;
        return String.format(
                getResources().getString(R.string.progress_text_format_loop),
                FormatStr.format2Dec(progress), model.getFileIndex(),
                model.getTestFiles().size(), model.getTestIndex().ordinal() + 1,
                model.getLoopNumber(), model.getLoopTotal());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w(TAG, "onPause: ");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.w(TAG, "onConfigurationChanged: ");
        super.onConfigurationChanged(newConfig);
    }

    private void lockOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    }

    private void unlockOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    protected void onResume() {
        Log.w(TAG, "onResume: ");
        super.onResume();
        if (model.getRunning()) {
            lockOrientation();
            String name = model.getTestFiles().get(model.getFileIndex()).getName();
            int max = model.getTestFiles().size() * 4 * model.getLoopTotal();
            double progress = ((model.getFileIndex() - 1) * 4 * model.getLoopNumber() +
                    model.getTestIndex().ordinal() + 1) / (double) max * 100d;
            updateProgress(progress, progressToString(), name);

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

}
