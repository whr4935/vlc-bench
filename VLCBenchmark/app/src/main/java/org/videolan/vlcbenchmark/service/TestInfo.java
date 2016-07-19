package org.videolan.vlcbenchmark.service;

import java.io.Serializable;

/**
 * Created by penava_b on 19/07/16.
 */
public class TestInfo implements Serializable {
    private String name;

    private Score score = null;

    public TestInfo(String name, Score score) {
        this.name = name;
        this.score = score;
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
}
