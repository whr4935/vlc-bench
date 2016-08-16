package org.videolan.vlcbenchmark;

import android.Manifest;
import android.app.Activity;
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
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.videolan.vlcbenchmark.service.BenchServiceDispatcher;
import org.videolan.vlcbenchmark.service.BenchServiceListener;
import org.videolan.vlcbenchmark.service.FAILURE_STATES;
import org.videolan.vlcbenchmark.service.MediaInfo;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by noeldu_b on 7/11/16.
 */
public class TestPage extends Activity implements BenchServiceListener {

    private BenchServiceDispatcher dispatcher;
    private List<TestInfo>[] resultsTest;
    private List<MediaInfo> testFiles;
    private TEST_TYPES testIndex = TEST_TYPES.SOFTWARE_SCREENSHOT;
    private int fileIndex = 0;
    private int loopNumber = 0;
    private StringBuilder logBuilder = new StringBuilder();
    private TestInfo lastTestInfo = null;
    private int numberOfTests;

    private TextView percentText = null;
    private TextView textLog = null;
    private Button oneTest = null,
            threeTests = null;
    private ProgressBar progressBar = null;

    enum TEST_TYPES {
        SOFTWARE_SCREENSHOT,
        SOFTWARE_PLAYBACK,
        HARDWARE_SCREENSHOT,
        HARDWARE_PLAYBACK;

        public TEST_TYPES next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public boolean isSoftware() {
            return (ordinal() / 2) % 2 == 0;
        }

        public boolean isScreenshot() {
            return ordinal() % 2 == 0;
        }

        @Override
        public String toString() {
            return super.toString().replace("_", " ").toLowerCase();
        }
    }

    private static final String VLC_PACKAGE_NAME = "org.videolan.vlc.debug";
    private static final String SCREENSHOTS_EXTRA = "org.videolan.vlc.gui.video.benchmark.TIMESTAMPS";
    private static final String BENCH_ACTIVITY = "org.videolan.vlc.gui.video.benchmark.BenchActivity";
    private static final String BENCH_ACTION = "org.videolan.vlc.ACTION_BENCHMARK";
    private static final String PROGRESS_TEXT_FORMAT = "%.2f %% | file %d/%d | test %d";
    private static final String PROGRESS_TEXT_FORMAT_LOOPS = PROGRESS_TEXT_FORMAT + " | loop %d/%d";
    private static final String SCREENSHOT_NAMING = "Screenshot_";
    private static final double MAX_SCREENSHOT_COLOR_DIFFERENCE_PERCENT = 2.5;
    private static final String SHARED_PREFERENCE = "org.videolab.vlc.gui.video.benchmark.UNCAUGHT_EXCEPTIONS";
    private static final String SHARED_PREFERENCE_STACK_TRACE = "org.videolab.vlc.gui.video.benchmark.STACK_TRACE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_page);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        percentText = (TextView) findViewById(R.id.percentText);
        oneTest = (Button) findViewById(R.id.benchOne);
        threeTests = (Button) findViewById(R.id.benchThree);
        textLog = (TextView) findViewById(R.id.extractEditText);

