package org.videolan.vlcbenchmark.service;

/**
 * Created by penava_b on 18/07/16.
 */
public interface BenchServiceListener {
    void checkSumFailed(Exception exception);
    void downloadFailed(Exception exception);
    void doneReceived(double score);
    void testPassed(String testName);
    void filePassed(String fileName);
    void updatePercent(double purcent);

}
