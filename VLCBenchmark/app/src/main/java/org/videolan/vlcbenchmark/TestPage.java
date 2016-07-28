package org.videolan.vlcbenchmark;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import org.videolan.vlcbenchmark.service.BenchServiceDispatcher;
import org.videolan.vlcbenchmark.service.BenchServiceListener;
import org.videolan.vlcbenchmark.service.FAILURE_STATES;
import org.videolan.vlcbenchmark.service.MediaInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by noeldu_b on 7/11/16.
 */
public class TestPage extends Activity {

    private BenchServiceDispatcher dispatcher;
    private List<TestInfo> resultsTestOne;
    private List<TestInfo> resultsTestTwo;
    private List<TestInfo> resultsTestThree;
    private double softScore = 0;
    private double hardScore = 0;
    private ProgressBar progressBar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_page);

        resultsTestOne = new ArrayList<>();
        resultsTestTwo = new ArrayList<>();
        resultsTestThree = new ArrayList<>();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setProgress(25);
        dispatcher = new BenchServiceDispatcher(new BenchServiceListener() {

            @Override
            public void failure(FAILURE_STATES reason, Exception exception) {
                Log.e("testPage", exception.toString());
            }

            @Override
            public void doneReceived(List<MediaInfo> files) {
                Intent intent = new Intent(TestPage.this, ResultPage.class);
                intent.putExtra("resultsTestOne", (ArrayList<TestInfo>) resultsTestOne);
                intent.putExtra("resultsTestTwo", (ArrayList<TestInfo>) resultsTestTwo);
                intent.putExtra("resultsTestThree", (ArrayList<TestInfo>) resultsTestThree);
                intent.putExtra("soft", softScore);
                intent.putExtra("hard", hardScore);
                startActivity(intent);
            }

            @Override
            public void updatePercent(double percent) {
                progressBar.setProgress((int) Math.round(percent));
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        dispatcher.stopService();
    }

    private int numberOfTests;

    private void launchTest()
    {
        try {
            dispatcher.startService(this);
        }
        catch (Exception e) {
            Log.e("testPage", "The service couldn't be started");
        }
    }

    public void testOne(View v) {
        numberOfTests = 1;
        launchTest();
    }

    public void testThree(View v) {
        numberOfTests = 3;
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

