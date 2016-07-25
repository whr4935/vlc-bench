package org.videolan.vlcbenchmark.service;

/**
 * Created by penava_b on 18/07/16.
 */
public interface BenchServiceListener {

    void failure(FAILURE_STATES reason, Exception exception);
    void doneReceived(Score score);
    void testPassed(String testName);
    void filePassed(TestInfo fileName);
    void updatePercent(double purcent);
}
