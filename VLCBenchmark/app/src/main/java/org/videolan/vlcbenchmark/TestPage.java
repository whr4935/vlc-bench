package org.videolan.vlcbenchmark;

import android.content.Intent;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import junit.framework.Test;

import org.videolan.vlcbenchmark.R;
import org.videolan.vlcbenchmark.service.BenchService;
import org.videolan.vlcbenchmark.service.BenchServiceAdapter;
import org.videolan.vlcbenchmark.service.BenchServiceDispatcher;
import org.videolan.vlcbenchmark.service.BenchServiceListener;
import org.videolan.vlcbenchmark.service.Score;
import org.videolan.vlcbenchmark.service.TestInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by noeldu_b on 7/11/16.
 */
public class TestPage extends AppCompatActivity {

    private BenchServiceDispatcher dispatcher;
    private double progress = 0;
    private List<TestInfo> resultsTestOne;
    private List<TestInfo> resultsTestTwo;
    private List<TestInfo> resultsTestThree;
    private double softScore;
    private double hardScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_page);

        resultsTestOne = new ArrayList<>();
        resultsTestTwo = new ArrayList<>();
        resultsTestThree = new ArrayList<>();
        final ProgressBar pb = (ProgressBar)findViewById(R.id.progressBar);
        dispatcher = new BenchServiceDispatcher(new BenchServiceListener() {
            @Override
            public void checkSumFailed(Exception exception) {

            }

            @Override
            public void downloadFailed(Exception exception) {

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
                synchronized (resultsTestOne) {
                    resultsTestOne.add(info);
                }
            }

            @Override
            public void updatePercent(double percent) {
                progress = percent;
                pb.setProgress((int)Math.round(percent));
            }
        });
    }

    public void testOne(View v) {
        dispatcher.startService(this, 1);
    }

    public void testThree(View v) {
        dispatcher.startService(this, 3);
    }

}
