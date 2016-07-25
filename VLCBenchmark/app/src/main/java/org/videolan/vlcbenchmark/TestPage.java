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
import org.videolan.vlcbenchmark.service.Score;
import org.videolan.vlcbenchmark.service.TestInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_page);

        resultsTestOne = new ArrayList<>();
        resultsTestTwo = new ArrayList<>();
        resultsTestThree = new ArrayList<>();
        final ProgressBar pb = (ProgressBar)findViewById(R.id.progressBar);
        pb.setProgress(25);
        dispatcher = new BenchServiceDispatcher(new BenchServiceListener() {

            @Override
            public void failure(FAILURE_STATES reason, Exception exception) {
                Log.e("testPage", exception.toString());
            }

            @Override
            public void doneReceived(Score score) {
                Intent intent = new Intent(TestPage.this, ResultPage.class);
                intent.putExtra("resultsTestOne", (ArrayList<TestInfo>) resultsTestOne);
                intent.putExtra("resultsTestTwo", (ArrayList<TestInfo>) resultsTestTwo);
                intent.putExtra("resultsTestThree", (ArrayList<TestInfo>) resultsTestThree);
                intent.putExtra("soft", softScore);
                intent.putExtra("hard", hardScore);
                startActivity(intent);
            }

            @Override
            public void testPassed(String testName) {

            }

            @Override
            public void filePassed(TestInfo info) {
                switch (info.getLoopNumber()) {
                    case 0:
                        synchronized (resultsTestOne) {
                            resultsTestOne.add(info);
                        }
                        break;
                    case 1:
                        synchronized (resultsTestTwo) {
                            resultsTestTwo.add(info);
                        }
                        break;
                    default:
                        synchronized (resultsTestThree) {
                            resultsTestThree.add(info);
                        }
                        break;
                }
            }

            @Override
            public void updatePercent(double percent) {
                pb.setProgress((int)Math.round(percent));
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        dispatcher.stopService();
    }

    public void testOne(View v) {
        try {
            dispatcher.startService(this, 1);
        }
        catch (Exception e) {
            Log.e("testPage", "The service couldn't be started");
        }
    }

    public void testThree(View v) {
        try {
            dispatcher.startService(this, 3);
        }
        catch (Exception e) {
            Log.e("testPage", "The service couldn't be started");
        }
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
        }

        /* Checking to see if there is any signature */
        if (sigs != null) {
            if (sigs.length > 0) {
                benchSignature = sigs[0].hashCode();
            } else {
                return false;
            }
        } else {
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
        }

        /* checking to see if there is are any signatures */
        if (sigs_vlc != null) {
            if (sigs_vlc.length > 0) {
                vlcSignature = sigs_vlc[0].hashCode();
            } else {
                return false;
            }
        } else {
            return false;
        }

        /* Debug */
        Log.i("VLCBenchmark", "benchSignature = " + benchSignature);
        Log.i("VLCBenchmark", "vlcSignature = " + vlcSignature);

        /* Checking to see if the signatures correspond */
        if (benchSignature != vlcSignature) {
            return false;
        }

        /* Debug */
        Log.i("VLCBenchmark", "Both signatures are the same");

        return true;
    }
}

