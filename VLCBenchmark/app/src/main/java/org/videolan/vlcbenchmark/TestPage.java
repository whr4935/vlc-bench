package org.videolan.vlcbenchmark;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import org.videolan.vlcbenchmark.service.BenchServiceDispatcher;
import org.videolan.vlcbenchmark.service.BenchServiceListener;
import org.videolan.vlcbenchmark.service.FAILURE_STATES;
import org.videolan.vlcbenchmark.service.MediaInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by noeldu_b on 7/11/16.
 */
public class TestPage extends Activity {

    private BenchServiceDispatcher dispatcher;
    private List<TestInfo>[] resultsTest;
    private List<MediaInfo> testFiles;
    private double softScore = 0;
    private double hardScore = 0;
    private TEST_TYPES testIndex = TEST_TYPES.SOFTWARE_SCREENSHOT;
    private int fileIndex = 0;
    private int loopNumber = 0;


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
    }

    private static final String SCREENSHOTS_EXTRA = "org.videolan.vlc.gui.video.benchmark.TIMESTAMPS";
    private static final String BENCH_ACTIVITY = "org.videolan.vlc.gui.video.benchmark.BenchActivity";
    private static final String BENCH_ACTION = "org.videolan.vlc.ACTION_BENCHMARK";

    private ProgressBar progressBar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_page);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setProgress(25);
        dispatcher = new BenchServiceDispatcher(new BenchServiceListener() {

            @Override
            public void failure(FAILURE_STATES reason, Exception exception) {
                Log.e("testPage", exception.toString());
            }

            @Override
            public void doneReceived(List<MediaInfo> files) {
                testFiles = files;
                TestPage.this.testIndex = TEST_TYPES.SOFTWARE_SCREENSHOT;
                MediaInfo currentFile = files.get(0);
                Intent intent = new Intent(BENCH_ACTION).setComponent(new ComponentName("org.videolan.vlc.debug", BENCH_ACTIVITY))
//                                        .setDataAndTypeAndNormalize(Uri.parse("file:/" + Uri.parse(currentFile.getLocalUrl())), "video/*") TODO use this line when vlc and vlc-benchmark have the same ID
                        .setDataAndTypeAndNormalize(Uri.parse("https://raw.githubusercontent.com/DaemonSnake/FileDump/master/" + currentFile.getUrl()), "video/*")
                        .putExtra("disable_hardware", true).putExtra(SCREENSHOTS_EXTRA, (Serializable) currentFile.getSnapshot());
                TestPage.this.startActivityForResult(intent, 42);
            }

            @Override
            public void updatePercent(final double percent) {
                progressBar.setProgress((int) Math.round(percent));
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable("TEST_FILES", (Serializable) testFiles);
        savedInstanceState.putInt("TEST_INDEX", testIndex.ordinal());
        savedInstanceState.putInt("FILE_INDEX", fileIndex);
        savedInstanceState.putInt("NUMBER_OF_TEST", numberOfTests);
        savedInstanceState.putInt("CURRENT_LOOP_NUMBER", loopNumber);
        savedInstanceState.putDouble("SOFT_SCORE", softScore);
        savedInstanceState.putDouble("HARD_SCORE", hardScore);
        savedInstanceState.putSerializable("RESULTS_TEST", (Serializable) resultsTest);
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
        softScore = savedInstanceState.getDouble("SOFT_SCORE");
        hardScore = savedInstanceState.getDouble("HARD_SCORE");
        resultsTest = (List<TestInfo>[]) savedInstanceState.getSerializable("RESULTS_TEST");
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    private TestInfo lastTestInfo = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != -1) {
            errorWhileTesting(resultCode);
            return;
        }

        if (testIndex.ordinal() == 0) {
            lastTestInfo = new TestInfo();
            lastTestInfo.name = testFiles.get(fileIndex).getName();
            lastTestInfo.loopNumber = loopNumber;
        }

        if (testIndex.isScreenshot()) {
            lastTestInfo.percentOfBadScreenshots += data.getIntExtra("number_of_bad_screenshots", 0);
            lastTestInfo.percentOfBadSeek += data.getDoubleExtra("percent_of_bad_seek", 0.0);
        }
        else
            lastTestInfo.percentOfFrameDrop += data.getDoubleExtra("dropped_frame", 0);

        if (testIndex == TEST_TYPES.HARDWARE_PLAYBACK) {
            lastTestInfo.percentOfFrameDrop /= 2.0;
            resultsTest[loopNumber].add(lastTestInfo);
            lastTestInfo = null;
            fileIndex++;
            if (fileIndex >= testFiles.size()) {
                loopNumber++;
                fileIndex = 0;
            }
            if (loopNumber >= numberOfTests) {
                Intent intent = new Intent(TestPage.this, ResultPage.class);
                intent.putExtra("resultsTest", (Serializable) resultsTest);
                intent.putExtra("soft", softScore);
                intent.putExtra("hard", hardScore);
                startActivity(intent);
                cleanState();
                return;
            }
        }
        testIndex = testIndex.next();
        MediaInfo currentFile = testFiles.get(fileIndex);
        Intent intent = new Intent(BENCH_ACTION).setComponent(new ComponentName("org.videolan.vlc.debug", BENCH_ACTIVITY))
//                .setDataAndTypeAndNormalize(Uri.parse("file:/" + Uri.parse(currentFile.getLocalUrl())), "video/*"); //TODO use this line when vlc and vlc-benchmark have the same ID
                .setDataAndTypeAndNormalize(Uri.parse("https://raw.githubusercontent.com/DaemonSnake/FileDump/master/" + currentFile.getUrl()), "video/*");

        if (testIndex.isSoftware()) {
            intent = intent.putExtra("disable_hardware", true);
        }
        if (testIndex.isScreenshot()) {
            intent = intent.putExtra(SCREENSHOTS_EXTRA, (Serializable) currentFile.getSnapshot());
        }
        startActivityForResult(intent, 42);
    }

    private void errorWhileTesting(int resultCode) {
        String errorMsg = null;
        switch (resultCode) {
            case 0:
                errorMsg = "No compatible cpu, incorrect VLC abi variant installed";
                break;
            case 2:
                errorMsg = "Connection failed to audio service";
                break;
            case 3:
                errorMsg = "VLC is not able to play this file, it could be incorrect path/uri, not supported codec or broken file";
                break;
            case 4:
                errorMsg = "Error with hardware acceleration, user refused to switch to software decoding";
                break;
            case 5:
                errorMsg = "VLC continues playback, but for audio track only. (Audio file detected or user chose to)";
                break;
            default:
                return;
        }
        onError("Error: VLC failed", errorMsg);
    }

    private void cleanState() {
        fileIndex = 0;
        testIndex = TEST_TYPES.SOFTWARE_SCREENSHOT;
        loopNumber = 0;
        for (int i = 0; i < numberOfTests; i++)
            resultsTest[i].clear();
        numberOfTests = 0;
        progressBar.setProgress(0);
        hardScore = 0;
        softScore = 0;
    }

    private void onError(String title, String message) {
        cleanState();
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setCancelable(false).setNeutralButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        }).setIcon(android.R.drawable.ic_dialog_alert).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dispatcher.stopService();
    }

    private int numberOfTests;

    private void launchTest() {
        try {
            dispatcher.startService(this);
        } catch (Exception e) {
            Log.e("testPage", "The service couldn't be started");
        }
    }

    public void testOne(View v) {
        numberOfTests = 1;
        resultsTest = new ArrayList[] {new ArrayList<MediaInfo>()};
        launchTest();
    }

    public void testThree(View v) {
        numberOfTests = 3;
        resultsTest = new ArrayList[] {new ArrayList<MediaInfo>(), new ArrayList<MediaInfo>(), new ArrayList<MediaInfo>()};
        launchTest();
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
            Log.e("VLCBenchmark", "Failed to get signatures");
            return false;
        }

        /* Checking to see if there is any signature */
        if (sigs != null) {
            if (sigs.length > 0) {
                benchSignature = sigs[0].hashCode();
            } else {
                Log.e("VLC - Benchmark", "No signatures");
                return false;
            }
        } else {
            Log.e("VLC - Benchmark", "No signatures");
            return false;
        }

        /* Getting vlc normal or debug package name, *
         * according to our application's state */
        if (benchPackageName.contains("debug")) {
            vlcPackageName = "org.videolan.vlc.debug";
        } else {
            vlcPackageName = "org.videolan.vlc";
        }

        /* Debug */
        Log.i("VLCBenchmark", "benchPackage = " + benchPackageName);
        Log.i("VLCBenchmark", "vlcPackage = " + vlcPackageName);

        /* Getting vlc's signature */
        try {
            sigs_vlc = this.getPackageManager().getPackageInfo(vlcPackageName, PackageManager.GET_SIGNATURES).signatures;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("VLCBenchmark", "Failed to get second signature");
            return false;
        }

        /* checking to see if there is are any signatures */
        if (sigs_vlc != null) {
            if (sigs_vlc.length > 0) {
                vlcSignature = sigs_vlc[0].hashCode();
            } else {
                Log.e("VLC - Benchmark", "No second signature");
                return false;
            }
        } else {
            Log.e("VLC - Benchmark", "No second signature");
            return false;
        }

        /* Debug */
        Log.i("VLCBenchmark", "benchSignature = " + benchSignature);
        Log.i("VLCBenchmark", "vlcSignature = " + vlcSignature);

        /* Checking to see if the signatures correspond */
        if (benchSignature != vlcSignature) {
            Log.e("VLC - Benchmark", "Aborting, the VLC and Benchmark application don't share signatures");
            return false;
        }

        /* Debug */
        Log.i("VLCBenchmark", "Both signatures are the same");
        try {
            if (this.getPackageManager().getPackageInfo(vlcPackageName, 0).versionName != "2.0.5") {
                Log.e("VLC - Benchmark", "Wrong VLC version number");
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("VLC - Benchmark", "Failed to get version name");
            return false;
        }

        return true;
    }
}