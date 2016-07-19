package org.videolan.vlcbenchmark.service;

/**
 * Created by penava_b on 18/07/16.
 */
public class BenchServiceAdapter implements BenchServiceListener {

    @Override
    public void checkSumFailed(Exception exception) {
    }

    @Override
    public void downloadFailed(Exception exception) {
    }

    @Override
    public void doneReceived(Score score) {
    }

    @Override
    public void testPassed(String testName) {
    }

    @Override
    public void filePassed(TestInfo fileName) {
    }

    @Override
    public void updatePercent(double purcent) {
    }
}
