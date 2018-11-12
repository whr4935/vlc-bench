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

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.json.JSONException;
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

    private List<TestInfo>[] resultsTest;
    private List<MediaInfo> testFiles;
    private ArrayList<TestInfo> finalResults;
    private TEST_TYPES testIndex = TEST_TYPES.SOFTWARE_SCREENSHOT;
    private int fileIndex = 0;
    private int loopNumber = 0;
    private TestInfo lastTestInfo = null;
    protected int numberOfTests;
    protected boolean running = false;

    /**
     * Enum tool used internally only to iterate simply
     * over the different types of tests.
     */
    private enum TEST_TYPES {
        SOFTWARE_SCREENSHOT,
        SOFTWARE_PLAYBACK,
        HARDWARE_SCREENSHOT,
        HARDWARE_PLAYBACK;

        /**
         * Allows to use this enum as an incrementing type that loops once it reached its last value.
         * @return the next enum in ordinal order after the current one. If none are after it will return the first.
         */
        public TEST_TYPES next() {
            return values()[(ordinal() + 1) % values().length];
        }

        /**
         * @return true if the ordinal of the current enum represents a test software
         */
        public boolean isSoftware() {
            return (ordinal() / 2) % 2 == 0;
        }

        /**
         * @return true if the ordinal of the current enum represned a screenshot test
         */
        public boolean isScreenshot() {
            return ordinal() % 2 == 0;
        }

        /**
         * @return a human readable version of the enum's value by replacing underscores with spaces and passing the string to low case.
         */
        @Override
        public String toString() {
            return super.toString().replace("_", " ").toLowerCase();
        }
    }

    private static final String SCREENSHOTS_EXTRA = "org.videolan.vlc.gui.video.benchmark.TIMESTAMPS";
    private static final String BENCH_ACTIVITY = "org.videolan.vlc.gui.video.benchmark.BenchActivity";
    private static final String SCREENSHOT_ACTION = "org.videolan.vlc.gui.video.benchmark.ACTION_SCREENSHOTS";
    private static final String PLAYBACK_ACTION = "org.videolan.vlc.gui.video.benchmark.ACTION_PLAYBACK";
    private static final String INTENT_SCREENSHOT_DIR = "SCREENSHOT_DIR";
    private static final String SCREENSHOT_NAMING = "Screenshot_";
    private static final String SHARED_PREFERENCE = "org.videolab.vlc.gui.video.benchmark.UNCAUGHT_EXCEPTIONS";
    private static final String SHARED_PREFERENCE_STACK_TRACE = "org.videolab.vlc.gui.video.benchmark.STACK_TRACE";

    /* State keys */
    private static final String STATE_RUNNING = "STATE_RUNNING";
    private static final String STATE_TEST_FILES = "STATE_TEST_FILES";
    private static final String STATE_TEST_INDEX = "STATE_TEST_INDEX";
    private static final String STATE_FILE_INDEX = "STATE_FILE_INDEX";
    private static final String STATE_TEST_NUMBER = "STATE_TEST_NUMBER";
    private static final String STATE_CUR_LOOP_NUMBER = "STATE_CUR_LOOP_NUMBER";
    private static final String STATE_RESULT_TEST = "STATE_RESULT_TEST";
    private static final String STATE_LAST_TEST_INFO = "STATE_LAST_TEST_INFO";

    public abstract void setFilesChecked(boolean hasChecked);

    protected abstract boolean setCurrentFragment(int itemId);

    public abstract void dismissDialog();

    public abstract void resetDownload();
    /**
     * Is called during the {@link VLCWorkerModel#onCreate(Bundle)}.
     */
    protected abstract void setupUiMembers(Bundle savedInstanceState);

    /**
     * Called when ready to check presence and integrity of media files;
     */
    protected abstract void checkFiles();
    /**
     * Called to update the test dialog.
     * @param testName the name of the test (ex : screenshot software, ...)
     * @param fileIndex the index of the current file
     * @param numberOfFiles the total number of files
     * @param testNumber the index of the current test ({@link TEST_TYPES#ordinal()}
     * @param loopNumber the number of times we've repeated all tests
     * @param numberOfLoops the total number of time we have to repeat
     */
    protected abstract void updateTestProgress(String testName, int fileIndex, int numberOfFiles, int testNumber, int loopNumber, int numberOfLoops);

    /**
     * Is called if VLC stopped due to an uncaught exception while testing.
     *
     * @param errorMessage a String representing the issue that caused VLC to crash.
     * @param resume needs to be called if the implementation needs to continue testing other files.
     */
    protected abstract void onVlcCrashed(String errorMessage, Runnable resume);

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

        if (savedInstanceState != null) {
            running = savedInstanceState.getBoolean(STATE_RUNNING);
        }

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
        resultsTest = previousTest;
        if (resultsTest == null) {
            if (numberOfTests == 1) {
                this.numberOfTests = 1;
                resultsTest = new ArrayList[]{new ArrayList<TestInfo>()};
            } else if (numberOfTests == 3) {
                this.numberOfTests = 3;
                resultsTest = new ArrayList[]{new ArrayList<MediaInfo>(), new ArrayList<MediaInfo>(), new ArrayList<MediaInfo>()};
            } else {
                Log.e(TAG, "Wrong number of tests to start: " + numberOfTests);
                return;
            }
            fileIndex = 0;
            loopNumber = 0;
        } else {
            this.numberOfTests = resultsTest.length;
            int i = 0;
            while (i < this.numberOfTests && resultsTest[i].size() != 0)
                i += 1;
            loopNumber = i - 1;
            fileIndex = resultsTest[loopNumber].size();
        }
        testIndex = TEST_TYPES.SOFTWARE_SCREENSHOT;

        MediaInfo currentFile = testFiles.get(fileIndex);
        try {
            running = true;
            Intent intent = createIntentForVlc(currentFile);
            /* In case of failure due to an invalid file, stop benchmark, and display download page */
            if (intent == null) {
                dismissDialog();
                running = false;
                Log.e(TAG, "launchTests: " + getString(R.string.dialog_text_invalid_file ));
                new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_invalid_file).display(this);
                resetDownload();
                setCurrentFragment(R.id.home_nav);
                return;
            }
            startActivityForResult(intent, Constants.RequestCodes.VLC);
        } catch (ActivityNotFoundException e) {
            new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_vlc_failed).display(this);
            Log.e(TAG, "launchTests: Failed to start VLC");
            return;
        }
    }

    /**
     * This method is called when local media files were checked and validated
     * The files set by the method are those that the benchmark is going to test with VLC
     * @param files list of metadata for all the video/media to test.
     */
    public void setBenchmarkFiles(List<MediaInfo> files) {
        testFiles = files;
        testIndex = TEST_TYPES.SOFTWARE_SCREENSHOT;
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
        Intent intent = new Intent(testIndex.isScreenshot() ? SCREENSHOT_ACTION : PLAYBACK_ACTION)
                .setComponent(new ComponentName(getString(R.string.vlc_package_name), BENCH_ACTIVITY))
                .putExtra("item_location", Uri.parse("file://" + currentFile.getLocalUrl()));
        if (testIndex.isSoftware())
            intent = intent.putExtra("disable_hardware", true);
        if (testIndex.isScreenshot())
            intent = intent.putExtra(SCREENSHOTS_EXTRA, (Serializable) currentFile.getSnapshot());
        intent.putExtra(INTENT_SCREENSHOT_DIR, FileHandler.getFolderStr(FileHandler.screenshotFolder));
        intent.putExtra("from_start", true);
        if (testIndex.isSoftware() && testIndex.isScreenshot())
            Log.d(TAG, "onActivityResult: ===========================================================================================================" );
        Log.i(TAG, "Testing: " + currentFile.getName());
        Log.i(TAG, "Testing mode: " + ( testIndex.isSoftware() ? "Software - " : "Hardware - " )
            + (testIndex.isScreenshot() ? "Quality" : "Playback"));
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
             if (testIndex.ordinal() == 0) {
                 String name = testFiles.get(fileIndex).getName();
                 lastTestInfo = new TestInfo(name, loopNumber);
            }
            fillCurrentTestInfo(data, resultCode);
        } else if (requestCode == Constants.RequestCodes.GOOGLE_CONNECTION) {
            android.support.v4.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_page_fragment_holder);
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
            lastTestInfo.vlcCrashed(testIndex.isSoftware(), testIndex.isScreenshot(), errorMessage);
            if (testIndex.isScreenshot()) {
                // When a quality test fails, there is no screenshot analysis,
                // hence no delay before re-starting vlc-android.
                // That sometimes doesn't let vlc-android exit properly before being recreated.
                // For that specific case a delay was introduced.
                // But the class should be refactored as to not start anything in onActivityResult()
                // but rather later, in onResume() for example.
                Handler mHandler = new Handler();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        launchNextTest();
                    }
                }, 2000);
            }
        } else if (testIndex.isScreenshot()) {
            testScreenshot();
        } else {
            lastTestInfo.setBadFrames(data.getIntExtra("number_of_dropped_frames", 0), testIndex.isSoftware());
            lastTestInfo.setWarningNumber(data.getIntExtra("late_frames", 0), testIndex.isSoftware());
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
        final int numberOfScreenshot = testFiles.get(fileIndex).getColors().size();
        final List<int[]> colors = testFiles.get(fileIndex).getColors();

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
                lastTestInfo.setBadScreenshot(100.0 * badScreenshots / numberOfScreenshot, testIndex.isSoftware());
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
        if (running) {
            if (testIndex == TEST_TYPES.HARDWARE_PLAYBACK) {
                resultsTest[loopNumber].add(lastTestInfo);
                lastTestInfo = null;
                fileIndex++;
                Log.e(TAG, "launchNextTest: fileIndex: " + fileIndex );
                if (fileIndex >= testFiles.size()) {
                    loopNumber++;
                    fileIndex = 0;
                }
                if (loopNumber >= numberOfTests) {
                    onTestsFinished(resultsTest);
                    return;
                }
                ProgressSaver.save(this, resultsTest);
            }
            testIndex = testIndex.next();
            MediaInfo currentFile = testFiles.get(fileIndex);
            final Intent intent = createIntentForVlc(currentFile);
            if (intent == null) {
                Log.e(TAG, "launchNextTest: " + getString(R.string.dialog_text_invalid_file));
                dismissDialog();
                running = false;
                new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_invalid_file).display(this);
                resetDownload();
                setCurrentFragment(R.id.home_nav);
                return;
            }
            // Add delay for vlc to finish correctly
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivityForResult(intent, Constants.RequestCodes.VLC);
                }
            }, 4000);
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
        finalResults = TestInfo.mergeTests(results);
        dismissDialog();
        running = false;
        ProgressSaver.discard(this);
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

    private int checkVersion(String str1, String str2) {
        String[] vals1 = str1.split("\\.");
        String[] vals2 = str2.split("\\.");
        int i = 0;

        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }

        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }

        return Integer.signum(vals1.length - vals2.length);
    }

    public boolean checkVlcVersion() {
        if (!BuildConfig.DEBUG) {
            try { // tmp during the VLCBenchmark alpha, using the vlc beta
                if (checkVersion(this.getPackageManager().getPackageInfo(getString(R.string.vlc_package_name), 0).versionName, BuildConfig.VLC_VERSION) < 0) {
                    return false;
                }
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tool method to check if VLC's signature and ours match.
     *
     * @return true if VLC's signature matches our else false
     */
    public boolean checkSignature() {
        String benchPackageName = this.getPackageName();
        Signature[] sigs_vlc = null;
        Signature[] sigs = null;
        int vlcSignature;
        int benchSignature;

        /* Getting application signature*/
        try {
            sigs = this.getPackageManager().getPackageInfo(benchPackageName, PackageManager.GET_SIGNATURES).signatures;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        /* Checking to see if there is any signature */
        if (sigs != null && sigs.length > 0)
            benchSignature = sigs[0].hashCode();
        else
            return false;

        /* Getting vlc's signature */
        try {
            sigs_vlc = this.getPackageManager().getPackageInfo(getString(R.string.vlc_package_name), PackageManager.GET_SIGNATURES).signatures;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        /* checking to see if there is are any signatures */
        if (sigs_vlc != null && sigs_vlc.length > 0)
            vlcSignature = sigs_vlc[0].hashCode();
        else
            return false;

        return benchSignature == vlcSignature;
    }

    /**
     * Save all the fields of the current instance
     *
     * @param savedInstanceState Bundle in which we save said data
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(STATE_RUNNING, running);
        savedInstanceState.putSerializable(STATE_TEST_FILES, (Serializable) testFiles);
        savedInstanceState.putInt(STATE_TEST_INDEX, testIndex.ordinal());
        savedInstanceState.putInt(STATE_FILE_INDEX, fileIndex);
        savedInstanceState.putInt(STATE_TEST_NUMBER, numberOfTests);
        savedInstanceState.putInt(STATE_CUR_LOOP_NUMBER, loopNumber);
        savedInstanceState.putSerializable(STATE_RESULT_TEST, (Serializable) resultsTest);
        savedInstanceState.putSerializable(STATE_LAST_TEST_INFO, lastTestInfo);
    }

    /**
     * Restore all the members of the current instance previously saved inside a Bundle
     *
     * @param savedInstanceState Bundle from which we retrieve said data
     */
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        testFiles = (List<MediaInfo>) savedInstanceState.getSerializable(STATE_TEST_FILES);
        testIndex = TEST_TYPES.values()[savedInstanceState.getInt(STATE_TEST_INDEX)];
        fileIndex = savedInstanceState.getInt(STATE_FILE_INDEX);
        numberOfTests = savedInstanceState.getInt(STATE_TEST_NUMBER);
        loopNumber = savedInstanceState.getInt(STATE_CUR_LOOP_NUMBER);
        resultsTest = (List<TestInfo>[]) savedInstanceState.getSerializable(STATE_RESULT_TEST);
        lastTestInfo = (TestInfo) savedInstanceState.getSerializable(STATE_LAST_TEST_INFO);
    }

    @Override
    protected void onResume() {
        if (running) {
            String name = testFiles.get(fileIndex).getName();
            updateTestProgress(name, fileIndex + 1, testFiles.size(), testIndex.ordinal() + 1, loopNumber + 1, numberOfTests);
            /* case where no screenshots */
            /* if screenshots, launchNextTest called from Screenshot Validation thread */
            if (!testIndex.isScreenshot()) {
                launchNextTest();
            }
        }
        super.onResume();
    }

}
