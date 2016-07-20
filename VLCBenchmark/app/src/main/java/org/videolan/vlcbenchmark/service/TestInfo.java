package org.videolan.vlcbenchmark.service;

import java.io.Serializable;

/**
 * Created by penava_b on 19/07/16.
 */
public final class TestInfo implements Serializable {
    protected String name;
    protected Score score = new Score();
    protected int loopNumber;
    protected int frameDropped = 0;
    protected double percentOfBadScreenshots = 0;
    protected double percentOfBadSeek = 0;
    protected int numberOfWarnings = 0;

    protected TestInfo(String name, int loopNumber) {
        this.name = name;
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
