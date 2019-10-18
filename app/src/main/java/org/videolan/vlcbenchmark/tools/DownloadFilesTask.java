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
import android.util.Log;
import android.util.Pair;

import androidx.fragment.app.Fragment;

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
    private long mTotalFileSize = 0;

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
     * @param downloadedSize the total size that was already downloaded
     * @throws IOException if the device is not connected to WIFI or LAN or if the download failed due to an IO error.
     */
    private void downloadFile(File file, MediaInfo fileData, long downloadedSize) throws IOException {
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
            long fromTime = System.nanoTime(), passedTime;
            long passedSize = 0;
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
                    downloadedSize += passedSize;
                    publishProgress(new Pair<>(downloadedSize, passedSize));
                    fromTime = System.nanoTime();
                    passedSize = 0;
                }
            }
            if (!StorageManager.INSTANCE.checkFileSum(file, fileData.getChecksum())) {
                StorageManager.INSTANCE.delete(file);
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
            // Add nomedia file to stop vlc medialibrary from indexing the benchmark files
            StorageManager.INSTANCE.setNoMediaFile();
            mFilesInfo = JSonParser.getMediaInfos(fragment.getActivity());
            for (MediaInfo fileData : mFilesInfo) {
                mTotalFileSize += fileData.getSize();
            }
            String mediaFolderStr = StorageManager.INSTANCE.getFolderStr(StorageManager.INSTANCE.mediaFolder);
            if (mediaFolderStr == null) {
                Log.e(TAG, "Failed to get media directory");
                dialog = new DialogInstance(R.string.dialog_title_oups, R.string.dialog_text_download_error);
                return false;
            }
            File mediaFolder = new File(mediaFolderStr);
            HashSet<File> unusedFiles = new HashSet<>(Arrays.asList(mediaFolder.listFiles()));
            long downloadedSize = 0;
            for (MediaInfo fileData : mFilesInfo) {
                if (isCancelled()) {
                    return false;
                }
                Log.i(TAG, "downloadFiles: downloading: " + fileData.getName());
                File localFile = new File(mediaFolder.getPath() + '/' + fileData.getName());
                if (localFile.exists()) {
                    if (localFile.isFile() && StorageManager.INSTANCE.checkFileSum(localFile, fileData.getChecksum())) {
                        fileData.setLocalUrl(localFile.getAbsolutePath());
                        unusedFiles.remove(localFile);
                        downloadedSize += fileData.getSize();
                        publishProgress(new Pair<>(downloadedSize, 0L));
                        continue;
                    } else if (!localFile.getPath().contains(".nomedia")) {
                        // Check not to remove the nomedia file
                        StorageManager.INSTANCE.delete(localFile);
                    }
                }
                downloadFile(localFile, fileData, downloadedSize);
                downloadedSize += fileData.getSize();
                fileData.setLocalUrl(localFile.getAbsolutePath());
            }
            for (File toRemove : unusedFiles) {
                // Check not to remove the nomedia file
                if (!toRemove.getPath().contains(".nomedia")) {
                    StorageManager.INSTANCE.delete(toRemove);
                }
            }
        } catch (ConnectException e) {
            Log.e(TAG, e.getMessage(), e);
            dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_error_connect);
            return false;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_error_io);
            return false;
        } catch (GeneralSecurityException e) {
            Log.e(TAG, e.getMessage(), e);
            dialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_download_error);
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Pair... values) {
        super.onProgressUpdate(values);
        if (fragment instanceof MainPageFragment && values.length >= 1) {
            MainPageFragment mainPageFragment = (MainPageFragment) fragment;
            Pair<Long, Long> progressValues = values[0];
            long downloadedSize = progressValues.first;
            long downloadedSpeed = progressValues.second;
            double percent = (double)downloadedSize / (double)mTotalFileSize * 100d;
            String state = downloadedSpeed == 0 ?
                    fragment.getString(R.string.dialog_text_download_checking_file) :
                    fragment.getString(R.string.dialog_text_download_downloading);
            String progressString = String.format(
                    fragment.getString(R.string.dialog_text_download_progress),
                    FormatStr.format2Dec(percent), FormatStr.bitRateToString(downloadedSpeed),
                    FormatStr.sizeToString(downloadedSize), FormatStr.sizeToString(mTotalFileSize));
            mainPageFragment.updateProgress(percent, progressString, state);
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
