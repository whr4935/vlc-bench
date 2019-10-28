/*
 *****************************************************************************
 * JsonHandler.java
 *****************************************************************************
 * Copyright © 2017 - 2018 VLC authors and VideoLAN
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

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.videolan.vlcbenchmark.BuildConfig;
import org.videolan.vlcbenchmark.SystemPropertiesProxy;

public class JsonHandler {

    private final static String TAG = "JsonHandler";

    public static String save(ArrayList<TestInfo> testInfoList) throws JSONException {
        JSONArray testInformation;
        testInformation = getTestInformation(testInfoList, true);
        FileOutputStream jsonFileOutputStream;
        String folderName = StorageManager.INSTANCE.getInternalDirStr(StorageManager.INSTANCE.jsonFolder);
        if (!StorageManager.INSTANCE.checkFolderLocation(folderName)) {
            Log.e(TAG, "Failed to created json folder");
            return null;
        }
        String fileName = FormatStr.INSTANCE.getDateStr();
        File jsonFile = new File(folderName + fileName + ".txt");
        try {
            jsonFileOutputStream = new FileOutputStream(jsonFile);
            if (BuildConfig.DEBUG) {
                jsonFileOutputStream.write(testInformation.toString(4).getBytes());
            } else {
                jsonFileOutputStream.write(testInformation.toString().getBytes());
            }

        } catch (IOException e) {
            Log.e(TAG, "Failed to save json test results");
            StorageManager.INSTANCE.delete(jsonFile);
            return null;
        }
        return fileName;
    }

    public static ArrayList<TestInfo> load(String fileName) {
        File jsonFile = new File(StorageManager.INSTANCE.getInternalDirStr(StorageManager.INSTANCE.jsonFolder) + fileName);
        ArrayList<TestInfo> testInfoList = new ArrayList<>();
        try {
            StringBuilder text = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(jsonFile));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
            JSONArray jsonArray = new JSONArray(text.toString());
            for (int i = 0 ; i < jsonArray.length() ; ++i) {
                testInfoList.add(i, new TestInfo(jsonArray.getJSONObject(i)));
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Json file not found: " + e.toString());
            return null;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to load json file : " + e.toString());
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Failed to read jsonFile : " + e.toString());
            return null;
        }
        return testInfoList;
    }

    public static ArrayList<String> getFileNames() {
        String dirname = StorageManager.INSTANCE.getInternalDirStr(StorageManager.INSTANCE.jsonFolder);
        if (dirname == null) {
            return null;
        }
        File dir = new File(dirname);
        File[] files = dir.listFiles();
        ArrayList<String> results = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.getName().contains(".txt")) {
                    results.add(file.getName().replaceAll(".txt", ""));
                }
            }
        }
        return results;
    }

    public static boolean deleteFiles(){
        String dirpath = StorageManager.INSTANCE.getInternalDirStr(StorageManager.INSTANCE.jsonFolder);
        if (dirpath == null) {
            Log.e(TAG, "Failed to get folder path");
            return false;
        }
        File dir = new File(dirpath);
        File[] files = dir.listFiles();
        for (File file : files) {
            StorageManager.INSTANCE.delete(file);
        }
        return true;
    }

    /**
     * Returns the JSON array to send to the server.
     *
     * @param testInfoList list of all test results.
     * @return null in case of failure.
     */
    public static JSONObject dumpResults(Context context, ArrayList<TestInfo> testInfoList, Intent gpuData, Boolean withScreenshots) throws JSONException {
        JSONObject results = new JSONObject();
        JSONObject deviceInformation;
        JSONArray testInformation;
        double score_software = 0;
        double score_hardware = 0;

        deviceInformation = getDeviceInformation(gpuData);
        testInformation = getTestInformation(testInfoList, withScreenshots);

        for (TestInfo test : testInfoList) {
            score_software += test.getSoftware();
            score_hardware += test.getHardware();
        }

        if (deviceInformation == null || testInformation == null)
            return null;

        results.put("device_information", deviceInformation);
        results.put("test_information", testInformation);
        results.put("score_software", score_software);
        results.put("score_hardware", score_hardware);
        results.put("score", score_software + score_hardware);
        results.put("vlc_version", VLCProxy.Companion.getVLCVersion(context));

        return results;
    }

    /**
     * Returns test information in a JSONArray.
     *
     * @param testInfoList list of all test results.
     * @return null in case of failure.
     */
    private static JSONArray getTestInformation(ArrayList<TestInfo> testInfoList, Boolean withScreenshot) throws JSONException {
        JSONArray testInfoArray = new JSONArray();

        for (TestInfo element : testInfoList) {
            JSONObject testInfo;
            if (withScreenshot) {
                testInfo = element.jsonDumpWithScreenshots();
            } else {
                testInfo = element.jsonDump();
            }
            testInfoArray.put(testInfo);
        }
        return testInfoArray;
    }


    /**
     * Returns device information in a JSONObject.
     *
     * @return null in case of failure.
     */
    private static JSONObject getDeviceInformation(Intent gpuData) throws JSONException {
        JSONObject properties = new JSONObject();
        properties.put("board", Build.BOARD);
        properties.put("bootloader", Build.BOOTLOADER);
        properties.put("brand", Build.BRAND);
        properties.put("device", Build.DEVICE);
        properties.put("display", Build.DISPLAY);
        properties.put("fingerprint", Build.FINGERPRINT);
        properties.put("id", Build.ID);
        properties.put("manufacturer", Build.MANUFACTURER);
        properties.put("model", Build.MODEL);
        properties.put("product", Build.PRODUCT);
        properties.put("serial", Build.SERIAL);

        properties.put("supported_32_bit_abi", new JSONArray(Build.SUPPORTED_32_BIT_ABIS));
        properties.put("supported_64_bit_abi", new JSONArray(Build.SUPPORTED_64_BIT_ABIS));

        properties.put("tags", Build.TAGS);
        properties.put("type", Build.TYPE);

        properties.put("os_arch", System.getProperty("os.arch"));
        properties.put("kernel_name", System.getProperty("os.name"));
        properties.put("kernel_version", System.getProperty("os.version"));

        properties.put("version", Build.VERSION.RELEASE);
        properties.put("sdk", String.valueOf(Build.VERSION.SDK_INT));

        properties.put("cpu_model", SystemPropertiesProxy.getCpuModel());
        properties.put("cpu_cores", SystemPropertiesProxy.getCpuCoreNumber());
        properties.put("cpu_min_freq", SystemPropertiesProxy.getCpuMinFreq());
        properties.put("cpu_max_freq", SystemPropertiesProxy.getCpuMaxFreq());
        properties.put("total_ram", SystemPropertiesProxy.getRamTotal());

        properties.put("gpu_model", gpuData.getStringExtra("gl_renderer"));
        properties.put("gpu_vendor", gpuData.getStringExtra("gl_vendor"));
        properties.put("opengl_version", gpuData.getStringExtra("gl_version"));
        properties.put("opengl_extensions", gpuData.getStringExtra("gl_extensions"));

        return properties;
    }
}
