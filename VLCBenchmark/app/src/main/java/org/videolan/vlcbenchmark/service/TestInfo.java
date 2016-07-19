package org.videolan.vlcbenchmark.service;

import java.io.Serializable;

/**
 * Created by penava_b on 19/07/16.
 */
public class TestInfo implements Serializable {
    private String name;

    private Score score = null;

    private int loopNumber;

    public TestInfo(String name, Score score, int loopNumber) {
        this.name = name;
        this.score = score;
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
}
