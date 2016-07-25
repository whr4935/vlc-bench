package org.videolan.vlcbenchmark;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import org.videolan.vlcbenchmark.service.BenchServiceDispatcher;
import org.videolan.vlcbenchmark.service.BenchServiceListener;
import org.videolan.vlcbenchmark.service.FAILURE_STATES;
import org.videolan.vlcbenchmark.service.Score;
import org.videolan.vlcbenchmark.service.TestInfo;

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
        dispatcher.startService(this, 1);
    }

    public void testThree(View v) {
        dispatcher.startService(this, 3);
    }

}
