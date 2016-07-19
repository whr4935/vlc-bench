package org.videolan.vlcbenchmark.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

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

    private static final String DOMAIN_STR = "org.videolan.vlcbenchmark.service";

    //Actions
    private static final String ACTION_LAUNCH_SERVICE = DOMAIN_STR + "LAUNCH_SERVICE";
    public static final String DOWNLOAD_FAILURE = DOMAIN_STR + "DOWNLOAD_FAILURE";
    public static final String CHECKSUM_FAILURE = DOMAIN_STR + "CHECKSUM_FAILURE";
    public static final String FILE_TESTED_STATUS = DOMAIN_STR + "FILE_TESTED";
    public static final String TEST_PASSED_STATUS = DOMAIN_STR + "TEST_PASSED";
    public static final String DONE_STATUS = DOMAIN_STR + "DONE";
    public static final String PERCENT_STATUS = DOMAIN_STR + "PERCENT";

    //Arguments
    private static final String NUMBER_OF_TESTS = DOMAIN_STR + "NUMBER_OF_TESTS";
    public static final String EXTRA_CONTENT = DOMAIN_STR + "EXTRA_CONTENT";

    //Percent tools
    private static final double JSON_FINISHED_PERCENT = 100.0 / 8;
    private static final double DOWNLOAD_FINISHED_PERCENT = 100.0 / 4;
    private static final double DONE_PERCENT = 100.0;

    private static final String BASE_URL_MEDIA = "https://raw.githubusercontent.com/DaemonSnake/FileDump/master/";

    private List<MediaInfo> filesInfo = null;
    private static MediaInfo resumeMedia = null;

    public static void startService(Context context, BenchServiceDispatcher dispatcher, int numberOfTests) {
        Intent intent = new Intent(context, BenchService.class);
        intent.setAction(ACTION_LAUNCH_SERVICE);
        intent.putExtra(NUMBER_OF_TESTS, numberOfTests);
        LocalBroadcastManager.getInstance(context).registerReceiver(dispatcher, new IntentFilter(DOWNLOAD_FAILURE));
        LocalBroadcastManager.getInstance(context).registerReceiver(dispatcher, new IntentFilter(CHECKSUM_FAILURE));
        LocalBroadcastManager.getInstance(context).registerReceiver(dispatcher, new IntentFilter(FILE_TESTED_STATUS));
        LocalBroadcastManager.getInstance(context).registerReceiver(dispatcher, new IntentFilter(TEST_PASSED_STATUS));
        LocalBroadcastManager.getInstance(context).registerReceiver(dispatcher, new IntentFilter(DONE_STATUS));
        LocalBroadcastManager.getInstance(context).registerReceiver(dispatcher, new IntentFilter(PERCENT_STATUS));
        context.startService(intent);
    }

    public BenchService() {
        super("BenchService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || !ACTION_LAUNCH_SERVICE.equals(intent.getAction()))
            return;
        startService(intent.getIntExtra(NUMBER_OF_TESTS, 0));
    }

    private void reportStatus(String action, String extra) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(action).putExtra(EXTRA_CONTENT, extra));
    }

    private void reportStatus(String action, double extra) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(action).putExtra(EXTRA_CONTENT, extra));
    }

    private void reportStatus(String action, Exception exception) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(action).putExtra(EXTRA_CONTENT, exception));
    }

    private void startService(int numberOfLoops) {
        if (numberOfLoops <= 0)
            return;
        try {
            downloadFiles();
        } catch (IOException e) {
            reportStatus(DOWNLOAD_FAILURE, e);
            return;
        } catch (GeneralSecurityException e) {
            reportStatus(CHECKSUM_FAILURE, e);
            return;
        }
        mainLoop(numberOfLoops);
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

        reportStatus(PERCENT_STATUS, JSON_FINISHED_PERCENT);
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
                    reportStatus(PERCENT_STATUS, (DOWNLOAD_FINISHED_PERCENT - JSON_FINISHED_PERCENT) * percent + JSON_FINISHED_PERCENT);
                    continue;
                } else
                    localFile.delete();
            downloadFile(localFile, fileData);
            fileData.localUrl = localFile.getAbsolutePath();
            reportStatus(PERCENT_STATUS, (DOWNLOAD_FINISHED_PERCENT - JSON_FINISHED_PERCENT) * percent + JSON_FINISHED_PERCENT);
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

    private void mainLoop(int numberOfLoops) {
        double score = 100.0;

        for (int index = 0; index < numberOfLoops; index++) {
            for (MediaInfo fileData : filesInfo) {
                //reportStatus(TEST_PASSED_STATUS, (String) null);
                reportStatus(FILE_TESTED_STATUS, fileData.name);
            }
        }
        reportStatus(DONE_STATUS, score);
    }
}