package org.videolan.vlcbenchmark.service;

import java.io.Serializable;

/**
 * Created by penava_b on 19/07/16.
 */
public class Score implements Serializable {
    public double hardware;
    public double software;

    public void add(Score score) {
        hardware += score.hardware;
        software += score.software;
    }

    Score(double hardware, double software) {
        this.hardware = hardware;
        this.software = software;
    }

    Score() {
    }

    public Score avrage(int numberOfElements) {
        return new Score(hardware / numberOfElements, software / numberOfElements);
    }
}
