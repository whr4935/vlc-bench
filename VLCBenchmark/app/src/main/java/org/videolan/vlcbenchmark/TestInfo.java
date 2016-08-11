package org.videolan.vlcbenchmark;

import java.io.Serializable;

/**
 * Created by penava_b on 19/07/16.
 */
public class TestInfo implements Serializable {

    public static int PERFORMANCE = 0;
    public static int PLAYBACK = 1;

    String name;
    double[] software = {30d, 20d}; //performance, playback
    double[] hardware = {30d, 20d};
    int loopNumber;
    double percentOfFrameDrop;
    double percentOfBadScreenshots;
    double percentOfBadSeek;
    int numberOfWarnings;

    public void badScreenshot(boolean isSoftware) {
//        (isSoftware ? software : hardware)[PLAYBACK];
    }

    public void badSeek(boolean isSoftware) {
    }

    public void badFrames(int number_of_dropped_frames, boolean isSoftware) {
        double[] tmp = (isSoftware ? software : hardware);

        tmp[PERFORMANCE] -= 5 * number_of_dropped_frames;
        if (tmp[PERFORMANCE] <= 0)
            tmp[PERFORMANCE] = 0;
    }
}
