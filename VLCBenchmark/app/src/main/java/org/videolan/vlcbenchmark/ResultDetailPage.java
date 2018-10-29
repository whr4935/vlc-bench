/*
 *****************************************************************************
 * ResultDetailPage.java
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

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setTitle(result.getName());
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setupText();
    }

    private void setupText() {
        ((TextView) findViewById(R.id.software_score)).setText(String.format(getResources().getString(R.string.detail_score), format2Dec(result.getSoftware())));
        ((TextView) findViewById(R.id.software_bad_screenshot)).setText(String.format(getResources().getString(R.string.detail_bad_screenshots), format2Dec(result.getBadScreenshots(0))));
        ((TextView) findViewById(R.id.software_frames_dropped)).setText(String.format(getResources().getString(R.string.detail_frames_dropped), result.getFrameDropped(0)));
        ((TextView) findViewById(R.id.software_warning_number)).setText(String.format(getResources().getString(R.string.detail_warning_number), result.getNumberOfWarnings(0)));
        setCrashText(((TextView) findViewById(R.id.software_crash)), TestInfo.SOFT);

        ((TextView) findViewById(R.id.hardware_score)).setText(String.format(getResources().getString(R.string.detail_score), format2Dec(result.getHardware())));
        ((TextView) findViewById(R.id.hardware_bad_screenshot)).setText(String.format(getResources().getString(R.string.detail_bad_screenshots), format2Dec(result.getBadScreenshots(1))));
        ((TextView) findViewById(R.id.hardware_frames_dropped)).setText(String.format(getResources().getString(R.string.detail_frames_dropped), result.getFrameDropped(1)));
        ((TextView) findViewById(R.id.hardware_warning_number)).setText(String.format(getResources().getString(R.string.detail_warning_number), result.getNumberOfWarnings(1)));
        setCrashText(((TextView) findViewById(R.id.hardware_crash)), TestInfo.HARD);
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
