package org.videolan.vlcbenchmark.service;

import java.io.Serializable;

/**
 * Created by penava_b on 19/07/16.
 */
public class TestInfo implements Serializable {
    private String name;
    private double hardwareScore;
    private double softwareScore;

    public TestInfo(String name, double hardwareScore, double softwareScore) {
        this.name = name;
        this.hardwareScore = hardwareScore;
        this.softwareScore = softwareScore;
    }

    public String getName() {
        return name;
    }

    public double getHardwareScore() {
        return hardwareScore;
    }

    public double getSoftwareScore() {
        return softwareScore;
    }
}
