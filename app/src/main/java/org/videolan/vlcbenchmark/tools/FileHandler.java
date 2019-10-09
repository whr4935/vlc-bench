/*
 *****************************************************************************
 * FileHandler.java
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

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

public class FileHandler {

    private final static String TAG = "FileHandler";

    public final static String jsonFolder = "jsonFolder";
    public final static String mediaFolder = "media_folder";
    public final static String screenshotFolder = "screenshot_folder";

    private final static String benchFolder =
            Environment.getExternalStorageDirectory() +  "/VLCBenchmark/";

    public static String getFolderStr(String name) {
        String folderStr;
        if (!checkFolderLocation(benchFolder)) {
            return null;
        }
        folderStr = benchFolder + name + "/";
        if (!checkFolderLocation(folderStr)) {
            return null;
        }
        return folderStr;
    }

    // Adding a nomedia to the media folder stops the vlc medialibrary from indexing the files
    // in the folder. Stops the benchmark from polluting the user's vlc library
    public static void setNoMediaFile() {
        String path = getFolderStr(mediaFolder);
        if (path == null) {
            Log.e(TAG, "setNoMediaFile: path is null");
            return;
        }
        path += ".nomedia";
        File nomediafile = new File(path);
        try {
            if (!nomediafile.createNewFile()) {
                Log.e(TAG, "setNoMediaFile: nomedia file was not created");
            }
        } catch (IOException e) {
            Log.e(TAG, "setNoMediaFile: failed to create nomedia file: " + e.toString());
        }
    }

    public static boolean checkFolderLocation(String name) {
        File folder = new File(name);
        boolean ret = true;
        if (!folder.exists()) {
            ret = folder.mkdir();
        }
        return ret;
    }

    public static void delete(final File file) {
        Util.runInBackground(new Runnable() {
            @Override
            public void run() {
                if (!file.delete()) {
                    Log.e(TAG, "Failed to delete file: " + file.getAbsolutePath());
                }
            }
        });
    }

    /**
     * Check if a file correspond to a sha512 key.
     *
     * @param file     the file to compare
     * @param checksum the wished value of the transformation of the file by using the sha512 algorithm.
     * @return true if the result of sha512 transformation is identical to the given String in argument (checksum).
     * @throws GeneralSecurityException if the algorithm is not found.
     * @throws IOException              if an IO error occurs while we read the file.
     */
    static boolean checkFileSum(File file, String checksum) throws GeneralSecurityException, IOException {
        MessageDigest algorithm;
        FileInputStream stream = null;

        try {
            stream = new FileInputStream(file);
            algorithm = MessageDigest.getInstance("SHA512");
            byte[] buff = new byte[2048];
            int read;
            while ((read = stream.read(buff, 0, 2048)) != -1)
                algorithm.update(buff, 0, read);
            buff = algorithm.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : buff) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString().equals(checksum);
        } finally {
            if (stream != null)
                stream.close();
        }
    }

    public static void deleteScreenshots() {
        Util.runInBackground(new Runnable() {
            @Override
            public void run() {
                File dir = new File(FileHandler.getFolderStr(screenshotFolder));
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.delete()) {
                            Log.e("VLCBench", "Failed to delete sample " + file.getName());
                            break;
                        }
                    }
                }
            }
        });
    }

    public static void checkFileSumAsync(File file, String checksum, IOnFileCheckedListener listener) {
        AppExecutors.INSTANCE.diskIO().execute(() -> {
            try {
                Boolean fileChecked = checkFileSum(file, checksum);
                Util.runInUiThread(() -> listener.onFileChecked(fileChecked));
            } catch (GeneralSecurityException|IOException e) {
                Util.runInUiThread(() -> listener.onFileChecked(false));
            }
        });
    }

    public interface IOnFileCheckedListener {
        void onFileChecked(Boolean valid);
    }
}
