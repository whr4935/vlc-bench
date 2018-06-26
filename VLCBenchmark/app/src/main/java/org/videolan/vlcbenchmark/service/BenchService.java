/*****************************************************************************
 * BenchService.java
 *****************************************************************************
 * Copyright © 2016-2017 VLC authors and VideoLAN
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

package org.videolan.vlcbenchmark.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import org.videolan.vlcbenchmark.BuildConfig;
import org.videolan.vlcbenchmark.R;
import org.videolan.vlcbenchmark.tools.DialogInstance;
import org.videolan.vlcbenchmark.tools.FileHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * This class is the service responsible for:
 * -downloading the JSon file
 * -checking if the medias listed in the JSon file are present
 * -checking if they are corresponding with our version of the files
 * -downloading them if not present or incorrect
 * <p>
 * During this whole process it will forward information about its progress
 * through a Handler to the calling activity.
 * <p>
 * Those information are of the following nature (accompanied with the data type it transmit) :
 * -the service failed : FAILURE_STATES + Exception
 * -the service is done : List<MediaInfo>
 * -the service's progress rate or progress rate and download speed : double or Pair<Double, Long>
 * -the service has finished a step in its process : String
 */
public class BenchService extends IntentService {

    private static final String TAG = "BenchService";

    //Message's what
    public static final int FAILURE = 0;
    public static final int DONE_STATUS = 1;
    public static final int PERCENT_STATUS = 2;
    public static final int PERCENT_STATUS_BITRATE = 3;
    public static final int STEP_FINISHED = 4;
    public static final int FILE_CHECK = 5;
    public static final int DONE_DOWNLOAD = 6;
    public static final int NO_INTERNET = 7;
    public static final int DIALOG = 8;

    public static final class FileCheckContext {
        public static final int check = 1;
        public static final int download = 2;
    }

    /**
     * Field holding the result of the service,
     * the list of MediaInfo for all videos/media.
     */
    private List<MediaInfo> filesInfo = null;

    /**
     * Field holding an instance of {@link Handler} used by the service
     * to exchange data & information with the calling activity.
     * <p>
     * In the current implantation the instance set in dispatcher is of the underling
     * type {@link BenchServiceDispatcher}.
     */
    private Handler dispatcher = null;

    /**
     * Default constructor
     */
    public BenchService() {
        super("BenchService");
    }

    /**
     * Gets rid of @see {@link BenchService#dispatcher} if it's was not already done
     */
    @Override
    public void onDestroy() {
        dispatcher = null;
    }

    /**
     * Entry point of the service.
     * <p>
     * It calls the main method {@link BenchService#downloadFiles()}
     * and catch its exceptions.
     * <p>
     * If an exception is caught a {@link BenchService#FAILURE} event will be send,
     * otherwise a {@link BenchService#DONE_STATUS} event.
     *
     * @param intent ignored, the service's behaviour is always the same
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            int action = intent.getIntExtra("action", ServiceActions.SERVICE_UNKNOWN);
            switch (action) {
                case ServiceActions.SERVICE_CONNECT:
                    Log.i(TAG, "Connected");
                    break;
                case ServiceActions.SERVICE_CHECKFILES:
                    checkFiles(intent.getIntExtra("context", FileCheckContext.check));
                    break;
                case ServiceActions.SERVICE_DOWNLOAD:
                    downloadFiles();
                    break;
                case ServiceActions.SERVICE_POST:
                    UploadJson(intent.getStringExtra("json"));
                    break;
                default:
                    Log.e(TAG, "Unknown service action requested");
                    break;
            }
            // TODO if IOexception is only related to no internet
            // remove it and handle at local level
        } catch (IOException e) {
            sendMessage(FAILURE, FAILURE_STATES.DOWNLOAD_FAILED, e);
        } catch (GeneralSecurityException e) {
            sendMessage(FAILURE, FAILURE_STATES.CHECKSUM_FAILED, e);
        }
    }

    /**
     * Method called when an activity tries to bind itself with BenchService.
     * <p>
     * The activity must downcast the received IBinder instance to {@link BenchService.Binder}
     * and call the {@link BenchService.Binder#sendData(Handler)} on it with its {@link Handler}
     * <p>
     * Until this is not done the BenchService will hang until it is.
     *
     * @param intent ignored
     * @return a new instance of {@link BenchService.Binder}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    /**
     * Tool class that allows to an Activity and an instance of BenchService to exchange a {@link Handler}
     * to communicate directly.
     *
     * @see BenchService#onBind(Intent)
     */
    protected class Binder extends android.os.Binder {

