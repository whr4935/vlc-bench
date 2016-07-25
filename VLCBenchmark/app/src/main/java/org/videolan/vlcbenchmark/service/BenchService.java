package org.videolan.vlcbenchmark.service;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;

import org.videolan.vlcbenchmark.ScreenshotActivity;

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
import java.util.concurrent.CountDownLatch;

public class BenchService extends IntentService implements Runnable {

    //Message's what
    public static final int FAILURE = 0;
    public static final int FILE_TESTED_STATUS = 1;
    public static final int TEST_PASSED_STATUS = 2;
    public static final int DONE_STATUS = 3;
    public static final int PERCENT_STATUS = 4;

    //Percent tools
    private static final double JSON_FINISHED_PERCENT = 100.0 / 8;
    private static final double DOWNLOAD_FINISHED_PERCENT = 100.0 / 4;
    private static final double DONE_PERCENT = 100.0;

    private static final String BASE_URL_MEDIA = "https://raw.githubusercontent.com/DaemonSnake/FileDump/master/";

    private List<MediaInfo> filesInfo = null;
    private Handler dispatcher = null;
    private int numberOfLoops;
    private static MediaInfo resumeMedia = null;

    public BenchService() {
        super("BenchService");
    }

    @Override
    public void onDestroy() {
        dispatcher = null;
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
        mainLoop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    private CountDownLatch screenshotSynchronize = null;

    protected class Binder extends android.os.Binder {
        void sendData(int numberOfLoops, Handler dispatcher) {
            synchronized (BenchService.this) {
                BenchService.this.numberOfLoops = numberOfLoops;
                BenchService.this.dispatcher = dispatcher;
                BenchService.this.notifyAll();
            }
        }

        Runnable getScreenshotIsDone() {
            return BenchService.this;
        }
    }

    @Override
    public void run() {
        if (screenshotSynchronize != null)
            screenshotSynchronize.countDown();
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
        if (hasWifiAndLan(this)) {
            resumeMedia = fileData;
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
            while ((read = urlStream.read(buffer, 0, 2048)) != -1) {
                fileStream.write(buffer, 0, read);
                percent += pas * read;
                sendMessage(PERCENT_STATUS, (DOWNLOAD_FINISHED_PERCENT - JSON_FINISHED_PERCENT) * percent + JSON_FINISHED_PERCENT);
            }
            if (checkFileSum(file, fileData.checksum))
                return;
            file.delete();
            resumeMedia = fileData;
            throw new GeneralSecurityException(new Formatter().format("Media file '%s' is incorrect, aborting", fileData.url).toString());
        } finally {
            if (fileStream != null)
                fileStream.close();
            if (urlStream != null)
                urlStream.close();
        }
    }

    private void downloadFiles() throws IOException, GeneralSecurityException {
        if (filesInfo != null)
            return;
        filesInfo = JSonParser.getMediaInfos();

        sendMessage(PERCENT_STATUS, JSON_FINISHED_PERCENT);
        File mediaFolder = new File(getFilesDir().getPath() + "/media_dir");
        if (!mediaFolder.exists())
            mediaFolder.mkdir();
        HashSet<File> unusedFiles = new HashSet<>(Arrays.asList(mediaFolder.listFiles()));
        int offset = (resumeMedia == null ? 0 : filesInfo.indexOf(resumeMedia));
        if (offset == -1) {
            resumeMedia = null;
            offset = 0;
        }
        List<MediaInfo> subList = filesInfo.subList(offset, filesInfo.size());
        double percent = 1.0 / filesInfo.size() * offset;
        for (MediaInfo fileData : subList) {
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
        resumeMedia = null;
        for (File toRemove : unusedFiles)
            toRemove.delete();
    }

    private boolean checkFileSum(File file, String checksum) throws GeneralSecurityException, IOException {
        MessageDigest algorithm;
        FileInputStream stream = null;
        DigestInputStream digest = null;

        try {
            stream = new FileInputStream(file);
            digest = new DigestInputStream(stream, (algorithm = MessageDigest.getInstance("SHA512")));
            while (digest.read() != -1)
                ;
            byte[] bArray = algorithm.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < bArray.length; i++) {
                sb.append(Integer.toString((bArray[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString().equals(checksum);
        } finally {
            if (stream != null)
                stream.close();
            if (digest != null)
                digest.close();
        }
    }

    private static final double NUMBER_OF_TESTS_PER_FILE = 4;

    public static void getFinishedCallback(Context context, final IServiceConnected serviceConnected) {
        context.bindService(new Intent(context, BenchService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                serviceConnected.onConnect(this, ((Binder)iBinder).getScreenshotIsDone());
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        }, Context.BIND_AUTO_CREATE);

    }

    private Score testFile(int loopIndex, MediaInfo info, double percent, double pas) {
        TestInfo testStats = new TestInfo(info.name, loopIndex);
        for (int i = 0; i < NUMBER_OF_TESTS_PER_FILE; i++) {
            //Insert testing here
            percent += pas;
            sendMessage(PERCENT_STATUS, percent);
        }
        sendMessage(FILE_TESTED_STATUS, testStats);
        return testStats.score;
    }

    private void mainLoop() {
        Score score = new Score();
        double percent = DOWNLOAD_FINISHED_PERCENT;
        double pas = (DONE_PERCENT - DOWNLOAD_FINISHED_PERCENT) / (Double.valueOf(numberOfLoops) * filesInfo.size());

        for (int loopIndex = 0; loopIndex < numberOfLoops; loopIndex++) {
            for (MediaInfo fileData : filesInfo) {
                score.add(testFile(loopIndex, fileData, percent, pas / NUMBER_OF_TESTS_PER_FILE));
                percent += pas;
            }
        }
        sendMessage(DONE_STATUS, score.avrage(numberOfLoops * filesInfo.size()));
    }
}