package org.videolan.vlcbenchmark;

import java.io.Serializable;

/**
 * Created by penava_b on 19/07/16.
 */
public class TestInfo implements Serializable {

    public static final int PLAYBACK = 0; //screenshot
    public static final int PERFORMANCE = 1; //bad frames
    static final int SOFT = 0;
    static final int HARD = 1;
    private static final String PLAYBACK_STR = "playback (screenshots/seek)";
    private static final String PERFORMANCE_STR = "performance (frames dropped)";

    String name;
    double[] software = {20d, 30d}; //playback, performance
    double[] hardware = {20d, 30d};
    int loopNumber;
    int[] framesDropped = {0, 0};
    double[] percentOfBadScreenshots = {0d, 0d};
    int[] numberOfWarnings = {0, 0};
    boolean[][] crashed = {{false, false}, {false, false}};

    public void badScreenshot(double percent, boolean isSoftware) {
        percentOfBadScreenshots[isSoftware ? SOFT : HARD] = percent;
    }

    public void badFrames(int number_of_dropped_frames, boolean isSoftware) {
        double[] tmp = (isSoftware ? software : hardware);

        framesDropped[isSoftware ? SOFT : HARD] += number_of_dropped_frames;
        tmp[PERFORMANCE] -= 5 * number_of_dropped_frames;
        if (tmp[PERFORMANCE] <= 0)
            tmp[PERFORMANCE] = 0;
    }

    public void vlcCrashed(boolean isSoftware, boolean isScreenshot) {
        crashed[isSoftware ? SOFT : HARD][isScreenshot ? PLAYBACK : PERFORMANCE] = true;
        (isSoftware ? software : hardware)[(isScreenshot ? PLAYBACK : PERFORMANCE)] = 0;
    }

    private static String strip(String str) {
        if (str.startsWith("\n"))
            str = str.substring(1);
        if (str.endsWith("\n"))
            str = str.substring(0, str.length());
        return str;
    }

    public String getCrashes(int index) {
        if (!crashed[index][PLAYBACK] && !crashed[index][PERFORMANCE])
            return "";
        return strip((crashed[index][PLAYBACK] ? PLAYBACK_STR : "") + "\n" + (crashed[index][PERFORMANCE] ? PERFORMANCE_STR : "") + '\n');
    }
}
