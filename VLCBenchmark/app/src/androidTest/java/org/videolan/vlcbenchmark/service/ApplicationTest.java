package org.videolan.vlcbenchmark.service;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.util.Log;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    private boolean stop = false;

    public ApplicationTest() throws Exception {
        super(Application.class);
    }

    @Override
    public void setUp() {
        Log.e("TEST::", "START...");
        BenchServiceDispatcher dispatcher = new BenchServiceDispatcher(new BenchServiceListener() {
            @Override
            public void checkSumFailed(Exception exception) {
                stop = true;
            }

            @Override
            public void downloadFailed(Exception exception) {
                stop = true;
            }

            @Override
            public void doneReceived(double score) {
                stop = true;
            }

            @Override
            public void testPassed(String testName) {
            }

            @Override
            public void filePassed(String fileName) {
                Log.e("TEST::FILE TESTED", fileName);
            }

            @Override
            public void updatePercent(double percent) {
                Log.e("TEST:: PERCENT!!", Double.toString(percent));
            }
        });
        dispatcher.startService(ApplicationTest.this.getContext(), 1);
        while (!stop)
            try {
                Thread.sleep(1000, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        Log.e("TEST::", "END OF TEST");
    }
}
