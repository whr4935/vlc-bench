/*
 *****************************************************************************
 * TestInfo.java
 *****************************************************************************
 * Copyright © 2016-2018 VLC authors and VideoLAN
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

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.videolan.vlcbenchmark.Constants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
    private static final String QUALITY_SFX = "Quality: ";
    private static final String PLAYBACK_SFX = "Playback: ";

    public static final double SCORE_TOTAL = 50d;

    private static final double SCORE_MAX_PLAYBACK = 20d;
    private static final double SCORE_MAX_PERFORMANCE = 30d;

    private String name;
    private double[] software = {SCORE_MAX_PLAYBACK, SCORE_MAX_PERFORMANCE};
    private double[] hardware = {SCORE_MAX_PLAYBACK, SCORE_MAX_PERFORMANCE};
    private int loopNumber;
    private int[] framesDropped = {0, 0};
    private double[] percentOfBadScreenshots = {0d, 0d};
    private int[] numberOfWarnings = {0, 0};
    private String[][] crashed = {{"", ""}, {"", ""}};

    public TestInfo(String name, int loopNumber) {
        this.name = name;
        this.loopNumber = loopNumber;
    }

    public TestInfo(String name, double[] software, double[] hardware, int[] framesDropped,
                    double[] percentOfBadScreenshots, int[] numberOfWarnings, String[][] crashed) {
        this.name = name;
        this.software = software;
        this.hardware = hardware;
        this.loopNumber = 0;
        this.framesDropped = framesDropped;
        this.percentOfBadScreenshots = percentOfBadScreenshots;
        this.numberOfWarnings = numberOfWarnings;
        this.crashed = crashed;
    }

    public TestInfo(JSONObject jsonObject) {
        try {
            JSONArray array;
            name = jsonObject.getString("name");
            array = jsonObject.getJSONArray("hardware_score");
            for (int i = 0 ; i < array.length() ; ++i) {
                hardware[i] = array.getDouble(i);
            }
            array = jsonObject.getJSONArray("software_score");
            for (int i = 0 ; i < array.length() ; ++i) {
                software[i] = array.getDouble(i);
            }
            loopNumber = jsonObject.getInt("loop_number");
            array = jsonObject.getJSONArray("frames_dropped");
            for (int i = 0 ; i < array.length() ; ++i) {
                framesDropped[i] = array.getInt(i);
            }
            array = jsonObject.getJSONArray("percent_of_bad_screenshot");
            for (int i = 0 ; i < array.length() ; ++i) {
                percentOfBadScreenshots[i] = array.getDouble(i);
            }
            array = jsonObject.getJSONArray("number_of_warning");
            for (int i = 0 ; i < array.length() ; ++i) {
                numberOfWarnings[i] = array.getInt(i);
            }
            array = jsonObject.getJSONArray("crashed");
            for (int i = 0 ; i < array.length() ; ++i) {
                JSONArray subArray = array.getJSONArray(i);
                for (int j = 0 ; j < subArray.length() ; ++j) {
                    crashed[i][j] = subArray.getString(j);
                }
            }
        } catch (JSONException e){
            Log.e("VLCBench", e.toString());
            //TODO handle json exception
        }
    }

    public static double getHardScore(ArrayList<TestInfo> testInfo) {
        double hardware = 0;
        for (TestInfo info : testInfo) {
            hardware += info.getHardware();
        }
        return hardware;
    }

    public static double getSoftScore(ArrayList<TestInfo> testInfo) {
        double software = 0;
        for (TestInfo info : testInfo) {
            software += info.getSoftware();
        }
        return software;
    }

    public static double getGlobalScore(ArrayList<TestInfo> list) {

        return getHardScore(list) + getSoftScore(list);
    }

    public String getName() {
        return name;
    }

    public double getSoftware() {
        return software[QUALITY] + software[PLAYBACK];
    }

    public double getSoftwareSpecific(int quality) { return software[quality]; }

    public double getHardware() {
        return hardware[QUALITY] + hardware[PLAYBACK];
    }

    public double getHardwareSpecific(int quality) { return hardware[quality]; }

    public int getFrameDropped(int testType) {
        return framesDropped[testType == SOFT ? SOFT : HARD];
    }

    public double getBadScreenshots(int testType) {
        return percentOfBadScreenshots[testType == SOFT ? SOFT : HARD];
    }

    public int getNumberOfWarnings(int testType) {
        return numberOfWarnings[testType == SOFT ? SOFT : HARD];
    }

    public void setBadScreenshot(double percent, boolean isSoftware) {
        double[] tmp = (isSoftware ? software : hardware);

        percentOfBadScreenshots[isSoftware ? SOFT : HARD] = percent;
        tmp[QUALITY] = Math.floor((1.0 - (percent / 100.0)) * tmp[QUALITY]);
    }

    public void setBadFrames(int number, boolean isSoftware) {
        this.framesDropped[isSoftware ? SOFT : HARD] += number;
        if (isSoftware) {
            software[PLAYBACK] = software[PLAYBACK] - 2 * number > 0 ? software[PLAYBACK] - 2 * number : 0;
        } else {
            hardware[PLAYBACK] = hardware[PLAYBACK] - 2 * number > 0 ? hardware[PLAYBACK] - 2 * number : 0;
        }
    }

    public void setWarningNumber(int number, boolean isSoftware) {
        this.numberOfWarnings[isSoftware ? SOFT : HARD] += number;
        if (isSoftware) {
            software[PLAYBACK] = software[PLAYBACK] - number > 0 ? software[PLAYBACK] - number : 0;
        } else {
            hardware[PLAYBACK] = hardware[PLAYBACK] - number > 0 ? hardware[PLAYBACK] - number : 0;
        }
    }

    private String getSfx(boolean isScreenshot) {
        return (isScreenshot ? QUALITY_SFX : PLAYBACK_SFX);
    }

    public void vlcCrashed(boolean isSoftware, boolean isScreenshot, String errorMessage) {
        crashed[isSoftware ? SOFT : HARD][isScreenshot ? QUALITY : PLAYBACK] =
                getSfx(isScreenshot) + errorMessage;
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

    public String getCrashes(int decoding, int testtype) {
        return crashed[decoding][testtype];
    }

    public boolean hasCrashed(int decoding) {
        return !crashed[decoding][QUALITY].equals("") || !crashed[decoding][PLAYBACK].equals("");
    }

    public static ArrayList<TestInfo> mergeTests(List<TestInfo>[] results) {
        ArrayList<TestInfo> test = new ArrayList<>();
        for (int i = 0 ; i < results[0].size() ; ++i) {
            double[] software = {0d, 0d};
            double[] hardware = {0d, 0d};
            int[] framesDropped = {0, 0};
            double[] percentOfBadScreenshots = {0d, 0d};
            int[] numberOfWarnings = {0, 0};
            String[][] crashed = {{"", ""}, {"", ""}};

            for (int inc = 0 ; inc < results.length ; ++inc) {
                software[TestInfo.QUALITY] += results[inc].get(i).getSoftwareSpecific(TestInfo.QUALITY);
                software[TestInfo.PLAYBACK] += results[inc].get(i).getSoftwareSpecific(TestInfo.PLAYBACK);
                hardware[TestInfo.QUALITY] += results[inc].get(i).getHardwareSpecific(TestInfo.QUALITY);
                hardware[TestInfo.PLAYBACK] += results[inc].get(i).getHardwareSpecific(TestInfo.PLAYBACK);
                framesDropped[TestInfo.QUALITY] += results[inc].get(i).getFrameDropped(TestInfo.QUALITY);
                framesDropped[TestInfo.PLAYBACK] += results[inc].get(i).getFrameDropped(TestInfo.PLAYBACK);
                percentOfBadScreenshots[TestInfo.QUALITY] += results[inc].get(i).getBadScreenshots(TestInfo.QUALITY);
                percentOfBadScreenshots[TestInfo.PLAYBACK] += results[inc].get(i).getBadScreenshots(TestInfo.PLAYBACK);
                numberOfWarnings[TestInfo.QUALITY] += results[inc].get(i).getNumberOfWarnings(TestInfo.QUALITY);
                numberOfWarnings[TestInfo.PLAYBACK] += results[inc].get(i).getNumberOfWarnings(TestInfo.PLAYBACK);
                //TODO add method to handle several crash
                crashed[TestInfo.SOFT][TestInfo.QUALITY] += results[inc].get(i).getCrashes(TestInfo.SOFT, TestInfo.QUALITY);
                crashed[TestInfo.SOFT][TestInfo.PLAYBACK] += results[inc].get(i).getCrashes(TestInfo.SOFT, TestInfo.PLAYBACK);
                crashed[TestInfo.HARD][TestInfo.QUALITY] += results[inc].get(i).getCrashes(TestInfo.HARD, TestInfo.QUALITY);
                crashed[TestInfo.HARD][TestInfo.PLAYBACK] += results[inc].get(i).getCrashes(TestInfo.HARD, TestInfo.PLAYBACK);
            }

            software[TestInfo.QUALITY] /= results.length;
            software[TestInfo.PLAYBACK] /= results.length;
            hardware[TestInfo.QUALITY] /= results.length;
            hardware[TestInfo.PLAYBACK] /= results.length;
            framesDropped[TestInfo.QUALITY] /= results.length;
            framesDropped[TestInfo.PLAYBACK] /= results.length;
            percentOfBadScreenshots[TestInfo.QUALITY] /= results.length;
            percentOfBadScreenshots[TestInfo.PLAYBACK] /= results.length;
            numberOfWarnings[TestInfo.QUALITY] /= results.length;
            numberOfWarnings[TestInfo.PLAYBACK] /= results.length;

            test.add(new TestInfo(results[0].get(i).getName(), software, hardware,
                    framesDropped, percentOfBadScreenshots, numberOfWarnings, crashed));
        }
        return test;
    }

    public JSONObject jsonDump() throws JSONException {
        int[] jsonSoftware = {(int)software[QUALITY], (int)software[PLAYBACK]};
        int[] jsonHardware = {(int)hardware[QUALITY], (int)hardware[PLAYBACK]};
        int[] jsonPercent = {(int)percentOfBadScreenshots[QUALITY],
                (int)percentOfBadScreenshots[PLAYBACK]};
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("hardware_score", new JSONArray(jsonHardware));
        jsonObject.put("software_score", new JSONArray(jsonSoftware));
        jsonObject.put("loop_number", loopNumber);
        jsonObject.put("frames_dropped", new JSONArray(framesDropped));
        jsonObject.put("percent_of_bad_screenshot", new JSONArray(jsonPercent));
        jsonObject.put("number_of_warning", new JSONArray(numberOfWarnings));
        JSONArray array = new JSONArray();
        array.put(new JSONArray(crashed[0]));
        array.put(new JSONArray(crashed[1]));
        jsonObject.put("crashed", array);
        return jsonObject;
    }
}
