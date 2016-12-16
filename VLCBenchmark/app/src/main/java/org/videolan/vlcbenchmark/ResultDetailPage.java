package org.videolan.vlcbenchmark;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.videolan.vlcbenchmark.tools.JsonHandler;
import org.videolan.vlcbenchmark.tools.TestInfo;
import org.w3c.dom.Text;

import java.util.ArrayList;

public class ResultDetailPage extends AppCompatActivity {

    TestInfo result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_detail_page);

        if (!getIntent().hasExtra("result")) {
            Log.e("VLCBench", "no extra result");
        }
        result = (TestInfo) getIntent().getSerializableExtra("result");

        setupUi();
    }

    private void setupUi() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //TODO handle the null pointer
        getSupportActionBar().setTitle("Detail");
        setupText();
    }

    private void setupText() {
        TextView mName;
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

        mName = (TextView) findViewById(R.id.test_name);
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

        mName.setText(result.getName());
        mSoftwareScore.setText("Score: " + result.getSoftware());
        mSoftwareBadScreenshot.setText("Percent of bad screenshots: " + result.getBadScreenshots(0));
        mSoftwareFramesDropped.setText("Frames dropped: " + result.getFrameDropped(0));
        mSoftwareWarningNumber.setText("Number of warnings: " + result.getNumberOfWarnings(0));
        mHardwareScore.setText("Score: " + result.getHardware());
        mHardwareBadScreenshot.setText("Percent of bad screenshots: " + result.getBadScreenshots(1));
        mHardwareFramesDropped.setText("Frames dropped: " + result.getFrameDropped(1));
        mHardwareWarningNumber.setText("Number of warnings: " + result.getNumberOfWarnings(1));

//        mSoftwareCrash.setText("Crash: " + result.getCrashes(0));
//        mHardwareCrash.setText("Crash: " + result.getCrashes(1));

        setCrashText(mSoftwareCrash, 0);
        setCrashText(mHardwareCrash, 1);
    }

    private void setCrashText(TextView textView, int decoding) {
        StringBuilder text = new StringBuilder();
        if (result.hasCrashed(decoding)) {
            text.append("Crash:\n  - ");
            text.append(result.getCrashes(decoding, 0));
            if (result.getCrashes(decoding, 0) != "") {
                text.append("\n  - ");
            }
            text.append(result.getCrashes(decoding, 1));
            textView.setText(text.toString());
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
