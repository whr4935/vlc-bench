package org.videolan.vlcbenchmark.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

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
    public static final int DOWNLOAD_FAILURE = 0;
    public static final int CHECKSUM_FAILURE = 1;
    public static final int FILE_TESTED_STATUS = 2;
    public static final int TEST_PASSED_STATUS = 3;
    public static final int DONE_STATUS = 4;
    public static final int PERCENT_STATUS = 5;

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
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            downloadFiles();
        } catch (IOException e) {
            sendMessage(DOWNLOAD_FAILURE, e);
            return;
        } catch (GeneralSecurityException e) {
            sendMessage(CHECKSUM_FAILURE, e);
            return;
        }
        mainLoop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    protected class Binder extends android.os.Binder {
        void sendData(int numberOfLoops, Handler dispatcher) {
            BenchService.this.numberOfLoops = numberOfLoops;
            BenchService.this.dispatcher = dispatcher;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    private void sendMessage(int what, Object obj) {
        if (dispatcher == null)
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        dispatcher.sendMessage(dispatcher.obtainMessage(what, obj));
    }

    private void downloadFile(File file, MediaInfo fileData) throws IOException, GeneralSecurityException {
        file.createNewFile();
        URL fileUrl = new URL(BASE_URL_MEDIA + fileData.url);
        FileOutputStream fileStream = null;
        InputStream urlStream = null;
        try {
            fileStream = new FileOutputStream(file);
            urlStream = fileUrl.openStream();
            byte[] buffer = new byte[2048];
            int read = 0;
            while ((read = urlStream.read(buffer, 0, 2048)) != -1)
                fileStream.write(buffer, 0, read);
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
            downloadFile(localFile, fileData);
            fileData.localUrl = localFile.getAbsolutePath();
            sendMessage(PERCENT_STATUS, (DOWNLOAD_FINISHED_PERCENT - JSON_FINISHED_PERCENT) * percent + JSON_FINISHED_PERCENT);
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

    public Score testFile(int loopIndex, MediaInfo info, double percent, double pas) {
        Score score = new Score();
        for (int i = 0; i < NUMBER_OF_TESTS_PER_FILE; i++) {
            //Insert testing here
            percent += pas;
            sendMessage(PERCENT_STATUS, percent);
        }
        sendMessage(FILE_TESTED_STATUS, new TestInfo(info.name, score, loopIndex));
        return score;
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