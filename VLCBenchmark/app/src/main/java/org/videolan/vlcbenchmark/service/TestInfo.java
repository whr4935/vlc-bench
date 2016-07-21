package org.videolan.vlcbenchmark.service;

import java.io.Serializable;

/**
 * Created by penava_b on 19/07/16.
 */
public class TestInfo implements Serializable {
    String name;
    Score score = new Score();
    int loopNumber;
    int frameDropped = 0;
    double percentOfBadScreenshots = 0;
    double percentOfBadSeek = 0;
    int numberOfWarnings = 0;

    TestInfo(String name, int loopNumber) {
        this.name = name;
        this.loopNumber = loopNumber;
    }

    public String getName() {
        return name;
    }

    public double getHardwareScore() {
        return score.hardware;
    }

    public double getSoftwareScore() {
        return score.software;
    }

    public int getLoopNumber() {
        return loopNumber;
    }

    public int getFrameDropped() {
        return frameDropped;
    }

    public double getPercentOfBadScreenshots() {
        return percentOfBadScreenshots;
    }

    public double getPercentOfBadSeek() {
        return percentOfBadSeek;
    }

    public int getNumberOfWarnings() {
        return numberOfWarnings;
    }
}
