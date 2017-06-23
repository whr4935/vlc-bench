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
