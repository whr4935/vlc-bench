package org.videolan.vlcbenchmark.tools;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import org.videolan.vlcbenchmark.BuildConfig;
import org.videolan.vlcbenchmark.R;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class UploadResultsTask extends AsyncTask<String, Void, Boolean> {

    private final String TAG = UploadResultsTask.class.getName();
    private DialogInstance dialog;

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
                dialog = new DialogInstance(R.string.dialog_title_success, R.string.dialog_text_upload_success);
                success = true;
            }
            connection.disconnect();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_no_internet);
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
                dialog = new DialogInstance(R.string.dialog_title_success, R.string.dialog_text_upload_success);
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
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        if (dialog != null) {
            dialog.display(activity);
        }
    }
}

