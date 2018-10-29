/*
 *****************************************************************************
 * CurrentTestFragment.java
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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.videolan.vlcbenchmark.tools.FormatStr;

/**
 * A simple {@link Fragment} subclass.
 */
public class CurrentTestFragment extends DialogFragment {

    private final static String TAG = CurrentTestFragment.class.getName();

    TestView mListener;

    private TextView percentText = null;
    private TextView currentSample = null;
    private ProgressBar progressBar = null;

    private int mMode = 0;
    public static final int MODE_DOWNLOAD = 1;
    public static final int MODE_BENCHMARK = 2;
    public static final int MODE_FILECHECK = 3;

    public static final String ARG_MODE = "MODE";

    public CurrentTestFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_current_test, container, false);
        Button cancel = (Button) view.findViewById(R.id.current_test_cancel);

        if (getArguments() != null) {
            mMode = getArguments().getInt(ARG_MODE, MODE_BENCHMARK);
        } else {
            mMode = MODE_BENCHMARK;
        }
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        currentSample = (TextView) view.findViewById(R.id.current_sample);
        percentText = (TextView) view.findViewById(R.id.percentText);
        TextView title = (TextView) view.findViewById(R.id.test_dialog_title);
        if (mMode == MODE_DOWNLOAD) {
            title.setText(R.string.dialog_title_downloading);
        } else if (mMode == MODE_BENCHMARK){
            title.setText(R.string.dialog_title_testing);
        } else {
            title.setText(getString(R.string.dialog_title_checking_files));
        }
        if (mMode == MODE_BENCHMARK) {
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.cancelBench();
                    dismiss();
                }
            });
        } else if (mMode == MODE_DOWNLOAD){
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.cancelDownload();
                    dismiss();
                }
            });
        } else {
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.cancelFileCheck();
                    dismiss();
                }
            });
        }
        setUiToDefault();
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof TestView) {
            mListener = (TestView) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onStart() {
        mListener.setDialogFragment(this);
        super.onStart();
    }

    @Override
    public void onDetach() {
        mListener.setDialogFragment(null);
        super.onDetach();
    }

    public int getMode() {
        return mMode;
    }

    public void setUiToDefault() {
        progressBar.setProgress(0);
        progressBar.setMax(100);
        percentText.setText(R.string.default_percent_value);
        currentSample.setText("");
    }

    public void updateFileCheckProgress(int file, int total) {
        double percent = (double)file / (double)total * 100d;
        String strPercent;

        progressBar.setProgress((int) Math.round(percent));
        strPercent = String.format(getString(R.string.dialog_text_file_check_progress), file, total);
        percentText.setText(strPercent);
    }

    public void updatePercent(double percent, long bitRate) {
        String strPercent;
        progressBar.setProgress((int) Math.round(percent));
        if (bitRate <= 0) {
            strPercent = FormatStr.format2Dec(percent) + "%";
            percentText.setText(strPercent);
        }
        else {
            strPercent = FormatStr.format2Dec(percent) + "% (" + FormatStr.bitRateToString(bitRate) + ")";
            percentText.setText(strPercent);
        }
    }

    public void updateTestProgress(String sampleName, int fileIndex, int numberOfFiles, int testNumber, int loopNumber, int numberOfLoops) {
        int max = numberOfFiles * 4 * numberOfLoops;
        double progress = ((fileIndex - 1)* 4 * loopNumber + testNumber) / (double)max* 100d;
        progressBar.setProgress((int) Math.round(progress));
        progressBar.setMax(100);
        currentSample.setText(sampleName);
        if (numberOfLoops != 1) {
            percentText.setText(String.format(
                    getResources().getString(R.string.progress_text_format_loop),
                    FormatStr.format2Dec(progress), fileIndex,
                    numberOfFiles, testNumber, loopNumber, numberOfLoops));
        }
        else {
            percentText.setText(
                    String.format(getResources().getString(R.string.progress_text_format),
                            FormatStr.format2Dec(progress), fileIndex,
                            numberOfFiles, testNumber));
        }
    }

    public interface TestView {
        void setDialogFragment(CurrentTestFragment fragment);
        void cancelBench();
        void cancelDownload();
        void cancelFileCheck();
    }
}
