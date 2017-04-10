package org.videolan.vlcbenchmark.tools;

import android.os.Environment;

import org.videolan.vlcbenchmark.BuildConfig;

import java.io.File;

public class FileHandler {

    public static String getFolderStr(String name) {
        String folderStr;
        if (BuildConfig.DEBUG) {
            folderStr = Environment.getExternalStorageDirectory() + File.separator + name + File.separator;
        } else {
            folderStr = null;
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