        dispatcher = new BenchServiceDispatcher(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
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

    @UiThread
    public void launchTests(View v) {
        int id = v.getId();

        if (id == R.id.benchOne) {
            numberOfTests = 1;
            resultsTest = new ArrayList[]{new ArrayList<MediaInfo>()};
        } else if (id == R.id.benchThree) {
            numberOfTests = 3;
            resultsTest = new ArrayList[]{new ArrayList<MediaInfo>(), new ArrayList<MediaInfo>(), new ArrayList<MediaInfo>()};
        } else
            return;

        fileIndex = 0;
        testIndex = TEST_TYPES.SOFTWARE_SCREENSHOT;
        loopNumber = 0;
        progressBar.setProgress(0);
        progressBar.setMax(100);
        percentText.setText(R.string.default_percent_value);
        logBuilder = new StringBuilder();
        textLog.setText(logBuilder.toString());

        try {
            dispatcher.startService(this);
        } catch (RuntimeException e) {
            new AlertDialog.Builder(this).setTitle("Please wait").setMessage("VLC will start shortly").setNeutralButton(android.R.string.ok, null).show();
        }
    }

    @Override
    public void updatePercent(double percent, long bitRate) {
        progressBar.setProgress((int) Math.round(percent));
        if (bitRate == BenchServiceDispatcher.NO_BITRATE)
            percentText.setText(String.format("%.2f %%", percent));
        else
            percentText.setText(String.format("%.2f %% (%s)", percent, bitRateToString(bitRate)));
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
        new AlertDialog.Builder(this).setTitle("Error during download").setMessage("An exception occurred while downloading:\n" + exception.toString()).setNeutralButton("ok", null).show();
    }

    @Override
    public void doneReceived(List<MediaInfo> files) {
        testFiles = files;
        TestPage.this.testIndex = TEST_TYPES.SOFTWARE_SCREENSHOT;
        MediaInfo currentFile = files.get(0);
        Intent intent = new Intent(BENCH_ACTION).setComponent(new ComponentName(VLC_PACKAGE_NAME, BENCH_ACTIVITY))
//                                        .setDataAndTypeAndNormalize(Uri.parse("file:/" + Uri.parse(currentFile.getLocalUrl())), "video/*") //TODO use this line when vlc and vlc-benchmark have the same ID
                .setDataAndTypeAndNormalize(Uri.parse("https://raw.githubusercontent.com/DaemonSnake/FileDump/master/" + currentFile.getUrl()), "video/*")
                .putExtra("disable_hardware", true).putExtra(SCREENSHOTS_EXTRA, (Serializable) currentFile.getSnapshot());
        oneTest.setVisibility(View.INVISIBLE);
        threeTests.setVisibility(View.INVISIBLE);
        try {
            startActivityForResult(intent, 42);
        } catch (ActivityNotFoundException e) {
            //TODO or not, should be taken care of beforehand
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (fileIndex == 0 && testIndex == TEST_TYPES.SOFTWARE_SCREENSHOT) {
            progressBar.setProgress(0);
            progressBar.setMax(TEST_TYPES.values().length * testFiles.size() * numberOfTests);
        }
        super.onActivityResult(requestCode, resultCode, data);

        if (testIndex.ordinal() == 0) {
            lastTestInfo = new TestInfo();
            lastTestInfo.name = testFiles.get(fileIndex).getName();
            lastTestInfo.loopNumber = loopNumber;
            logBuilder.append(lastTestInfo.name).append('\n');
        }

        progressBar.incrementProgressBy(1);
        if (numberOfTests != 1)
            percentText.setText(String.format(PROGRESS_TEXT_FORMAT_LOOPS, progressBar.getProgress() * 100.0 / progressBar.getMax(), fileIndex + 1, testFiles.size(), testIndex.ordinal() + 1,
                    loopNumber + 1, numberOfTests));
        else
            percentText.setText(String.format(PROGRESS_TEXT_FORMAT, progressBar.getProgress() * 100.0 / progressBar.getMax(), fileIndex + 1, testFiles.size(), testIndex.ordinal() + 1));
        logBuilder.append(String.format("        %s tests %s\n", testIndex.toString(), (resultCode == RESULT_OK ? "finished" : "failed")));
        textLog.setText(logBuilder.toString());

        if (data != null && resultCode == -1) {
            fillCurrentTestInfo(data, false);
            return;
        }

        String errorMessage;
        if (data == null) {
            try {
                Context packageContext = createPackageContext(VLC_PACKAGE_NAME, 0);
                SharedPreferences preferences = packageContext.getSharedPreferences(SHARED_PREFERENCE, Context.MODE_PRIVATE);
                errorMessage = preferences.getString(SHARED_PREFERENCE_STACK_TRACE, null);
            } catch (PackageManager.NameNotFoundException e) {
                errorMessage = e.getMessage();
            }
        } else
            errorMessage = vlcErrorCodeToString(resultCode);

        new AlertDialog.Builder(this) {{
            setTitle("VLC crashed on test");
            setCancelable(false);
        }}
                .setMessage(errorMessage)
                .setNeutralButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                fillCurrentTestInfo(data, true);
                            }
                        })
                .show();
    }

    private String vlcErrorCodeToString(int resultCode) {
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
        }
        return "Unknown error code";
    }

    private void fillCurrentTestInfo(Intent data, boolean failed) {
        if (failed) {
            lastTestInfo.vlcCrashed(testIndex.isSoftware(), testIndex.isScreenshot());
        } else if (testIndex.isScreenshot()) {
            testScreenshot(data);
            return;
        } else {
            lastTestInfo.badFrames(data.getIntExtra("number_of_dropped_frames", 0), testIndex.isSoftware());
        }
        launchNextTest();
    }

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
                Intent intent = new Intent(TestPage.this, ResultPage.class);
                intent.putExtra("resultsTest", resultsTest);
                double softScore = 0, hardScore = 0;
                for (int i = 0; i < numberOfTests; i++)
                    for (TestInfo test : resultsTest[i]) {
                        softScore += test.software[TestInfo.PLAYBACK] + test.software[TestInfo.PERFORMANCE];
                        hardScore += test.hardware[TestInfo.PLAYBACK] + test.hardware[TestInfo.PERFORMANCE];
                    }
                int totalNumberOfElement = testFiles.size() * numberOfTests;
                softScore /= totalNumberOfElement;
                hardScore /= totalNumberOfElement;
                intent.putExtra("soft", softScore);
                intent.putExtra("hard", hardScore);
                oneTest.setVisibility(View.VISIBLE);
                threeTests.setVisibility(View.VISIBLE);
                startActivity(intent);
                return;
            }
        }
        testIndex = testIndex.next();
        MediaInfo currentFile = testFiles.get(fileIndex);
        Intent intent = new Intent(BENCH_ACTION).setComponent(new ComponentName(VLC_PACKAGE_NAME, BENCH_ACTIVITY))
