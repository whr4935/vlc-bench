package org.videolan.vlcbenchmark.tools;

import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.videolan.vlcbenchmark.BuildConfig;
import org.videolan.vlcbenchmark.SystemPropertiesProxy;

public class JsonHandler {

    private final static String TAG = JsonHandler.class.getName();

    public static String toDatePrettyPrint(String name) {
        char[] array;
        int count = 0;
        int i = 0;
        array = name.toCharArray();
        while (i < array.length && count < 6) {
            if (array[i] == '_') {
                if (count == 1) {
                    array[i] = ',';
                } else if (count >= 4) {
                    array[i] = ':';
                } else {
                    array[i] = ' ';
                }
                ++count;
            }
            ++i;
        }
        return new String(array);
    }

    public static String fromDatePrettyPrint(String name) {
        char[] array;
        int count = 0;
        int i = 0;
        array = name.toCharArray();
        while (i < array.length && count < 6) {
            if (array[i] == ' ' || array[i] == ',' || array[i] == ':') {
                array[i] = '_';
                ++count;
            }
            ++i;
        }
        return new String(array);
    }

    public static String save(ArrayList<TestInfo> testInfoList) throws JSONException {
        JSONArray testInformation;
        testInformation = getTestInformation(testInfoList);
        FileOutputStream fileOutputStream;
        String folderName = FileHandler.getFolderStr("jsonFolder");
        if (!FileHandler.checkFolderLocation(folderName)) {
            Log.e("VLCBench", "Failed to created json folder");
            return null;
        }
        String fileName = FormatStr.getDateStr();
        File jsonFile = new File(folderName + fileName + ".txt");
        try {
            fileOutputStream = new FileOutputStream(jsonFile);
            fileOutputStream.write(testInformation.toString(4).getBytes());
        } catch (IOException e) {
            Log.e("VLC Benchmark", "Failed to save json test results");
            //TODO handle fail to write to file
        }
        return fileName;
    }

    public static ArrayList<TestInfo> load(String fileName) {
        File jsonFile = new File(FileHandler.getFolderStr("jsonFolder") + fileName);
        ArrayList<TestInfo> testInfoList = new ArrayList<TestInfo>();
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
            Log.e("VLCBenchmark", "Json file not found: " + e.toString());
            return null;
        } catch (JSONException e) {
            Log.e("VLCBenchmark", "Failed to load json file : " + e.toString());
            return null;
        } catch (IOException e) {
            Log.e("VLCBenchmark", "Failed to read jsonFile : " + e.toString());
            return null;
        }
        return testInfoList;
    }

    public static ArrayList<String> getFileNames() {
        File dir = new File(FileHandler.getFolderStr("jsonFolder"));
        File[] files = dir.listFiles();
        ArrayList<String> fileNames = new ArrayList<String>();
        if (files != null) {
            for (File file : files) {
                fileNames.add(file.getName().replaceAll(".txt", ""));
            }
        }
        return fileNames;
    }

    public static boolean deleteFiles(){
        File dir = new File(FileHandler.getFolderStr("jsonFolder"));
        File[] files = dir.listFiles();
        for (File file : files) {
            if (!file.delete()) {
                Log.e("VLCBench", "Failed to delete test results");
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the JSON array to send to the server.
     *
     * @param testInfoList list of all test results.
     * @return null in case of failure.
     */
    public static JSONObject dumpResults(ArrayList<TestInfo> testInfoList, Intent gpuData) throws JSONException {
        JSONObject results = new JSONObject();
        JSONObject deviceInformation;
        JSONArray testInformation;
        double score_software = 0;
        double score_hardware = 0;

        deviceInformation = getDeviceInformation(gpuData);
        testInformation = getTestInformation(testInfoList);

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
        results.put("vlc_version", BuildConfig.VLC_VERSION);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "openGL_extensions size: " + results.getJSONObject("device_information").getString("opengl_extensions").length());
            Log.d(TAG, "device_information: " + results.getJSONObject("device_information").toString(4));
            Log.d(TAG, "test_information: " + results.getJSONArray("test_information").toString(4));
        }

        return results;
    }

    /**
     * Returns test information in a JSONArray.
     *
     * @param testInfoList list of all test results.
     * @return null in case of failure.
     */
    private static JSONArray getTestInformation(ArrayList<TestInfo> testInfoList) throws JSONException {
        JSONArray testInfoArray = new JSONArray();

        for (TestInfo element : testInfoList) {
            JSONObject testInfo = new JSONObject();
            element.transferInJSon(testInfo);
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
