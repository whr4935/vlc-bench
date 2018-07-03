package org.videolan.vlcbenchmark.tools;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import org.videolan.vlcbenchmark.MainPage;
import org.videolan.vlcbenchmark.R;
import org.videolan.vlcbenchmark.service.JSonParser;
import org.videolan.vlcbenchmark.service.MediaInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;

public class DownloadFilesTask extends AsyncTask<Void, Pair, Boolean> {

    private final String TAG = DownloadFilesTask.class.getName();
    private DialogInstance dialog;
    private List<MediaInfo> mFilesInfo;

    private Activity activity;

    public DownloadFilesTask(Activity activity) {
        this.activity = activity;
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
     */
    private void downloadFile(File file, MediaInfo fileData, double percent, double pas) throws IOException {
        if (!Util.hasWifiAndLan(activity)) {
            Log.e(TAG, "There is no wifi");
            throw new IOException("Cannot download the videos without WIFI, please connect to wifi and retry");
        }
        file.createNewFile();
        URL fileUrl = new URL(activity.getString(R.string.file_location_url) + fileData.getUrl());
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
                publishProgress(new Pair<Double, Long>(100d * percent, (long)bitPerSeconds));
                fromTime = System.nanoTime();
            }
            if (!FileHandler.checkFileSum(file, fileData.getChecksum())) {
                FileHandler.delete(file);
                throw new GeneralSecurityException(new Formatter().format("Media file '%s' is incorrect, aborting", fileData.getUrl()).toString());
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

    /**
     * Obtain the list of {@link MediaInfo} by calling {@link JSonParser#getMediaInfos(Context)},
     * prepare the environment to download the videos and
     * then check if they are already on the device and if so if the video is valid.
     */
    private boolean downloadFiles() {
        if (!Util.hasWifiAndLan(activity)) {
            Log.e(TAG, "downloadFiles: no wifi");
            dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_no_wifi);
            return false;
        }
        try {
            mFilesInfo = JSonParser.getMediaInfos(activity);
            String mediaFolderStr = FileHandler.getFolderStr(FileHandler.mediaFolder);
            if (mediaFolderStr == null) {
                Log.e(TAG, "Failed to get media directory");
                dialog = new DialogInstance(R.string.dialog_title_oups, R.string.dialog_text_download_error);
                return false;
            }
            File mediaFolder = new File(mediaFolderStr);
            HashSet<File> unusedFiles = new HashSet<>(Arrays.asList(mediaFolder.listFiles()));
            double percent = 0d;
            for (MediaInfo fileData : mFilesInfo) {
                File localFile = new File(mediaFolder.getPath() + '/' + fileData.getName());
                if (localFile.exists())
                    if (localFile.isFile() && FileHandler.checkFileSum(localFile, fileData.getChecksum())) {
                        fileData.setLocalUrl(localFile.getAbsolutePath());
                        unusedFiles.remove(localFile);
                        continue;
                    } else {
                        FileHandler.delete(localFile);
                    }
                percent += 1.0 / mFilesInfo.size();
                downloadFile(localFile, fileData, percent, 1.0 / mFilesInfo.size());
                fileData.setLocalUrl(localFile.getAbsolutePath());
            }
            for (File toRemove : unusedFiles) {
                FileHandler.delete(toRemove);
            }
        } catch (Exception e) {
            Log.e(TAG, "downloadFiles: " + e.toString());
            return false;
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Pair... values) {
        super.onProgressUpdate(values);
        if (activity instanceof MainPage && values.length >= 1) {
            MainPage mainPage = (MainPage) activity;
            Pair<Double, Long> progressValues = values[0];
            mainPage.updatePercent(progressValues.first, progressValues.second);
        }
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        return downloadFiles();
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if (activity instanceof MainPage) {
            ((MainPage) activity).setFilesDownloaded(success);
            if (success) {
                ((MainPage) activity).setBenchmarkFiles(mFilesInfo);
            }
        }
        if (dialog != null) {
            dialog.display(activity);
        }
    }
}
