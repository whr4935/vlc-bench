/*
 *****************************************************************************
 * CheckFilesTask.java
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

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import org.videolan.vlcbenchmark.MainPage;
import org.videolan.vlcbenchmark.R;
import org.videolan.vlcbenchmark.service.JSonParser;
import org.videolan.vlcbenchmark.service.MediaInfo;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class CheckFilesTask extends AsyncTask<Void, Pair, Boolean> {

    private final String TAG = CheckFilesTask.class.getName();
    private DialogInstance errDialog;
    private List<MediaInfo> mFilesInfo;
    private long downloadSize = -1;

    //warning fixed in MainPage.onDestroy() -> should not leak;
    private Activity activity;

    public CheckFilesTask(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        int counter = 0;
        if (!Util.hasWifiAndLan(activity)) {
            Log.e(TAG, "There is no wifi.");
            errDialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_no_internet);
            return false;
        }
        ArrayList<MediaInfo> filesToDownload;
        try {
            mFilesInfo = JSonParser.getMediaInfos(activity);
            String dirStr = FileHandler.getFolderStr(FileHandler.mediaFolder);
            if (dirStr == null) {
                errDialog = new DialogInstance(R.string.dialog_title_oups, R.string.dialog_text_file_creation_failure);
                return false;
            }
            File dir = new File(dirStr);
            File[] files = dir.listFiles();
            filesToDownload = new ArrayList<>();
            for (MediaInfo mediaFile : mFilesInfo) {
                publishProgress(new Pair<>(counter, mFilesInfo.size()));
                counter += 1;
                if (isCancelled()) {
                    downloadSize = -1;
                    Log.w(TAG, "doInBackground: file check was cancelled");
                    return false;
                }
                Log.i(TAG, "doInBackground: checking " + mediaFile.getName());
                boolean presence = false;
                if (files != null) {
                    for (File localFile : files) {
                        if (localFile.getName().equals(mediaFile.getName())) {
                            if (!FileHandler.checkFileSum(localFile, mediaFile.getChecksum())) {
                                Log.i(TAG, "doInBackground: " + mediaFile.getName() + " file is corrupted");
                                mediaFile.setLocalUrl(localFile.getAbsolutePath());
                                FileHandler.delete(localFile);
                                filesToDownload.add(mediaFile);
                                downloadSize += mediaFile.getSize();
                            }
                            mediaFile.setLocalUrl(localFile.getAbsolutePath());
                            presence = true;
                            break;
                        }
                    }
                }
                if (!presence) {
                    Log.i(TAG, "doInBackground: " + mediaFile.getName() + " file is missing");
                    filesToDownload.add(mediaFile);
                    downloadSize += mediaFile.getSize();
                }
            }
        } catch (IOException | GeneralSecurityException e) {
            Log.e(TAG, "doInBackground: Failed to check files: " + e.toString());
            errDialog = new DialogInstance(R.string.dialog_title_oups, R.string.dialog_text_sample);
            return false;
        }
        Log.i(TAG, "doInBackground: End of file check");
        if (filesToDownload.size() != 0) {
            Log.i(TAG, "doInBackground: Missing " + filesToDownload.size() + " files");
            return false;
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Pair... values) {
        super.onProgressUpdate(values);
        if (activity instanceof MainPage && values.length >= 1) {
            MainPage mainPage = (MainPage) activity;
            Pair<Integer, Integer> progressValues = values[0];
            mainPage.updateFileCheckProgress(progressValues.first, progressValues.second);
        }

    }

    /**
     * Sets the main page fragment according to the success of the file check
     * If successful, also sets the files for the benchmarks
     * Furthermore it displays any dialog that might have been spawned during the process
     * @param checkState file check success boolean
     */
    @Override
    protected void onPostExecute(Boolean checkState) {
        super.onPostExecute(checkState);
        if (activity instanceof MainPage) {
            MainPage mainPageContext = (MainPage) activity;
            mainPageContext.setFilesChecked(checkState);
            if (checkState) {
                mainPageContext.setBenchmarkFiles(mFilesInfo);
                mainPageContext.setFilesDownloaded(true);
            } else {
                mainPageContext.setDownloadSize(downloadSize);
            }
        }
        if (errDialog != null) {
            errDialog.display(activity);
        }
    }

}
