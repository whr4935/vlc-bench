package org.videolan.vlcbenchmark;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;

/**
 *  SystemPropertiesProxy gets
 */
public class SystemPropertiesProxy {

    /* String[] to store the unit indicator (ex: Gigabyte -> G)
    * in use for getReadableValue(...)*/
    private static String[] ext = {"K", "M", "G"};
    /* format to remove all decimals after the second */
    private static DecimalFormat df2 = new DecimalFormat(".##");

    /**
     * This class cannot be instantiated
     */
    private SystemPropertiesProxy() {
    }

    /**
     * Get the value for the given key.
     *
     * @return an empty string if the key isn't found
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static String get(String key) throws IllegalArgumentException {
        String ret;
        try {
            Class<?> SystemProperties = Class.forName("android.os.SystemProperties");

            /* Parameters Types */
            Class[] paramTypes = { String.class };
            Method get = SystemProperties.getMethod("get", paramTypes);

            /* Parameters */
            Object[] params = { key };
            ret = (String) get.invoke(SystemProperties, params);
        } catch (IllegalArgumentException iAE) {
            throw iAE;
        } catch (Exception e) {
            ret = "";
        }
        return ret;
    }

    /**
     * Reads spec from system files
     * @param filename file to open
     * @param info attribute to recover
     * @return attribute string after ': ' in file line
     */
    private static String readSysFile(String filename, String info) {
        try {
            String line;
            FileReader file = new FileReader(filename);
            BufferedReader reader = new BufferedReader(file);
            while ((line = reader.readLine()) != null) {
                Log.e("VLCBench", line);
                if (line.regionMatches(0, info, 0, info.length())) {
                    String[] splits = line.split(": ");
                    if (splits.length > 1) {
                        return splits[1].trim();
                    }
                }
            }
        } catch (java.io.FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * Converts a number to a human readable string form with unit
     * @param value int value to convert to readable form
     * @return readable string
     */
    private static String getReadableValue(int value) {
        int count = 0;
        double number = value;
        while ((number / 1000.0) > 1.0) {
            number = number / 1000.0;
            count += 1;
        }
        return df2.format(number) + " " + ext[count];
    }

    /**
     * Returns the cpu model
     * @return string with cpu model
     */
    public static String getCpuModel() {
        return readSysFile("/proc/cpuinfo", "Hardware");
    }

    /**
     * Reads and returns total Ram
     * @return string with total ram and unit
     */
    public static String getRamTotal() {
        String info = readSysFile("proc/meminfo", "MemTotal");
        if (info != null) {
            info = info.replaceFirst(" kB", "");
            info = getReadableValue(Integer.parseInt(info)) + "B";
        }
        return info;
    }

    /**
     * Reads and returns cpu minimum frequency
     * @return string with cpu minimum frequency and unit
     */
    public static String getCpuMinFreq() {
        try {
            FileReader file = new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq");
            BufferedReader reader = new BufferedReader(file);
            int min_freq = Integer.parseInt(reader.readLine());
            return getReadableValue(min_freq) + "Hz";
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Reads and returns cpu maximum frequency
     * @return string with cpu maximum frequency and unit
     */
    public static String getCpuMaxFreq() {
        try {
            FileReader file = new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
            BufferedReader reader = new BufferedReader(file);
            int max_freq = Integer.parseInt(reader.readLine());
            return getReadableValue(max_freq) + "Hz";
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns the number of available cpu cores
     * @return string with the number of available cpu cores
     */
    public static String getCpuCoreNumber() {
        return Integer.toString(Runtime.getRuntime().availableProcessors());
    }
}
