/*****************************************************************************
 * FileHandler.java
 *****************************************************************************
 * Copyright © 2017 VLC authors and VideoLAN
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

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.videolan.vlcbenchmark.BuildConfig;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileHandler {

    private final static String TAG = "FileHandler";

    public final static String jsonFolder = "jsonFolder";
    public final static String mediaFolder = "media_folder";
    public final static String screenshotFolder = "screenshot_folder";
    public static ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
    public static Handler mHandler = new Handler(Looper.getMainLooper());

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

    public static void delete(final String filepath) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                File file = new File(filepath);
                if (!file.delete()) {
                    Log.e(TAG, "Failed to delete file: " + file.getName());
                }
            }
        });
    }

    public static void delete(final File file) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                if (!file.delete()) {
                    Log.e(TAG, "Failed to delete file: " + file.getName());
                }
            }
        });
    }

}
