package org.videolan.vlcbenchmark.tools;

import android.os.Environment;

import org.videolan.vlcbenchmark.BuildConfig;

import java.io.File;

public class FileHandler {

    public final static String jsonFolder = "jsonFolder";
    public final static String mediaFolder = "media_folder";
    public final static String screenshotFolder = "screenshot_folder";

    private final static String benchFolder =
            Environment.getExternalStorageDirectory() + File.separator + "VLCBenchmark" + File.separator;

    public static String getFolderStr(String name) {
        String folderStr;
        if (BuildConfig.DEBUG) {
            if (!checkFolderLocation(benchFolder)) {
                return null;
            }
            folderStr = benchFolder + name + File.separator;
        } else {
            folderStr = Environment.getDataDirectory().getAbsolutePath() + File.separator + name + File.separator;
        }
        if (!checkFolderLocation(folderStr)) {
            return null;
        }
        return folderStr;
    }

    public static boolean checkFolderLocation(String name) {
        File folder = new File(name);
        boolean ret = true;
        if (!folder.exists()) {
            ret = folder.mkdir();
        }
        return ret;
    }

}
