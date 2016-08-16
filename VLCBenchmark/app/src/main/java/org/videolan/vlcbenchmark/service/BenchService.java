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

public class BenchService extends IntentService {

    //Message's what
    public static final int FAILURE = 0;
    public static final int FILE_TESTED_STATUS = 1;
    public static final int TEST_PASSED_STATUS = 2;
    public static final int DONE_STATUS = 3;
    public static final int PERCENT_STATUS = 4;
    public static final int PERCENT_STATUS_BITRATE = 5;

    //Percent tools
    private static final double JSON_FINISHED_PERCENT = 100.0 / 4;
    private static final double DOWNLOAD_FINISHED_PERCENT = 100.0;

    private static final String BASE_URL_MEDIA = "https://raw.githubusercontent.com/DaemonSnake/FileDump/master/";

    private List<MediaInfo> filesInfo = null;
    private Handler dispatcher = null;

    public BenchService() {
        super("BenchService");
    }

    @Override
    public void onDestroy() {
        dispatcher = null;
    }

    private static class InTestException extends Exception {
        FAILURE_STATES what;

        public InTestException(FAILURE_STATES what, String msg) {
            super(msg);
            this.what = what;
        }
    }

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

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    protected class Binder extends android.os.Binder {
        void sendData(Handler dispatcher) {
            synchronized (BenchService.this) {
                BenchService.this.dispatcher = dispatcher;
                BenchService.this.notifyAll();
            }
        }
    }

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

    private void sendMessage(int what, Object obj) {
        sendMessage(what, FAILURE_STATES.SUCCESS, obj);
    }

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
            if (checkFileSum(file, fileData.checksum))
                return;
            file.delete();
            throw new GeneralSecurityException(new Formatter().format("Media file '%s' is incorrect, aborting", fileData.url).toString());
        } finally {
            if (fileStream != null)
                fileStream.close();
            if (urlStream != null)
                urlStream.close();
        }
    }

    private void downloadFiles() throws IOException, GeneralSecurityException {
        filesInfo = JSonParser.getMediaInfos();

        sendMessage(PERCENT_STATUS, JSON_FINISHED_PERCENT);
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
                    continue;
                } else
                    localFile.delete();
            downloadFile(localFile, fileData, percent, 1.0 / filesInfo.size());
            fileData.localUrl = localFile.getAbsolutePath();
        }
        for (File toRemove : unusedFiles)
            toRemove.delete();
    }

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