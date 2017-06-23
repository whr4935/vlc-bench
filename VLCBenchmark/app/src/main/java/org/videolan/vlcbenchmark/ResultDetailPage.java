/*****************************************************************************
 * ResultDetailPage.java
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

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.videolan.vlcbenchmark.tools.TestInfo;

import static org.videolan.vlcbenchmark.tools.FormatStr.format2Dec;

public class ResultDetailPage extends AppCompatActivity {

    private final static String TAG = ResultDetailPage.class.getName();
    private TestInfo result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_detail_page);

        if (!getIntent().hasExtra("result")) {
            Log.e(TAG, "no extra result");
            onBackPressed();
            return;
        }
        result = (TestInfo) getIntent().getSerializableExtra("result");

        setupUi();
    }

    private void setupUi() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(result.getName());
        }
        setupText();
    }

    private void setupText() {
        TextView mSoftwareScore;
        TextView mSoftwareFramesDropped;
        TextView mSoftwareBadScreenshot;
        TextView mSoftwareWarningNumber;
        TextView mSoftwareCrash;
        TextView mHardwareScore;
        TextView mHardwareFramesDropped;
        TextView mHardwareBadScreenshot;
        TextView mHardwareWarningNumber;
        TextView mHardwareCrash;

        mSoftwareScore = (TextView) findViewById(R.id.software_score);
        mSoftwareBadScreenshot = (TextView) findViewById(R.id.software_bad_screenshot);
        mSoftwareFramesDropped = (TextView) findViewById(R.id.software_frames_dropped);
        mSoftwareWarningNumber = (TextView) findViewById(R.id.software_warning_number);
        mSoftwareCrash = (TextView) findViewById(R.id.software_crash);
        mHardwareScore = (TextView) findViewById(R.id.hardware_score);
        mHardwareBadScreenshot = (TextView) findViewById(R.id.hardware_bad_screenshot);
        mHardwareFramesDropped = (TextView) findViewById(R.id.hardware_frames_dropped);
        mHardwareWarningNumber = (TextView) findViewById(R.id.hardware_warning_number);
        mHardwareCrash = (TextView) findViewById(R.id.hardware_crash);

        mSoftwareScore.setText(String.format(getResources().getString(R.string.detail_score), format2Dec(result.getSoftware())));
        mSoftwareBadScreenshot.setText(String.format(getResources().getString(R.string.detail_bad_screenshots), format2Dec(result.getBadScreenshots(0))));
        mSoftwareFramesDropped.setText(String.format(getResources().getString(R.string.detail_frames_dropped), result.getFrameDropped(0)));
        mSoftwareWarningNumber.setText(String.format(getResources().getString(R.string.detail_warning_number), result.getNumberOfWarnings(0)));
        mHardwareScore.setText(String.format(getResources().getString(R.string.detail_score), format2Dec(result.getHardware())));
        mHardwareBadScreenshot.setText(String.format(getResources().getString(R.string.detail_bad_screenshots), format2Dec(result.getBadScreenshots(1))));
        mHardwareFramesDropped.setText(String.format(getResources().getString(R.string.detail_frames_dropped), result.getFrameDropped(1)));
        mHardwareWarningNumber.setText(String.format(getResources().getString(R.string.detail_warning_number), result.getNumberOfWarnings(1)));

        setCrashText(mHardwareCrash, TestInfo.QUALITY);
        setCrashText(mSoftwareCrash, TestInfo.PLAYBACK);
    }

    private void setCrashText(TextView textView, int decoding) {
        StringBuilder text = new StringBuilder();
        if (result.hasCrashed(decoding)) {
            text.append("Crash:");
            if (!result.getCrashes(decoding, TestInfo.SOFT).equals("")) {
                text.append("\n  - ");
                text.append(result.getCrashes(decoding, TestInfo.SOFT));
            }
            if (!result.getCrashes(decoding, TestInfo.HARD).equals("")) {
                text.append("\n  - ");
                text.append(result.getCrashes(decoding, TestInfo.HARD));
            }
            textView.setText(text.toString());
        } else {
            textView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