//                .setDataAndTypeAndNormalize(Uri.parse("file:/" + Uri.parse(currentFile.getLocalUrl())), "video/*"); //TODO use this line when vlc and vlc-benchmark have the same ID
                .setDataAndTypeAndNormalize(Uri.parse("https://raw.githubusercontent.com/DaemonSnake/FileDump/master/" + currentFile.getUrl()), "video/*");

        if (testIndex.isSoftware())
            intent = intent.putExtra("disable_hardware", true);
        if (testIndex.isScreenshot())
            intent = intent.putExtra(SCREENSHOTS_EXTRA, (Serializable) currentFile.getSnapshot());
        startActivityForResult(intent, 42);
    }

    private boolean checkSignature() {
        String benchPackageName = this.getPackageName();
        String vlcPackageName;
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

        /* Getting vlc normal or debug package name, *
         * according to our application's state */
        if (benchPackageName.contains("debug")) {
            vlcPackageName = VLC_PACKAGE_NAME;
        } else {
            vlcPackageName = "org.videolan.vlc";
        }

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

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable("TEST_FILES", (Serializable) testFiles);
        savedInstanceState.putInt("TEST_INDEX", testIndex.ordinal());
        savedInstanceState.putInt("FILE_INDEX", fileIndex);
        savedInstanceState.putInt("NUMBER_OF_TEST", numberOfTests);
        savedInstanceState.putInt("CURRENT_LOOP_NUMBER", loopNumber);
        savedInstanceState.putSerializable("RESULTS_TEST", (Serializable) resultsTest);
        savedInstanceState.putSerializable("LAST_TEST_INFO", lastTestInfo);
        savedInstanceState.putSerializable("LOG_TEXT", logBuilder.toString());
        savedInstanceState.putInt("PROGRESS_VALUE", progressBar.getProgress());
        savedInstanceState.putInt("PROGRESS_MAX", progressBar.getMax());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        testFiles = (List<MediaInfo>) savedInstanceState.getSerializable("TEST_FILES");
        testIndex = TEST_TYPES.values()[savedInstanceState.getInt("TEST_INDEX")];
        fileIndex = savedInstanceState.getInt("FILE_INDEX");
        numberOfTests = savedInstanceState.getInt("NUMBER_OF_TEST");
        loopNumber = savedInstanceState.getInt("CURRENT_LOOP_NUMBER");
        logBuilder = new StringBuilder((String) savedInstanceState.getSerializable("LOG_TEXT"));
        lastTestInfo = (TestInfo) savedInstanceState.getSerializable("LAST_TEST_INFO");
        resultsTest = (List<TestInfo>[]) savedInstanceState.getSerializable("RESULTS_TEST");
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setProgress(savedInstanceState.getInt("PROGRESS_VALUE", 0));
        progressBar.setMax(savedInstanceState.getInt("PROGRESS_MAX", 100));
        percentText = (TextView) findViewById(R.id.percentText);
        oneTest = (Button) findViewById(R.id.benchOne);
        threeTests = (Button) findViewById(R.id.benchThree);
        textLog = (TextView) findViewById(R.id.extractEditText);
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    protected void onDestroy() {
        if (dispatcher != null) {
            dispatcher.stopService();
            dispatcher = null;
        }
        super.onDestroy();
    }
}