        /**
         * This method allows our BenchService instance to receive a {@link Handler} to communicate with the
         * calling activity.
         * <p>
         * Must be called by the Activity on the result of binding with BenchService, otherwise BenchService will
         * hang indefinitely.
         *
         * @param dispatcher to UI thread
         * @see BenchService#sendMessage(int, FAILURE_STATES, Object)
         */
        void sendData(Handler dispatcher) {
            synchronized (BenchService.this) {
                BenchService.this.dispatcher = dispatcher;
                BenchService.this.notifyAll();
            }
        }
    }

    /**
     * This is the method that is responsible to send information (a code and an object)
     * to the main activity on its UI thread.
     * <p>
     * If its only mean of communication ({@link BenchService#dispatcher} a {@link Handler}) is
     * not set this method will wait until being notified or interrupted.
     * <p>
     * Putting this wait in this method instead of in the {@link BenchService#onHandleIntent(Intent)} allows the service to start working
     * sooner and therefore reduce the amount of time waiting for BenchActivity and the Activity to handshake.
     * <p>
     * It its directly called by this class only in {@link BenchService#FAILURE} cases otherwise {@link BenchService#sendMessage(int, Object)} is called.
     *
     * @param what    the message code to send to the {@link Handler}
     * @param failure enum value representing the the reason why the BenchService failed
     * @param obj     the object to put inside the message for the Activity
     * @see BenchService#onBind(Intent)
     */
    private void sendMessage(int what, FAILURE_STATES failure, Object obj) {
        synchronized (this) {
            if (dispatcher == null)
                try {
                    Log.i(TAG, "dispatcher is null: Waiting");
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
        }
        dispatcher.sendMessage(dispatcher.obtainMessage(what, failure.ordinal(), 0, obj));
    }

    /**
     * Simplification of {@link BenchService#sendMessage(int, FAILURE_STATES, Object)}
     * used when the message is not a {@link BenchService#FAILURE} one
     *
     * @param what the message code to send to the {@link Handler}
     * @param obj  the object to put inside the message
     * @see BenchService#sendMessage(int, FAILURE_STATES, Object)
     */
    private void sendMessage(int what, Object obj) {
        sendMessage(what, FAILURE_STATES.SUCCESS, obj);
    }

    /**
     * Tool method to check if the device is currently connected to WIFI or LAN
     *
     * @param context the application's context to retrieve the connectivity service.
     * @return true if connected to WIFI or LAN else false
     */
    private static boolean hasWifiAndLan(Context context) {
        boolean networkEnabled = false;
        ConnectivityManager connectivity = (ConnectivityManager) (context.getSystemService(Context.CONNECTIVITY_SERVICE));
        if (connectivity != null) {
            NetworkInfo networkInfo = connectivity.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected() &&
                    (networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                networkEnabled = true;
            }
        }
        return networkEnabled;
    }


    private void httpJsonUpload(String json, String url) {
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
            } else {
                Log.i(TAG, "Api response: " + connection.getResponseCode() + " - " + connection.getResponseMessage());
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            sendMessage(DIALOG, new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_no_internet));
        }
    }

    private void httpsJsonUpload(String json, String url) {
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
                sendMessage(DIALOG, new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_err_upload));
            } else {
                Log.i(TAG, "Api response: " + connection.getResponseCode() + " - " + connection.getResponseMessage());
                sendMessage(DIALOG, new DialogInstance(R.string.dialog_title_success, R.string.dialog_text_upload_success));
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            sendMessage(DIALOG, new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_err_upload));
        }
    }

    /**
     *  Uploads json result file to server
     * @param json json string from JsonObject.toString()
     */
    private void UploadJson(String json) {
            if (BuildConfig.DEBUG) {
                httpJsonUpload(json, this.getString(R.string.build_api_address));
            } else {
                httpsJsonUpload(json, this.getString(R.string.build_api_address));
            }
    }

    /**
     * Method responsible for handling the download of a single file
     * and checking its integrity afterward.
     * <p>
     *
     * @param file     a {@link File} to represent the distant local file that this method will create and fill
     * @param fileData metadata about the media.
     * @param percent  current BenchService's progress ratio in percentage
     * @param pas      the percentage value of finishing the download of one file.
     * @throws IOException              if the device is not connected to WIFI or LAN {@link BenchService#hasWifiAndLan(Context)} or if the download failed due to an IO error.
     * @throws GeneralSecurityException thrown only if the downloaded file doesn't match the signature (SHA-512) contained in fileData
     */
    private void downloadFile(File file, MediaInfo fileData, double percent, double pas) throws IOException, GeneralSecurityException {
        if (!hasWifiAndLan(this)) {
            Log.e(TAG, "There is no wifi");
            throw new IOException("Cannot download the videos without WIFI, please connect to wifi and retry");
        }
        file.createNewFile();
        URL fileUrl = new URL(this.getResources().getString(R.string.file_location_url) + fileData.url);
        FileOutputStream fileStream = null;
        InputStream urlStream = null;
        try {
            fileStream = new FileOutputStream(file);
            percent -= pas;
            pas /= fileUrl.openConnection().getContentLength();
            urlStream = fileUrl.openStream();
            byte[] buffer = new byte[2048];
            int read;
            long fromTime = System.nanoTime(), toTime;
            while ((read = urlStream.read(buffer, 0, 2048)) != -1) {
                toTime = System.nanoTime();
                fileStream.write(buffer, 0, read);
                percent += pas * read;
                double bitPerSeconds = read * 1_000_000_000d / (toTime - fromTime);
                sendMessage(PERCENT_STATUS_BITRATE, new Pair<Double, Long>(100d * percent, (long) bitPerSeconds));
                fromTime = System.nanoTime();
            }
            if (!checkFileSum(file, fileData.checksum)) {
                FileHandler.delete(file);
                throw new GeneralSecurityException(new Formatter().format("Media file '%s' is incorrect, aborting", fileData.url).toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to download file : " + e.toString());

        } finally {
            if (fileStream != null)
                fileStream.close();
            if (urlStream != null)
                urlStream.close();
        }
    }

    //TODO add file deletion or take it out of download files
    //TODO create new download list as to loop only on files to download
    private void checkFiles(int context) {
        if (!hasWifiAndLan(this)) {
            Log.e(TAG, "There is no wifi.");
            sendMessage(FILE_CHECK, false);
            if (context == FileCheckContext.download) {
                sendMessage(FAILURE, FAILURE_STATES.DOWNLOAD_FAILED,
                        new IOException("Cannot download the videos without WIFI, please connect to wifi and retry"));
            }
        }
        ArrayList<MediaInfo> filesToDownload;
        try {
            filesInfo = JSonParser.getMediaInfos(this);
            String dirStr = FileHandler.getFolderStr(FileHandler.mediaFolder);
            if (dirStr == null) {
                sendMessage(BenchService.DIALOG, new DialogInstance(R.string.dialog_title_oups, R.string.dialog_text_file_creation_failure));
                return;
            }
            File dir = new File(dirStr);
            File[] files = dir.listFiles();
            filesToDownload = new ArrayList<>();
            for (MediaInfo mediaFile : filesInfo) {
                boolean presence = false;
                if (files != null) {
                    for (File localFile : files) {
                        if (localFile.getName().equals(mediaFile.getName())) {
                            if (!checkFileSum(localFile, mediaFile.checksum)) {
                                mediaFile.localUrl = localFile.getAbsolutePath();
                                FileHandler.delete(localFile);
                                filesToDownload.add(mediaFile);
                            }
                            mediaFile.localUrl = localFile.getAbsolutePath();
                            presence = true;
                            break;
                        }
                    }
                }
                if (!presence) {
                    filesToDownload.add(mediaFile);
                }
            }
        } catch (IOException | GeneralSecurityException e) {
            Log.e(TAG, "Failed to check files: " + e.toString());
            sendMessage(BenchService.FILE_CHECK, false);
            sendMessage(BenchService.DIALOG, new DialogInstance(R.string.dialog_title_oups, R.string.dialog_text_sample));
            return;
        }
        sendMessage(FILE_CHECK, true);
        if (filesToDownload.size() == 0) {
            sendMessage(DONE_STATUS, filesInfo);
        } else if (context == FileCheckContext.download) {
            try { //TODO handle exceptions on local level -> this is a duplicate from handleIntent
                downloadFiles();
            } catch (IOException e) {
                sendMessage(FAILURE, FAILURE_STATES.DOWNLOAD_FAILED, e);
            } catch (GeneralSecurityException e) {
                sendMessage(FAILURE, FAILURE_STATES.CHECKSUM_FAILED, e);
            }
        }
    }

    /**
     * Obtain the list of {@link MediaInfo} by calling {@link JSonParser#getMediaInfos(Context)},
     * prepare the environment to download the videos and
     * then check if they are already on the device and if so if the video is valid.
     * If its the case update the progress with assisted events, otherwise
     * call {@link BenchService#downloadFile(File, MediaInfo, double, double)}
     *
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private void downloadFiles() throws IOException, GeneralSecurityException {
        if (!hasWifiAndLan(this)) {
            Log.e(TAG, "Downloading Filessss no wifi !!");
            throw new IOException("Cannot download the videos without WIFI, please connect to wifi and retry");
        }
        filesInfo = JSonParser.getMediaInfos(this);

        sendMessage(PERCENT_STATUS, 0d);
        String mediaFolderStr = FileHandler.getFolderStr(FileHandler.mediaFolder);
        if (mediaFolderStr == null) {
            Log.e(TAG, "Failed to get media directory");
            sendMessage(BenchService.DIALOG, new DialogInstance(R.string.dialog_title_oups, R.string.dialog_text_download_error));
            return;
        }
        File mediaFolder = new File(mediaFolderStr);
        HashSet<File> unusedFiles = new HashSet<>(Arrays.asList(mediaFolder.listFiles()));
        double percent = 0d;
        for (MediaInfo fileData : filesInfo) {
            File localFile = new File(mediaFolder.getPath() + '/' + fileData.name);
            if (localFile.exists())
                if (localFile.isFile() && checkFileSum(localFile, fileData.checksum)) {
                    fileData.localUrl = localFile.getAbsolutePath();
                    unusedFiles.remove(localFile);
                    sendMessage(PERCENT_STATUS, 100d * percent);
                    continue;
                } else {
                    FileHandler.delete(localFile);
                }
            percent += 1.0 / filesInfo.size();
            downloadFile(localFile, fileData, percent, 1.0 / filesInfo.size());
            fileData.localUrl = localFile.getAbsolutePath();
        }
        for (File toRemove : unusedFiles) {
            FileHandler.delete(toRemove);
        }
        sendMessage(DONE_DOWNLOAD, true);
        sendMessage(DONE_STATUS, filesInfo);
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
    private boolean checkFileSum(File file, String checksum) throws GeneralSecurityException, IOException {
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
}