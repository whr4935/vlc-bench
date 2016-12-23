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

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.videolan.vlcbenchmark.ResultCodes;

import java.io.Serializable;
import java.lang.reflect.Array;
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

    private String name;
    private double[] software = {20d, 30d}; //playback, performance
    private double[] hardware = {20d, 30d};
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

    public void display() {
        Log.e("VLCBench - testinfo", "name = " + name);
        Log.e("VLCBench - testinfo", "software = " + (software[0] + software[1]));
        Log.e("VLCBench - testinfo", "hardware = " + (hardware[0] + hardware[1]));
        Log.e("VLCBench - testinfo", "loopnumber = " + loopNumber);
        Log.e("VLCBench - testinfo", "framesDropped[0] = " + framesDropped[0]);
        Log.e("VLCBench - testinfo", "framesDropped[1] = " + framesDropped[1]);
        Log.e("VLCBench - testinfo", "percentOfBadScreenshots[0] = " + percentOfBadScreenshots[0]);
        Log.e("VLCBench - testinfo", "percentOfBadScreenshots[1] = " + percentOfBadScreenshots[1]);
        Log.e("VLCBench - testinfo", "numberOfWarnings[0] = " + numberOfWarnings[0]);
        Log.e("VLCBench - testinfo", "numberOfWarnings[1] = " + numberOfWarnings[1]);
        Log.e("VLCBench - testinfo", crashed[0][0]);
        Log.e("VLCBench - testinfo", crashed[0][1]);
        Log.e("VLCBench - testinfo", crashed[1][0]);
        Log.e("VLCBench - testinfo", crashed[1][1]);
        Log.e("VLCBench - testinfo", "EOT");
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

    private String getSfx(boolean isScreenshot) {
        return (isScreenshot ? QUALITY_SFX : PLAYBACK_SFX);
    }

    public void vlcCrashed(boolean isSoftware, boolean isScreenshot, int resultCode) {
        switch (resultCode) {
            case ResultCodes.RESULT_OK:
                crashed[isSoftware ? SOFT : HARD][isScreenshot ? QUALITY : PLAYBACK] = getSfx(isScreenshot) + OK_STR;
                break;
            case ResultCodes.RESULT_FAILED:
                crashed[isSoftware ? SOFT : HARD][isScreenshot ? QUALITY : PLAYBACK] = (isScreenshot ? QUALITY_STR : PLAYBACK_STR);
                break;
            case ResultCodes.RESULT_NO_HW:
                crashed[isSoftware ? SOFT : HARD][isScreenshot ? QUALITY : PLAYBACK] = getSfx(isScreenshot) + NO_HW_STR;
                break;
            case ResultCodes.RESULT_VLC_CRASH:
                crashed[isSoftware ? SOFT : HARD][isScreenshot ? QUALITY : PLAYBACK] = getSfx(isScreenshot) +VLC_CRASH_STR;
                break;
            default:
                crashed[isSoftware ? SOFT : HARD][isScreenshot ? QUALITY : PLAYBACK] = getSfx(isScreenshot) +UNKNOWN_STR;
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

    public String getCrashes(int decoding, int testtype) {
        return crashed[decoding][testtype];
    }

    public boolean hasCrashed(int decoding) {
        if (crashed[decoding][QUALITY] != "" || crashed[decoding][PLAYBACK] != "") {
            return true;
        }
        return false;
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
                crashed[TestInfo.SOFT][TestInfo.QUALITY] += results[inc].get(i).getCrashes(TestInfo.SOFT, TestInfo.QUALITY) + "\n";
                crashed[TestInfo.SOFT][TestInfo.PLAYBACK] += results[inc].get(i).getCrashes(TestInfo.SOFT, TestInfo.PLAYBACK) + "\n";
                crashed[TestInfo.HARD][TestInfo.QUALITY] += results[inc].get(i).getCrashes(TestInfo.HARD, TestInfo.QUALITY) + "\n";
                crashed[TestInfo.HARD][TestInfo.PLAYBACK] += results[inc].get(i).getCrashes(TestInfo.HARD, TestInfo.PLAYBACK) + "\n";
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

    public void transferInJSon(JSONObject holder) throws JSONException {
        holder.put("name", name);
        holder.put("hardware_score", new JSONArray(hardware));
        holder.put("software_score", new JSONArray(software));
        holder.put("loop_number", loopNumber);
        holder.put("frames_dropped", new JSONArray(framesDropped));
        holder.put("percent_of_bad_screenshot", new JSONArray(percentOfBadScreenshots));
        holder.put("number_of_warning", new JSONArray(numberOfWarnings));
        JSONArray array = new JSONArray();
        array.put(new JSONArray(crashed[0]));
        array.put(new JSONArray(crashed[1]));
        holder.put("crashed", array);
    }
}
