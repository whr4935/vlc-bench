/*****************************************************************************
 * VLCWorkerModel.java
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
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.videolan.vlcbenchmark.service.BenchService;
import org.videolan.vlcbenchmark.service.BenchServiceDispatcher;
import org.videolan.vlcbenchmark.service.BenchServiceListener;
import org.videolan.vlcbenchmark.service.MediaInfo;
import org.videolan.vlcbenchmark.tools.CrashHandler;
import org.videolan.vlcbenchmark.tools.GoogleConnectionHandler;
import org.videolan.vlcbenchmark.tools.ScreenshotValidator;
import org.videolan.vlcbenchmark.tools.TestInfo;

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
public abstract class VLCWorkerModel extends AppCompatActivity implements BenchServiceListener {

    /**
     * We use this member to start, stop and listene to {@link org.videolan.vlcbenchmark.service.BenchService}
     */
    private BenchServiceDispatcher dispatcher;
    private List<TestInfo>[] resultsTest;
    private List<MediaInfo> testFiles;
    private TEST_TYPES testIndex = TEST_TYPES.SOFTWARE_SCREENSHOT;
    private int fileIndex = 0;
    private int loopNumber = 0;
    private TestInfo lastTestInfo = null;
    protected int numberOfTests;

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

    private static final String VLC_PACKAGE_NAME = "org.videolan.vlc";
    private static final String VLC_DEBUG_PACKAGE_NAME = "org.videolan.vlc.debug";
    private static final String SCREENSHOTS_EXTRA = "org.videolan.vlc.gui.video.benchmark.TIMESTAMPS";
    private static final String BENCH_ACTIVITY = "org.videolan.vlc.gui.video.benchmark.BenchActivity";
    private static final String SCREENSHOT_ACTION = "org.videolan.vlc.gui.video.benchmark.ACTION_SCREENSHOTS";
    private static final String PLAYBACK_ACTION = "org.videolan.vlc.gui.video.benchmark.ACTION_PLAYBACK";
    private static final String SCREENSHOT_NAMING = "Screenshot_";
    private static final double MAX_SCREENSHOT_COLOR_DIFFERENCE_PERCENT = 2.5;
    private static final String SHARED_PREFERENCE = "org.videolab.vlc.gui.video.benchmark.UNCAUGHT_EXCEPTIONS";
    private static final String SHARED_PREFERENCE_STACK_TRACE = "org.videolab.vlc.gui.video.benchmark.STACK_TRACE";
    private static final String SHARED_PREFERENCE_WARNING = "org.videolan.vlc.gui.video.benchmark.WARNING";
    private static final String WARNING_MESSAGE = "VLCBenchmark will extensively test your phone's video capabilities." +
            "\n\nIt will download a large amount of files and will run for several hours." +
            "\nFurthermore, it will need the permission to access external storage";

    private String vlcPackageName;

    public abstract void doneDownload();

    /**
     * Is called during the {@link VLCWorkerModel#onCreate(Bundle)}.
     */
    protected abstract void setupUiMembers(Bundle savedInstanceState);

    /**
     *  Is called in {@link VLCWorkerModel#launchTests(int)}.
     */
    protected abstract void resetUiToDefault();

    /**
     * Is called once that service has finished successfully.
     */
    protected abstract void updateUiOnServiceDone();

    /**
     * Is called before the first launch of VLC.
     * @param totalNumberOfElements the total number of tests that will occur from then.
     */
    protected abstract void initVlcProgress(int totalNumberOfElements);

    /**
     * Is called when a new file just started to be tested.
     * @param fileName the name of the file
     */
    protected abstract void onFileTestStarted(String fileName);

    /**
     * Is called once a single test is finished.
     * @param testName the name of the test (ex : screenshot software, ...)
     * @param succeeded status of the test
     * @param fileIndex the index of the current file
     * @param numberOfFiles the total number of files
     * @param testNumber the index of the current test ({@link TEST_TYPES#ordinal()}
     * @param loopNumber the number of times we've repeated all tests
     * @param numberOfLoops the total number of time we have to repeat
     */
    protected abstract void onSingleTestFinished(String testName, boolean succeeded, int fileIndex, int numberOfFiles, int testNumber, int loopNumber, int numberOfLoops);

    /**
     * Is called if VLC stopped due to an uncaught exception while testing.
     *
     * @param errorMessage a String representing the issue that caused VLC to crash.
     * @param resume needs to be called if the implementation needs to continue testing other files.
     */
    protected abstract void onVlcCrashed(String errorMessage, Runnable resume);

    /**
     * Is called when all tests are finished
     * @param results a array list of {@link TestInfo} where each element of the array represents a loop and each element of the List the results of for a file.
     */
    protected abstract void onTestsFinished(List<TestInfo>[] results);

    /**
     * Is called in {@link VLCWorkerModel#onSaveInstanceState(Bundle)}
     * @param saveInstanceState the Bundle to use to save UI data states
     */
    protected abstract void onSaveUiData(Bundle saveInstanceState);

    /**
     * Is called in {@link VLCWorkerModel#onRestoreInstanceState(Bundle)}
     * @param saveInstanceState the Bundle to use to restore UI data states
     */
    protected abstract void onRestoreUiData(Bundle saveInstanceState);

    /**
     * Initialization of the Activity.
     *
     * This calls {@link #setupUiMembers(Bundle savedInstanceState)}, sets up the {@link BenchServiceDispatcher} dispatcher member and request
     * permissions to read on the external storage.
     *
     * @param savedInstanceState
     */
    @Override
    final protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CrashHandler.setCrashHandler();

        dispatcher = new BenchServiceDispatcher(this);

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        boolean hasWarned = sharedPref.getBoolean(SHARED_PREFERENCE_WARNING, false);

        Intent intent = new Intent(this, BenchService.class);
        intent.putExtra("action", ServiceActions.SERVICE_CHECKFILES);
        dispatcher.startService(this, intent);

        setupUiMembers(savedInstanceState);

        if (!hasWarned) {
            new AlertDialog.Builder(this).setTitle("WARNING").setMessage(WARNING_MESSAGE).setNeutralButton(android.R.string.ok, null).show();
            SharedPreferences.Editor editor= sharedPref.edit();
            editor.putBoolean(SHARED_PREFERENCE_WARNING, true);
            editor.apply();
        }

        /* Getting vlc normal or debug package name, *
         * according to our application's state */
        if (BuildConfig.DEBUG) {
            vlcPackageName = VLC_DEBUG_PACKAGE_NAME;
        } else {
            vlcPackageName = VLC_PACKAGE_NAME;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        GoogleConnectionHandler.getInstance();
    }

    /**
     * This methods will be called once the user authorized the application to read files on the external storage.
     * If he didn't we create a dialog and kill the application once that dialog has been closed.
     *
     * @param requestCode ignored
     * @param permissions ignored
     * @param grantResults use this to check whether or not the {@link android.Manifest.permission#READ_EXTERNAL_STORAGE} permission has been granted.
     */
    @Override
    final public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // check for grantResult size. On some devices this callback is called before responding to the dialog
        if (requestCode == 1 && grantResults.length >= 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                AlertDialog dialog = new AlertDialog.Builder(this).setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                }).create();
                dialog.setCancelable(false);
                dialog.setTitle("Bad permission");
                dialog.setMessage("Cannot proceed without asked permission.\n\nExiting...");
                dialog.show();
        }
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
    final public void launchTests(int numberOfTests) {
            if (numberOfTests == 1) {
            this.numberOfTests = 1;
            resultsTest = new ArrayList[]{new ArrayList<MediaInfo>()};
        } else if (numberOfTests == 3) {
            this.numberOfTests = 3;
            resultsTest = new ArrayList[]{new ArrayList<MediaInfo>(), new ArrayList<MediaInfo>(), new ArrayList<MediaInfo>()};
        } else
            return;

        fileIndex = 0;
        testIndex = TEST_TYPES.SOFTWARE_SCREENSHOT;
        loopNumber = 0;
        resetUiToDefault();

        MediaInfo currentFile = testFiles.get(0);
        updateUiOnServiceDone();
        try {
            startActivityForResult(createIntentForVlc(currentFile), RequestCodes.VLC);
        } catch (ActivityNotFoundException e) {
            Log.e("VLCBench", "Failed to start VLC");
            //TODO or not, should be taken care of beforehand
        }
    }

    /**
     * Method called when {@link org.videolan.vlcbenchmark.service.BenchService} has finished his task
     * It call {@link VLCWorkerModel#updateUiOnServiceDone} and starts for the first time VLC.
     *
     * @param files list of metadata for all the video/media to test.
     */
    @Override
    final public void doneReceived(List<MediaInfo> files) {
        testFiles = files;
        testIndex = TEST_TYPES.SOFTWARE_SCREENSHOT;
        setDownloaded(true);
    }

    /**
     * This method creates a new intent that corresponds with VLC's BenchActivity launch protocol.
     * @param currentFile metadata about the current file
     * @return a new Intent
     */
    private Intent createIntentForVlc(MediaInfo currentFile) {
        Intent intent = new Intent(testIndex.isScreenshot() ? SCREENSHOT_ACTION : PLAYBACK_ACTION)
                .setComponent(new ComponentName(vlcPackageName, BENCH_ACTIVITY))
                .setDataAndTypeAndNormalize(Uri.parse("file://" + Uri.parse(currentFile.getLocalUrl())), "video/*");
//                .setDataAndTypeAndNormalize(Uri.parse("https://raw.githubusercontent.com/Skantes/FileDump/master/" + currentFile.getUrl()), "video/*");
        if (testIndex.isSoftware())
            intent = intent.putExtra("disable_hardware", true);
        if (testIndex.isScreenshot())
            intent = intent.putExtra(SCREENSHOTS_EXTRA, (Serializable) currentFile.getSnapshot());
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
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
         if (requestCode == RequestCodes.VLC) {
            if (fileIndex == 0 && testIndex == TEST_TYPES.SOFTWARE_SCREENSHOT) {
                initVlcProgress(TEST_TYPES.values().length * testFiles.size() * numberOfTests);
            }
            super.onActivityResult(requestCode, resultCode, data);

            if (testIndex.ordinal() == 0) {
                String name = testFiles.get(fileIndex).getName();
                lastTestInfo = new TestInfo(name, loopNumber);
                onFileTestStarted(name);
            }
            onSingleTestFinished(testIndex.toString(), resultCode == RESULT_OK, fileIndex + 1, testFiles.size(), testIndex.ordinal() + 1, loopNumber + 1, numberOfTests);

            if (data != null && resultCode == -1) {
                fillCurrentTestInfo(data, false, resultCode);
                return;
            }
            if (data == null && resultCode != ResultCodes.RESULT_OK) {
                fillCurrentTestInfo(data, true, resultCode);
                return;
            }
            String errorMessage;
            if (data == null) {
                try {
                    Context packageContext = createPackageContext(vlcPackageName, 0);
                    SharedPreferences preferences = packageContext.getSharedPreferences(SHARED_PREFERENCE, Context.MODE_PRIVATE);
                    errorMessage = preferences.getString(SHARED_PREFERENCE_STACK_TRACE, null);
                } catch (PackageManager.NameNotFoundException e) {
                    errorMessage = e.getMessage();
                }
            } else {
                errorMessage = vlcErrorCodeToString(resultCode, data);
            }

            onVlcCrashed(errorMessage, new Runnable() {
                @Override
                public void run() {
                    fillCurrentTestInfo(data, true, ResultCodes.RESULT_VLC_CRASH);
                }
            });
        } else if (requestCode == RequestCodes.GOOGLE_CONNECTION) {
             GoogleConnectionHandler.getInstance().handleSignInResult(data);
         }
    }

    /**
     * Find the appropriate error message according to the result code and Intent.
     *
     * @param resultCode the return code of VLC
     * @param data       the Intent received from VLC
     * @return The String associated with the code given or the String given through the Intent if the result code is equal to 6
     */
    private String vlcErrorCodeToString(int resultCode, Intent data) {
        switch (resultCode) {
            case 0:
                return "No compatible cpu, incorrect VLC abi variant installed";
            case 2:
                return "Connection failed to audio service";
            case 3:
                return "VLC is not able to play this file, it could be incorrect path/uri, not supported codec or broken file";
            case 4:
                return "Error with hardware acceleration, user refused to switch to software decoding";
            case 5:
                return "VLC continues playback, but for audio track only. (Audio file detected or user chose to)";
            case 6:
                return (data != null ? data.getStringExtra("Error") : "VLC's BenchActivity error");
        }
        return "Unknown error code";
    }

    /**
     * Small factoring function.
     *
     * @param data   Intent contained results from VLC.
     * @param failed boolean to know if the interpretation of the result of code of VLC indicated that VLC crashed.
     */
    private void fillCurrentTestInfo(Intent data, boolean failed, int resultCode) {
        if (failed) {
            lastTestInfo.vlcCrashed(testIndex.isSoftware(), testIndex.isScreenshot(), resultCode);
        } else if (testIndex.isScreenshot()) {
            testScreenshot(data);
            return;
        } else {
            lastTestInfo.badFrames(data.getIntExtra("number_of_dropped_frames", 0), testIndex.isSoftware());
        }
        launchNextTest();
    }

    /**
     * This method is called once a screenshot test is finished.
     * It spawns a new thread that will iterates over the screenshots
     * and check their existence and validity.
     * <p>
     * Every time said conditions are not met a counter is incremented.
     * At the end of the Thread we update call {@link TestInfo#badScreenshot(double, boolean)} with said number
     * and call {@link VLCWorkerModel#launchTests(int)} on the UI thread.
     *
     * @param data the Intent from which we get in which folder the screenshots are located.
     */
    private void testScreenshot(Intent data) {
        final String screenshotFolder = data.getStringExtra("screenshot_folder"); //TODO replace with SharedPreference
        final int numberOfScreenshot = testFiles.get(fileIndex).getColors().size();
        final List<Integer> colors = testFiles.get(fileIndex).getColors();

        new Thread() {
            @Override
            public void run() {
                int badScreenshots = 0;
                for (int i = 0; i < numberOfScreenshot; i++) {
                    String filePath = screenshotFolder + File.separator + SCREENSHOT_NAMING + i + ".jpg";
                    File file = new File(filePath);
                    boolean exists;
                    if (!(exists = file.exists()) ||
                            ScreenshotValidator.getValidityPercent(filePath, colors.get(i)) >= MAX_SCREENSHOT_COLOR_DIFFERENCE_PERCENT) {
                        badScreenshots++;
                    }
                    if (exists)
                        file.delete();
                }
                lastTestInfo.badScreenshot(100.0 * badScreenshots / numberOfScreenshot, testIndex.isSoftware());
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
        if (testIndex == TEST_TYPES.HARDWARE_PLAYBACK) {
            resultsTest[loopNumber].add(lastTestInfo);
            lastTestInfo = null;
            fileIndex++;
            if (fileIndex >= testFiles.size()) {
                loopNumber++;
                fileIndex = 0;
            }
            if (loopNumber >= numberOfTests) {
                onTestsFinished(resultsTest);
                return;
            }
        }
        testIndex = testIndex.next();
        MediaInfo currentFile = testFiles.get(fileIndex);
        startActivityForResult(createIntentForVlc(currentFile), RequestCodes.VLC);
    }

    /**
     * Tool method to check if VLC's signature and ours match.
     *
     * @return true if VLC's signature matches our else false
     */
    private boolean checkSignature() {
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
            sigs_vlc = this.getPackageManager().getPackageInfo(vlcPackageName, PackageManager.GET_SIGNATURES).signatures;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        /* checking to see if there is are any signatures */
        if (sigs_vlc != null && sigs_vlc.length > 0)
            vlcSignature = sigs_vlc[0].hashCode();
        else
            return false;

        /* Checking to see if the signatures correspond */
        if (benchSignature != vlcSignature)
            return false;

        try {
            if (this.getPackageManager().getPackageInfo(vlcPackageName, 0).versionName.equals("2.0.5"))
                return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        return true;
    }

    /**
     * Save all the fields of the current instance
     *
     * @param savedInstanceState Bundle in which we save said data
     */
    @Override
    final public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable("TEST_FILES", (Serializable) testFiles);
        savedInstanceState.putInt("TEST_INDEX", testIndex.ordinal());
        savedInstanceState.putInt("FILE_INDEX", fileIndex);
        savedInstanceState.putInt("NUMBER_OF_TEST", numberOfTests);
        savedInstanceState.putInt("CURRENT_LOOP_NUMBER", loopNumber);
        savedInstanceState.putSerializable("RESULTS_TEST", (Serializable) resultsTest);
        savedInstanceState.putSerializable("LAST_TEST_INFO", lastTestInfo);
        onSaveUiData(savedInstanceState);
    }

    /**
     * Restore all the members of the current instance previously saved inside a Bundle
     *
     * @param savedInstanceState Bundle from which we retrieve said data
     */
    @Override
    final public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        testFiles = (List<MediaInfo>) savedInstanceState.getSerializable("TEST_FILES");
        testIndex = TEST_TYPES.values()[savedInstanceState.getInt("TEST_INDEX")];
        fileIndex = savedInstanceState.getInt("FILE_INDEX");
        numberOfTests = savedInstanceState.getInt("NUMBER_OF_TEST");
        loopNumber = savedInstanceState.getInt("CURRENT_LOOP_NUMBER");
        lastTestInfo = (TestInfo) savedInstanceState.getSerializable("LAST_TEST_INFO");
        resultsTest = (List<TestInfo>[]) savedInstanceState.getSerializable("RESULTS_TEST");
        onRestoreUiData(savedInstanceState);
    }

    /**
     * Make sure that the service was stopped when the Activity is destroyed.
     */
    @Override
    @CallSuper
    protected void onDestroy() {
        if (dispatcher != null) {
            dispatcher.stopService();
            dispatcher = null;
        }
        super.onDestroy();
    }

}
