/*
 *****************************************************************************
 * UploadResultsTask.java
 *****************************************************************************
 * Copyright © 2018 VLC authors and VideoLAN
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.videolan.vlcbenchmark.BuildConfig;
import org.videolan.vlcbenchmark.R;
import org.videolan.vlcbenchmark.ResultPage;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class UploadResultsTask extends AsyncTask<String, Void, Boolean> {

    private final String TAG = UploadResultsTask.class.getName();
    private DialogInstance dialog = null;

    private Activity activity;

    public UploadResultsTask(Activity activity) {
        this.activity = activity;
    }

    private boolean httpJsonUpload(String json, String url) {
        boolean success;
        try {
            HttpURLConnection connection;
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("POST");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(json);
            writer.flush();
            int response = connection.getResponseCode();
            if (response != 200) {
                Log.e(TAG, "Api response: " + connection.getResponseCode() + " - " + connection.getResponseMessage());
                dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_err_upload);
                success = false;
            } else {
                Log.i(TAG, "Api response: " + connection.getResponseCode() + " - " + connection.getResponseMessage());
                success = true;
            }
            connection.disconnect();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_err_upload);
            success = false;
        }
        return success;
    }

    private boolean httpsJsonUpload(String json, String url) {
        boolean success;
        try {
            HttpsURLConnection connection;
            connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("POST");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(json);
            writer.flush();
            int response = connection.getResponseCode();
            if (response != 200) {
                Log.e(TAG, "Api response: " + connection.getResponseCode() + " - " + connection.getResponseMessage());
                dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_err_upload);
                success = false;
            } else {
                Log.i(TAG, "Api response: " + connection.getResponseCode() + " - " + connection.getResponseMessage());
                success = true;
            }
            connection.disconnect();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_err_upload);
            success = false;
        }
        return success;
    }

    /**
     *  Uploads json result file to server
     * @param strings json string from JsonObject.toString()
     */
    @Override
    protected Boolean doInBackground(String... strings) {
        boolean success;
        if (strings.length >= 1) {
            String json = strings[0];
            if (BuildConfig.DEBUG) {
                success = httpJsonUpload(json, activity.getString(R.string.build_api_address));
            } else {
                success = httpsJsonUpload(json, activity.getString(R.string.build_api_address));
            }
        } else {
            Log.e(TAG, "doInBackground: json result string is null" );
            dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_err_upload);
            success = false;
        }
        return success;
    }

    @Override
    protected void onCancelled() {
        Log.w(TAG, "onCancelled: Result upload was cancelled");
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if (activity instanceof IUploadResultsTask) {
            ((IUploadResultsTask)activity).dismissProgressDialog();
        }
        if (dialog != null && !success) {
            dialog.display(activity);
        } else if (success) {
            DialogInterface.OnClickListener websiteLink = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://bench.videolabs.io"));
                    activity.startActivity(browserIntent);
                }
            };
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.dialog_title_success)
                    .setMessage(R.string.dialog_text_upload_success)
                    .setNeutralButton(R.string.dialog_btn_visit, websiteLink)
                    .setNegativeButton(R.string.dialog_btn_continue, null)
                    .show();
        }
    }

    public interface IUploadResultsTask {
        void dismissProgressDialog();
    }
}

