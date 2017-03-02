package org.videolan.vlcbenchmark.tools;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import java.lang.reflect.Array;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.videolan.vlcbenchmark.SystemPropertiesProxy;

public class JsonHandler {

    private static String getName() {
        String str_date = new Date().toLocaleString();
        str_date = str_date.replaceAll(",", "");
        str_date = str_date.replaceAll(" ", "_");
        str_date = str_date.replaceAll(":", "_");
        return str_date;
    }

    private static String getFolder() {
        return Environment.getExternalStorageDirectory() + File.separator + "jsonFolder" + File.separator;
    }

    private static boolean secureJsonLocation() {
        File folder = new File(getFolder());
        boolean ret = true;
        if (!folder.exists()) {
            ret = folder.mkdir();
        }
        return ret;
    }

    public static String toDatePrettyPrint(String name) {
        char[] array;
        int count = 0;
        int i = 0;
        array = name.toCharArray();
        while (i < array.length && count < 3) {
            if (array[i] == '_') {
                array[i] = ' ';
                ++count;
            }
            ++i;
        }
        while (i < array.length && count < 5) {
            if (array[i] == '_') {
                array[i] = ':';
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
        while (i < array.length && count < 3) {
            if (array[i] == ' ') {
                array[i] = '_';
                ++count;
            }
            ++i;
        }
        while (i < array.length && count < 5) {
            if (array[i] == ':') {
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
        if (!secureJsonLocation()) {
            Log.e("VLCBench", "Failed to created json folder");
            return null;
        }
        String name = getName();
        File jsonFile = new File(getFolder() + name + ".txt");
        try {
            fileOutputStream = new FileOutputStream(jsonFile);
            fileOutputStream.write(testInformation.toString(4).getBytes());
        } catch (IOException e) {
            Log.e("VLC Benchmark", "Failed to save json test results");
            //TODO handle fail to write to file
        }
        return name;
    }

    public static ArrayList<TestInfo> load(String fileName) {
        File jsonFile = new File(getFolder() + fileName);
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
        File dir = new File(getFolder());
        File[] files = dir.listFiles();
        ArrayList<String> fileNames = new ArrayList<String>();
        if (files != null) {
            for (File file : files) {
                fileNames.add(file.getName().replaceAll(".txt", ""));
            }
            Collections.sort(fileNames);
            Collections.reverse(fileNames);
        }
        return fileNames;
    }

    public static boolean deleteFiles(){
        File dir = new File(getFolder());
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
    public static JSONObject dumpResults(ArrayList<TestInfo> testInfoList) throws JSONException {
        JSONObject results = new JSONObject();
        JSONObject deviceInformation;
        JSONArray testInformation;

        deviceInformation = getDeviceInformation();
        testInformation = getTestInformation(testInfoList);

        if (deviceInformation == null || testInformation == null)
            return null;

        results.put("device_information", deviceInformation);
        results.put("test_information", testInformation);
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

    public static void displayDeviceInfo() {
        Log.e("VLCBench", "board = " + Build.BOARD);
        Log.e("VLCBench", "bootloader = " + Build.BOOTLOADER);
        Log.e("VLCBench", "brand = " + Build.BRAND);
        Log.e("VLCBench", "device = " + Build.DEVICE);
        Log.e("VLCBench", "display = " + Build.DISPLAY);
        Log.e("VLCBench", "fingerprint = " + Build.FINGERPRINT);
        Log.e("VLCBench", "host = " + Build.HOST);
        Log.e("VLCBench", "id = " + Build.ID);
        Log.e("VLCBench", "manufacturer = " + Build.MANUFACTURER);
        Log.e("VLCBench", "model = " + Build.MODEL);
        Log.e("VLCBench", "product = " + Build.PRODUCT);
        Log.e("VLCBench", "serial = " + Build.SERIAL);

        /* Min version API 21 */
//        properties.put("supported_32_bit_abi", Build.SUPPORTED_32_BIT_ABIS);
//        properties.put("supported_64_bit_abi", Build.SUPPORTED_64_BIT_ABIS);
//        properties.put("supported_abi", Build.SUPPORTED_ABIS);

        Log.e("VLCBench", "tags = " + Build.TAGS);
        Log.e("VLCBench", "time = " + Build.TIME);
        Log.e("VLCBench", "type = " + Build.TYPE);
        Log.e("VLCBench", "user = " + Build.USER);

        Log.e("VLCBench", "os_arch = " + System.getProperty("os.arch"));
    }

    /**
     * Returns device information in a JSONObject.
     *
     * @return null in case of failure.
     */
    private static JSONObject getDeviceInformation() throws JSONException {
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

        properties.put("supported_32_bit_abi", Build.SUPPORTED_32_BIT_ABIS);
        properties.put("supported_64_bit_abi", Build.SUPPORTED_64_BIT_ABIS);
        properties.put("supported_abi_list", Build.SUPPORTED_ABIS);

        properties.put("tags", Build.TAGS);
        properties.put("type", Build.TYPE);

        properties.put("os_arch", System.getProperty("os.arch"));
        properties.put("kernel_name", System.getProperty("os.name"));
        properties.put("kernel_version", System.getProperty("os.version"));

        properties.put("version", Build.VERSION.RELEASE);
        properties.put("sdk", Build.VERSION.SDK_INT);

        properties.put("cpu_model", SystemPropertiesProxy.getCpuModel());
        properties.put("cpu_cores", SystemPropertiesProxy.getCpuCoreNumber());
        properties.put("cpu_min_freq", SystemPropertiesProxy.getCpuMinFreq());
        properties.put("cpu_max_freq", SystemPropertiesProxy.getCpuMaxFreq());
        properties.put("total_ram", SystemPropertiesProxy.getRamTotal());

        return properties;
    }
}
