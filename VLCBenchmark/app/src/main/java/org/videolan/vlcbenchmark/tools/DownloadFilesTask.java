/*
 *****************************************************************************
 * DownloadFilesTask.java
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

import android.content.Context;
import android.os.AsyncTask;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.util.Pair;

import org.videolan.vlcbenchmark.MainPageFragment;
import org.videolan.vlcbenchmark.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
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

    private Fragment fragment;

    public DownloadFilesTask(Fragment fragment) {
        this.fragment = fragment;
    }

    /**
     * Method responsible for handling the download of a single file
     * and checking its integrity afterward.
     * <p>
     *
     * @param file     a {@link File} to represent the distant local file that this method will create and fill
     * @param fileData metadata about the media.
     * @param percent  current BenchService's progress ratio in percentage
     * @param totalSize the total size of all file samples to download.
     * @throws IOException if the device is not connected to WIFI or LAN or if the download failed due to an IO error.
     */
    private void downloadFile(File file, MediaInfo fileData, double percent, long totalSize) throws IOException {
        if (fragment.getActivity() == null) {
            Log.e(TAG, "downloadFile: null activity");
            dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_oups);
            return;
        }
        if (!Util.hasWifiAndLan(fragment.getActivity())) {
            Log.e(TAG, "There is no wifi");
            throw new IOException("Cannot download the videos without WIFI, please connect to wifi and retry");
        }
        file.createNewFile();
        URL fileUrl = new URL(fragment.getString(R.string.file_location_url) + fileData.getUrl());
        FileOutputStream fileStream = null;
        InputStream urlStream = null;
        try {
            fileStream = new FileOutputStream(file);
            urlStream = fileUrl.openStream();
            byte[] buffer = new byte[2048];
            int read;
            int passedSize = 0; // one second download size for download speedometer
            long fromTime = System.nanoTime(), passedTime;
            while ((read = urlStream.read(buffer, 0, 2048)) != -1) {
                if (isCancelled()) {
                    return;
                }
                passedTime = System.nanoTime();
                fileStream.write(buffer, 0, read);
                passedSize += read;
                /* one second counter:
                   update interface for download percent and speed */
                if (passedTime - fromTime >= 1_000_000_000) {
                    percent += (double)passedSize / (double)totalSize * 100d;
                    publishProgress(new Pair<Double, Long>(percent, (long)passedSize));
                    fromTime = System.nanoTime();
                    passedSize = 0;
                }
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
        if (fragment.getActivity() == null) {
            Log.e(TAG, "downloadFiles: activity null");
            dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_oups);
            return false;
        }
        if (!Util.hasWifiAndLan(fragment.getActivity())) {
            Log.e(TAG, "downloadFiles: no wifi");
            dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_no_wifi);
            return false;
        }
        try {
            mFilesInfo = JSonParser.getMediaInfos(fragment.getActivity());
            long totalSize = 0;
            for (MediaInfo fileData : mFilesInfo) {
                totalSize += fileData.getSize();
            }
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
                if (isCancelled()) {
                    return false;
                }
                Log.i(TAG, "downloadFiles: downloading: " + fileData.getName());
                File localFile = new File(mediaFolder.getPath() + '/' + fileData.getName());
                if (localFile.exists()) {
                    if (localFile.isFile() && FileHandler.checkFileSum(localFile, fileData.getChecksum())) {
                        fileData.setLocalUrl(localFile.getAbsolutePath());
                        unusedFiles.remove(localFile);
                        percent += (double)fileData.getSize() / (double)totalSize * 100d;
                        publishProgress(new Pair<Double, Long>(percent, 0L));
                        continue;
                    } else {
                        FileHandler.delete(localFile);
                    }
                }
                downloadFile(localFile, fileData, percent, totalSize);
                percent += (double)fileData.getSize() / (double)totalSize * 100d;
                fileData.setLocalUrl(localFile.getAbsolutePath());
            }
            for (File toRemove : unusedFiles) {
                FileHandler.delete(toRemove);
            }
        } catch (ConnectException e) {
            Log.e(TAG, "downloadFiles: " + e.toString());
            dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_error_connect);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "downloadFiles: " + e.toString());
            dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_error_io);
            return false;
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "downloadFiles: " + e.toString());
            dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_download_error);
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Pair... values) {
        super.onProgressUpdate(values);
        if (fragment instanceof MainPageFragment && values.length >= 1) {
            MainPageFragment mainPageFragment = (MainPageFragment) fragment;
            Pair<Double, Long> progressValues = values[0];
            String progressString = String.format(
                    fragment.getString(R.string.dialog_text_download_progress),
                    FormatStr.format2Dec(progressValues.first), FormatStr.bitRateToString(progressValues.second));
            mainPageFragment.updateProgress(progressValues.first, progressString, "");
        }
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        return downloadFiles();
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if (fragment instanceof MainPageFragment) {
            ((MainPageFragment) fragment).dismissDialog();
            if (success) {
                ((MainPageFragment) fragment).onFilesDownloaded(mFilesInfo);
            }
        }
        if (dialog != null) {
            dialog.display(fragment.getActivity());
        }
    }
}
