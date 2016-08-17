package org.videolan.vlcbenchmark.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.util.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;

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

    //Message's what
    public static final int FAILURE = 0;
    public static final int DONE_STATUS = 1;
    public static final int PERCENT_STATUS = 2;
    public static final int PERCENT_STATUS_BITRATE = 3;
    public static final int STEP_FINISHED = 4;

    //Percent tools
    private static final double JSON_FINISHED_PERCENT = 100.0 / 4;
    private static final double DOWNLOAD_FINISHED_PERCENT = 100.0;

    private static final String BASE_URL_MEDIA = "https://raw.githubusercontent.com/DaemonSnake/FileDump/master/";

    //Steps strings
    private static final String JSON_FINISH_STR = "The list of the videos was correctly retrieved";
    private static final String DOWNLOAD_STR = "%s download finish";
    private static final String SHA512_SUCCESS_STR = "%s integrity check successful";
    private static final String SHA512_FAILED_STR = "%s integrity check failed";
    private static final String DOWNLOAD_INIT_STR = "Starting download of %s...";

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
            downloadFiles();
        } catch (IOException e) {
            sendMessage(FAILURE, FAILURE_STATES.CHECKSUM_FAILED, e);
            return;
        } catch (GeneralSecurityException e) {
            sendMessage(FAILURE, FAILURE_STATES.DOWNLOAD_FAILED, e);
            return;
        }
        sendMessage(DONE_STATUS, filesInfo);
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
         * @param dispatcher
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
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
        }
        dispatcher.sendMessage(dispatcher.obtainMessage(what, failure.ordinal(), 0, obj));
        if (what == DONE_STATUS || what == FAILURE)
            dispatcher = null;
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

    /**
     * Method responsible for handling the download of a single file
     * and checking its integrity afterward.
     * <p>
     * It also send {@link BenchService#PERCENT_STATUS} and {@link BenchService#STEP_FINISHED} events.
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
            throw new IOException("Cannot download the videos without WIFI, please connect to wifi and retry");
        }
        file.createNewFile();
        URL fileUrl = new URL(BASE_URL_MEDIA + fileData.url);
        FileOutputStream fileStream = null;
        InputStream urlStream = null;
        try {
            fileStream = new FileOutputStream(file);
            percent -= pas;
            pas /= fileUrl.openConnection().getContentLength();
            urlStream = fileUrl.openStream();
            byte[] buffer = new byte[2048];
            int read = 0;
            long fromTime = System.nanoTime(), toTime = 0;
            while ((read = urlStream.read(buffer, 0, 2048)) != -1) {
                toTime = System.nanoTime();
                fileStream.write(buffer, 0, read);
                percent += pas * read;
                double bitPerSeconds = read * 1_000_000_000d / (toTime - fromTime);
                sendMessage(PERCENT_STATUS_BITRATE, new Pair<Double, Long>((DOWNLOAD_FINISHED_PERCENT - JSON_FINISHED_PERCENT) * percent + JSON_FINISHED_PERCENT, (long) bitPerSeconds));
                fromTime = System.nanoTime();
            }
            sendMessage(STEP_FINISHED, String.format(DOWNLOAD_STR, fileData.name));
            if (checkFileSum(file, fileData.checksum)) {
                sendMessage(STEP_FINISHED, String.format(SHA512_SUCCESS_STR, fileData.name));
                return;
            }
            sendMessage(STEP_FINISHED, String.format(SHA512_FAILED_STR, fileData.name));
            file.delete();
            throw new GeneralSecurityException(new Formatter().format("Media file '%s' is incorrect, aborting", fileData.url).toString());
        } finally {
            if (fileStream != null)
                fileStream.close();
            if (urlStream != null)
                urlStream.close();
        }
    }

    /**
     * Obtain the list of {@link MediaInfo} by calling {@link JSonParser#getMediaInfos()},
     * prepare the environment to download the videos and
     * then check if they are already on the device and if so if the video is valid.
     * If its the case update the progress with assisted events, otherwise
     * call {@link BenchService#downloadFile(File, MediaInfo, double, double)}
     *
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private void downloadFiles() throws IOException, GeneralSecurityException {
        filesInfo = JSonParser.getMediaInfos();

        sendMessage(PERCENT_STATUS, JSON_FINISHED_PERCENT);
        sendMessage(STEP_FINISHED, JSON_FINISH_STR);
        File mediaFolder = new File(getFilesDir().getPath() + "/media_dir");
        if (!mediaFolder.exists())
            mediaFolder.mkdir();
        HashSet<File> unusedFiles = new HashSet<>(Arrays.asList(mediaFolder.listFiles()));
        double percent = 0d;
        for (MediaInfo fileData : filesInfo) {
            percent += 1.0 / filesInfo.size();
            File localFile = new File(mediaFolder.getPath() + '/' + fileData.name);
            if (localFile.exists())
                if (localFile.isFile() && checkFileSum(localFile, fileData.checksum)) {
                    fileData.localUrl = localFile.getAbsolutePath();
                    unusedFiles.remove(localFile);
                    sendMessage(PERCENT_STATUS, (DOWNLOAD_FINISHED_PERCENT - JSON_FINISHED_PERCENT) * percent + JSON_FINISHED_PERCENT);
                    sendMessage(STEP_FINISHED, String.format(SHA512_SUCCESS_STR, fileData.name));
                    continue;
                } else {
                    localFile.delete();
                    sendMessage(STEP_FINISHED, String.format(SHA512_FAILED_STR, fileData.name));
                }
            sendMessage(STEP_FINISHED, String.format(DOWNLOAD_INIT_STR, fileData.name));
            downloadFile(localFile, fileData, percent, 1.0 / filesInfo.size());
            fileData.localUrl = localFile.getAbsolutePath();
        }
        for (File toRemove : unusedFiles)
            toRemove.delete();
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
        DigestInputStream digest = null;

        try {
            stream = new FileInputStream(file);
            algorithm = MessageDigest.getInstance("SHA512");
            byte[] buff = new byte[2048];
            int read = 0;
            while ((read = stream.read(buff, 0, 2048)) != -1)
                algorithm.update(buff, 0, read);
            long time1 = System.currentTimeMillis();
            buff = algorithm.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < buff.length; i++) {
                sb.append(Integer.toString((buff[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString().equals(checksum);
        } finally {
            if (stream != null)
                stream.close();
            if (digest != null)
                digest.close();
        }
    }
}