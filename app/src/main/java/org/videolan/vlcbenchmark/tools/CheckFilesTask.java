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

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import androidx.fragment.app.Fragment;

import org.videolan.vlcbenchmark.MainPageFragment;
import org.videolan.vlcbenchmark.R;
import org.videolan.vlcbenchmark.SystemPropertiesProxy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CheckFilesTask extends AsyncTask<Void, Pair, Boolean> {

    private final String TAG = CheckFilesTask.class.getName();
    private DialogInstance errDialog;
    private List<MediaInfo> mFilesInfo;
    private long downloadSize = -1;

    //warning fixed in MainPage.onDestroy() -> should not leak;
    private Fragment fragment;

    public CheckFilesTask(Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        int counter = 0;
        if (fragment.getActivity() == null) {
            Log.e(TAG, "doInBackground: null activity");
            errDialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_oups);
            return false;
        }
        if (!Util.hasWifiAndLan(fragment.getActivity())) {
            Log.e(TAG, "There is no wifi.");
            errDialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_no_internet);
            return false;
        }
        ArrayList<MediaInfo> filesToDownload;
        try {
            mFilesInfo = JSonParser.getMediaInfos(fragment.getActivity());
            if (mFilesInfo == null) {
                errDialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_error_connect);
                return false;
            }
            String dirStr = StorageManager.INSTANCE.getInternalDirStr(StorageManager.mediaFolder);
            if (dirStr == null) {
                Log.e(TAG, "doInBackground: Failed to get media folder");
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
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            errDialog = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_error_conf);
            return false;
        }
        Log.i(TAG, "doInBackground: End of file check");
        if (filesToDownload.size() != 0) {
            Log.i(TAG, "doInBackground: Missing " + filesToDownload.size() + " files");
        }
        return true;
    }

    private boolean checkDeviceFreeSpace(long size) {
        long freeSpace = SystemPropertiesProxy.getFreeSpace();
        if (size > freeSpace) {
            Log.e("MainPageDownload", "checkDeviceFreeSpace: missing space to download all media files");
            long spaceNeeded = size - freeSpace;
            String space = FormatStr.INSTANCE.sizeToString(spaceNeeded);
            String msg = String.format(fragment.getString(R.string.dialog_text_missing_space), space);
            new AlertDialog.Builder(fragment.getContext())
                    .setTitle(fragment.getString(R.string.dialog_title_warning))
                    .setMessage(msg)
                    .setNegativeButton(fragment.getString(R.string.dialog_btn_ok), null)
                    .show();
            return false;
        }
        return true;
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

        if (fragment instanceof MainPageFragment) {
            if (checkState) {
                if (checkDeviceFreeSpace(downloadSize)) {
                    ((MainPageFragment) fragment).onFilesChecked(downloadSize);
                }
            }
        }
        if (errDialog != null) {
            errDialog.display(fragment.getActivity());
        }
    }

}
