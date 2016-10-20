/*****************************************************************************
 * TestInfo.java
 *****************************************************************************
 * Copyright © 2011-2014 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.vlcbenchmark.tools;

import org.json.JSONException;
import org.json.JSONObject;

import org.videolan.vlcbenchmark.ResultCodes;

import java.io.Serializable;

/**
 * Created by penava_b on 19/07/16.
 */
public class TestInfo implements Serializable {

    public static final int QUALITY = 0; //screenshot
    public static final int PLAYBACK = 1; //bad frames
    public static final int SOFT = 0;
    public static final int HARD = 1;
    private static final String OK_STR = "Good";
    private static final String QUALITY_STR = "Quality (screenshots/seek)";
    private static final String PLAYBACK_STR = "Playback (frames dropped)";
    private static final String NO_HW_STR = "No Hardware support";
    private static final String VLC_CRASH_STR = "VLC crashed";
    private static final String UNKNOWN_STR = "Unkown problem";

    private String name;
    private double[] software = {20d, 30d}; //playback, performance
    private double[] hardware = {20d, 30d};
    private int loopNumber;
    private int[] framesDropped = {0, 0};
    private double[] percentOfBadScreenshots = {0d, 0d};
    private int[] numberOfWarnings = {0, 0};
    private String[][] crashed = {{"Quality: ", "Playback: "}, {"Quality: ", "Playback: "}};

    public TestInfo(String name, int loopNumber) {
        this.name = name;
        this.loopNumber = loopNumber;
    }

    public String getName() {
        return name;
    }

    public double getSoftware() {
        return software[QUALITY] + software[PLAYBACK];
    }

    public double getHardware() {
        return hardware[QUALITY] + hardware[PLAYBACK];
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
        tmp[PLAYBACK] -= 5 * number_of_dropped_frames;
        if (tmp[PLAYBACK] <= 0)
            tmp[PLAYBACK] = 0;
    }

    public void vlcCrashed(boolean isSoftware, boolean isScreenshot, int resultCode) {
        switch (resultCode) {
            case ResultCodes.RESULT_OK:
                crashed[isSoftware ? SOFT : HARD][isScreenshot ? QUALITY : PLAYBACK] += OK_STR;
                break;
            case ResultCodes.RESULT_FAILED:
                crashed[isSoftware ? SOFT : HARD][isScreenshot ? QUALITY : PLAYBACK] += (isScreenshot ? QUALITY_STR : PLAYBACK_STR);
                break;
            case ResultCodes.RESULT_NO_HW:
                crashed[isSoftware ? SOFT : HARD][isScreenshot ? QUALITY : PLAYBACK] += NO_HW_STR;
                break;
            case ResultCodes.RESULT_VLC_CRASH:
                crashed[isSoftware ? SOFT : HARD][isScreenshot ? QUALITY : PLAYBACK] += VLC_CRASH_STR;
                break;
            default:
                crashed[isSoftware ? SOFT : HARD][isScreenshot ? QUALITY : PLAYBACK] += UNKNOWN_STR;
                break;
        }
        (isSoftware ? software : hardware)[(isScreenshot ? QUALITY : PLAYBACK)] = 0;
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
        return crashed[index][QUALITY] + "\n" + crashed[index][PLAYBACK] + '\n';
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
