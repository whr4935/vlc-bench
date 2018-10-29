/*
 *****************************************************************************
 * MainPageDownloadFragment.java
 *****************************************************************************
 * Copyright © 2016-2018 VLC authors and VideoLAN
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

package org.videolan.vlcbenchmark;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.videolan.vlcbenchmark.tools.DownloadFilesTask;
import org.videolan.vlcbenchmark.tools.FileHandler;
import org.videolan.vlcbenchmark.tools.FormatStr;

import java.io.File;


/**
 * A simple {@link Fragment} subclass.
 */

public class MainPageDownloadFragment extends Fragment {

    IMainPageDownloadFragment mListener;
    DownloadFilesTask downloadFilesTask;
    long downloadSize = -1;

    public MainPageDownloadFragment() {}

    private void startDownload() {
        downloadFilesTask = new DownloadFilesTask(getActivity());
        downloadFilesTask.execute();
        if (mListener != null) {
            mListener.setScreenOn();
        }
        CurrentTestFragment fragment = new CurrentTestFragment(); // tmp
        fragment.setCancelable(false);
        Bundle args = new Bundle();
        args.putInt(CurrentTestFragment.ARG_MODE, CurrentTestFragment.MODE_DOWNLOAD);
        fragment.setArguments(args);
        fragment.show(getFragmentManager(), "Download dialog");
    }

    private boolean checkDeviceFreeSpace(long size) {
        String mediaDir = FileHandler.getFolderStr(FileHandler.mediaFolder);
        if (mediaDir == null)
            return false;
        File file = new File(mediaDir);
        long freeSpace = file.getFreeSpace();
        if (size > freeSpace) {
            Log.e("MainPageDownload", "checkDeviceFreeSpace: missing space to download all media files");
            long spaceNeeded = size - freeSpace;
            String unit;
            double space;
            if (spaceNeeded % 1_000_000_000 > 0) {
                unit = "Go";
                space = spaceNeeded / 1_000_000_000d;
            } else {
                unit = "Mo";
                space = spaceNeeded / 1_000_000d;
            }
            String msg = String.format(getString(R.string.dialog_text_missing_space), FormatStr.format2Dec(space), unit);
            new AlertDialog.Builder(getContext())
                    .setTitle(getResources().getString(R.string.dialog_title_warning))
                    .setMessage(msg)
                    .setNegativeButton(getResources().getString(R.string.dialog_btn_ok), null)
                    .show();
            return false;
        }
        return true;
    }

    public void setDownloadSize(long downloadSize) {
        this.downloadSize = downloadSize;
    }

    public void cancelDownload() {
        if (downloadFilesTask != null) {
            downloadFilesTask.cancel(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_page_download, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FloatingActionButton dlButton = (FloatingActionButton) view.findViewById(R.id.fab_download);
        dlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(getContext())
                        .setTitle(getResources().getString(R.string.dialog_title_warning))
                        .setMessage(getResources().getString(R.string.download_warning))
                        .setNeutralButton(getResources().getString(R.string.dialog_btn_cancel), null)
                        .setNegativeButton(getResources().getString(R.string.dialog_btn_continue), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (checkDeviceFreeSpace(downloadSize)) {
                                    startDownload();
                                }
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (downloadFilesTask != null) {
            downloadFilesTask.cancel(true);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IMainPageDownloadFragment) {
            mListener = (IMainPageDownloadFragment) context;
        } else {
            throw new RuntimeException(context.toString());
        }
    }

    public interface IMainPageDownloadFragment {
        boolean getHasChecked();
        void setScreenOn();
    }

}
