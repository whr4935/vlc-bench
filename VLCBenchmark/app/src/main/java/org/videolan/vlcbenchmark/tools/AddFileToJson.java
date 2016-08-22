/*****************************************************************************
 * AddFileToJson.java
 *****************************************************************************
 * Copyright © 2011-2014 VLC authors and VideoLAN
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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by penava_b on 18/08/16.
 */
public class AddFileToJson extends Activity implements Runnable {

//    static final Long timestamps[] = new Long[]{500L, 1500L, 2000L};
//    static final String fileName = "big_buck_bunny_480p_H264_AAC_25fps_1800K_short.MP4";
    static final String fileName = "Tractor_500kbps_x265.mp4";
    static final Long timestamps[] = new Long[]{500L, 1500L, 3000L};

    Integer[] snapshots = new Integer[timestamps.length];
    String sha512String;

    private static final String VLC_PACKAGE_NAME = "org.videolan.vlc.debug"; //TODO replace with release package
    private static final String SCREENSHOTS_EXTRA = "org.videolan.vlc.gui.video.benchmark.TIMESTAMPS";
    private static final String BENCH_ACTIVITY = "org.videolan.vlc.gui.video.benchmark.BenchActivity";
    private static final String SCREENSHOT_ACTION = "org.videolan.vlc.gui.video.benchmark.ACTION_SCREENSHOTS";
    private static final String SCREENSHOT_NAMING = "Screenshot_";
    private static final String FILE_URL = "https://raw.githubusercontent.com/Skantes/FileDump/master/";
    private static final String TAG = "AddFileToJSon";
    private static final String SHARED_PREFERENCE = "org.videolab.vlc.gui.video.benchmark.UNCAUGHT_EXCEPTIONS";
    private static final String SHARED_PREFERENCE_STACK_TRACE = "org.videolab.vlc.gui.video.benchmark.STACK_TRACE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        else
            new Thread(this).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
            new AlertDialog.Builder(this) {{
                setCancelable(false);
            }}.setMessage("Permission refused exiting...").setNeutralButton(android.R.string.ok, null).show();
        else
            new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            sha512String = getFileSum();
        } catch (Exception e) {
            sha512String = "FAILURE";
        }
        Log.e(TAG + ":key", sha512String);

        final Intent intent = new Intent(SCREENSHOT_ACTION)
                .setComponent(new ComponentName(VLC_PACKAGE_NAME, BENCH_ACTIVITY))
                .setDataAndTypeAndNormalize(Uri.parse(FILE_URL + fileName), "video/*")
                .putExtra(SCREENSHOTS_EXTRA, (Serializable) new ArrayList<Long>(Arrays.asList(timestamps)))
                .putExtra("disable_hardware", true);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startActivityForResult(intent, 42);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.e("TEST::", Integer.toString(resultCode));
        if (data == null) {
            String errorMessage;
            try {
                Context packageContext = createPackageContext(VLC_PACKAGE_NAME, 0);
                SharedPreferences preferences = packageContext.getSharedPreferences(SHARED_PREFERENCE, Context.MODE_PRIVATE);
                errorMessage = preferences.getString(SHARED_PREFERENCE_STACK_TRACE, null);
            } catch (PackageManager.NameNotFoundException e) {
                errorMessage = e.getMessage();
            }
            new AlertDialog.Builder(this) {{
                setCancelable(false);
            }}.setMessage(errorMessage).show();
            return;
        }
        final String screenshotFolder = data.getStringExtra("screenshot_folder");

        new Thread() {
            @Override
            public void run() {
                super.run();
                for (int i = 0; i < timestamps.length; i++) {
                    snapshots[i] = ScreenshotValidator.averageColorForImage(screenshotFolder + File.separator + SCREENSHOT_NAMING + i + ".jpg");
                    Log.e(TAG + ":snapshot", Integer.toString(snapshots[i]));
                    new File(screenshotFolder + File.separator + SCREENSHOT_NAMING + i + ".jpg").delete();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(AddFileToJson.this).setMessage("Finished see logs:\n" + "sha512: " + sha512String + '\n' + "colors: " + Arrays.toString(snapshots)).show();
                    }
                });
            }
        }.start();
    }

    private String getFileSum() throws GeneralSecurityException, IOException {
        MessageDigest algorithm;
        InputStream stream = null;

        try {
            stream = new URL(FILE_URL + fileName).openStream();
            algorithm = MessageDigest.getInstance("SHA512");
            byte[] buff = new byte[2048];
            int read = 0;
            while ((read = stream.read(buff, 0, 2048)) != -1)
                algorithm.update(buff, 0, read);
            buff = algorithm.digest();
            StringBuilder sb = new StringBuilder();
            for (byte aBuff : buff) {
                sb.append(Integer.toString((aBuff & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } finally {
            if (stream != null)
                stream.close();
        }
    }
}
