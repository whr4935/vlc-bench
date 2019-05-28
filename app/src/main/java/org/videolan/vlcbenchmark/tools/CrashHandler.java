/*
 *****************************************************************************
 * CrashHandler.java
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

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler defaultUEH;

    private CrashHandler() { this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler(); }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {

        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);

        // Getting stacktrace
        StackTraceElement[] trace = ex.getStackTrace();
        StackTraceElement[] trace2 = new StackTraceElement[trace.length + 3];
        System.arraycopy(trace, 0, trace2, 0, trace.length);
        trace2[trace.length] = new StackTraceElement("Android", "MODEL", android.os.Build.MODEL, -1);
        trace2[trace.length + 1] = new StackTraceElement("Android", "VERSION", android.os.Build.VERSION.RELEASE, -1);
        trace2[trace.length + 2] = new StackTraceElement("Android", "FINGERPRINT", android.os.Build.FINGERPRINT, -1);
        ex.setStackTrace(trace2);

        ex.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();

        // Writing log file
        FileOutputStream fileOutputStream;
        String fileName = FormatStr.getDateStr() + "_VLCBenchmark-Crash.log";
        String folderName = FileHandler.getFolderStr("VLCBenchmark_crashLogs");
        if (FileHandler.checkFolderLocation(folderName)) {
            File file = new File(folderName + fileName);
            try {
                fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(stacktrace.getBytes());
            } catch (IOException e) {
                Log.e("VLCBench", "Failed to write crash log: " + e.toString());
            }
        } else {
            Log.e("VLCBench", "Failed to create crash log folder");
        }

        this.defaultUEH.uncaughtException(thread, ex);
    }

    public static boolean setCrashHandler() {
        try {
            Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
