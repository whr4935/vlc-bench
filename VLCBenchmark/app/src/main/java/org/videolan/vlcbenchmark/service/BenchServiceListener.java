package org.videolan.vlcbenchmark.service;

import java.util.List;

/**
 * Created by penava_b on 18/07/16.
 */
public interface BenchServiceListener {

    void failure(FAILURE_STATES reason, Exception exception);
    void doneReceived(List<MediaInfo> files);
    void updatePercent(double percent, long bitRate);
    void stepFinished(String message);
}
