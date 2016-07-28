package org.videolan.vlcbenchmark.service;

import org.videolan.vlcbenchmark.TestInfo;

import java.util.List;

/**
 * Created by penava_b on 18/07/16.
 */
public class BenchServiceAdapter implements BenchServiceListener {

    @Override
    public void failure(FAILURE_STATES reason, Exception exception) {

    }

    @Override
    public void doneReceived(List<MediaInfo> files) {
    }

    @Override
    public void updatePercent(double percent) {
    }
}
