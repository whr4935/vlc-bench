package org.videolan.vlcbenchmark;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by penava_b on 19/07/16.
 */
public class TestInfo implements Serializable {

    public static final int PLAYBACK = 0; //screenshot
    public static final int PERFORMANCE = 1; //bad frames
    public static final int SOFT = 0;
    public static final int HARD = 1;
    private static final String PLAYBACK_STR = "playback (screenshots/seek)";
    private static final String PERFORMANCE_STR = "performance (frames dropped)";

    private String name;
    private double[] software = {20d, 30d}; //playback, performance
    private double[] hardware = {20d, 30d};
    private int loopNumber;
    private int[] framesDropped = {0, 0};
    private double[] percentOfBadScreenshots = {0d, 0d};
    private int[] numberOfWarnings = {0, 0};
    private boolean[][] crashed = {{false, false}, {false, false}};

    public TestInfo(String name, int loopNumber) {
        this.name = name;
        this.loopNumber = loopNumber;
    }

    public String getName() {
        return name;
    }

    public double getSoftware() {
        return software[PLAYBACK] + software[PERFORMANCE];
    }

    public double getHardware() {
        return hardware[PLAYBACK] + hardware[PERFORMANCE];
    }

    public int getFrameDropped(int testType) {
        return framesDropped[testType == SOFT ? SOFT : HARD];
    }

    public double getBadScreenshots(int testType) {
        return percentOfBadScreenshots[testType == SOFT ? SOFT : HARD];
    }

    public int getNumberOfWarnings(int testType) {
        return numberOfWarnings[testType == SOFT ? SOFT : HARD];
    }

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
        int begin = 0, end = str.length() - 1;

        if (str.isEmpty())
            return "";
        while (str.charAt(begin) == '\n')
            begin++;
        while (str.charAt(end) == '\n')
            end--;
        if (begin >= end)
            return "";
        return str.substring(begin, end);
    }

    public String getCrashes(int index) {
        if (!crashed[index][PLAYBACK] && !crashed[index][PERFORMANCE])
            return "";
        return strip((crashed[index][PLAYBACK] ? PLAYBACK_STR : "") + "\n" + (crashed[index][PERFORMANCE] ? PERFORMANCE_STR : "") + '\n');
    }

    public void transferInJSon(JSONObject holder) throws JSONException {
        holder.put("name", name);
        holder.put("hardware_score", hardware);
        holder.put("software_score", software);
        holder.put("loop_number", loopNumber);
        holder.put("frame_dropped", framesDropped);
        holder.put("percent_of_bad_screenshot", percentOfBadScreenshots);
        holder.put("number_of_warning", numberOfWarnings);
    }
}
