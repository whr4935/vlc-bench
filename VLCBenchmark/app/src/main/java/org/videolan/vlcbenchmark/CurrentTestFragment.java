/*****************************************************************************
 * CurrentTestFragment.java
 *****************************************************************************
 * Copyright © 2016-2017 VLC authors and VideoLAN
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.videolan.vlcbenchmark.service.BenchServiceDispatcher;
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

    public static final int MODE_DOWNLOAD = 1;
    public static final int MODE_BENCHMARK = 2;

    public static final String ARG_MODE = "MODE";

    public CurrentTestFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        int mode;
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_current_test, container, false);
        Button cancel = (Button) view.findViewById(R.id.current_test_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.cancelBench();
                dismiss();
            }
        });
        if (getArguments() != null) {
            mode = getArguments().getInt(ARG_MODE, MODE_BENCHMARK);
        } else {
            mode = MODE_BENCHMARK;
        }
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        currentSample = (TextView) view.findViewById(R.id.current_sample);
        percentText = (TextView) view.findViewById(R.id.percentText);
        TextView title = (TextView) view.findViewById(R.id.test_dialog_title);
        if (mode == MODE_DOWNLOAD) {
            title.setText(R.string.dialog_title_downloading);
        } else {
            title.setText(R.string.dialog_title_testing);
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

    public void setUiToDefault() {
        progressBar.setProgress(0);
        progressBar.setMax(100);
        percentText.setText(R.string.default_percent_value);
        currentSample.setText("");
    }

    public void updatePercent(double percent, long bitRate) {
        String strPercent;
        progressBar.setProgress((int) Math.round(percent));
        if (bitRate < 0) {
            strPercent = FormatStr.format2Dec(percent) + "%";
            percentText.setText(strPercent);
        }
        else {
            strPercent = FormatStr.format2Dec(percent) + "% (" + FormatStr.bitRateToString(bitRate) + ")";
            percentText.setText(strPercent);
        }
    }

    public void updateTestProgress(String sampleName, int fileIndex, int numberOfFiles, int testNumber, int loopNumber, int numberOfLoops) {
        progressBar.setProgress((numberOfFiles * 4) * (loopNumber - 1) + ((fileIndex - 1) * 4) + testNumber);
        progressBar.setMax(numberOfFiles * numberOfLoops * 4);
        currentSample.setText(sampleName);
        if (numberOfLoops != 1) {
            percentText.setText(String.format(
                    getResources().getString(R.string.progress_text_format_loop),
                    FormatStr.format2Dec(progressBar.getProgress() * 100.0 / progressBar.getMax()), fileIndex,
                    numberOfFiles, testNumber, loopNumber, numberOfLoops));
        }
        else {
            percentText.setText(
                    String.format(getResources().getString(R.string.progress_text_format),
                            FormatStr.format2Dec(progressBar.getProgress() * 100.0 / progressBar.getMax()), fileIndex,
                            numberOfFiles, testNumber));
        }
    }

    public interface TestView {
        void setDialogFragment(CurrentTestFragment fragment);
        void cancelBench();
    }
}
