package org.videolan.vlcbenchmark;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.videolan.vlcbenchmark.service.BenchServiceDispatcher;
import org.videolan.vlcbenchmark.service.FAILURE_STATES;
import org.videolan.vlcbenchmark.tools.TestInfo;

import java.util.List;

/**
 * Created by noeldu_b on 7/11/16.
 */
public class MainPage extends VLCWorkerModel {

    private TextView percentText = null;
    private TextView textLog = null;
    private Button oneTest = null,
            threeTests = null;
    private ProgressBar progressBar = null;

    private static final String PROGRESS_TEXT_FORMAT = "%.2f %% | file %d/%d | test %d";
    private static final String PROGRESS_TEXT_FORMAT_LOOPS = PROGRESS_TEXT_FORMAT + " | loop %d/%d";

    public void launchTests(View v) {
        switch (v.getId()) {
            case R.id.benchOne:
                launchTests(1);
                break;
            case R.id.benchThree:
                launchTests(3);
                break;
        }
    }

    @Override
    protected void setupUiMembers() {
        setContentView(R.layout.activity_main_page);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        percentText = (TextView) findViewById(R.id.percentText);
        oneTest = (Button) findViewById(R.id.benchOne);
        threeTests = (Button) findViewById(R.id.benchThree);
        textLog = (TextView) findViewById(R.id.extractEditText);
    }

    @Override
    protected void resetUiToDefault() {
        progressBar.setProgress(0);
        progressBar.setMax(100);
        percentText.setText(R.string.default_percent_value);
        textLog.setText("");
    }

    @Override
    protected void updateUiOnServiceDone() {
        oneTest.setVisibility(View.INVISIBLE);
        threeTests.setVisibility(View.INVISIBLE);
    }

    @Override
    public void stepFinished(String message) {
        textLog.append(message + '\n');
    }

    @Override
    public void updatePercent(double percent, long bitRate) {
        progressBar.setProgress((int) Math.round(percent));
        if (bitRate == BenchServiceDispatcher.NO_BITRATE)
            percentText.setText(String.format("%.2f %%", percent));
        else
            percentText.setText(String.format("%.2f %% (%s)", percent, bitRateToString(bitRate)));
    }

    private String bitRateToString(long bitRate) {
        if (bitRate <= 0)
            return "0 bps";

        double powOf10 = Math.round(Math.log10(bitRate));

        if (powOf10 < 3)
            return String.format("%l bps", bitRate);
        else if (powOf10 >= 3 && powOf10 < 6)
            return String.format("%.2f kbps", bitRate / 1_000d);
        else if (powOf10 >= 6 && powOf10 < 9)
            return String.format("%.2f mbps", bitRate / 1_000_000d);
        return String.format("%.2f gbps", bitRate / 1_000_000_000d);
    }

    @Override
    public void failure(FAILURE_STATES reason, Exception exception) {
        new AlertDialog.Builder(this).setTitle("Error during download").setMessage("An exception occurred while downloading:\n" + exception.toString()).setNeutralButton("ok", null).show();
    }

    @Override
    protected void initVlcProgress(int totalNumberOfElements) {
        progressBar.setProgress(0);
        progressBar.setMax(totalNumberOfElements);
        textLog.append("\n");
    }

    @Override
    protected void onFileTestStarted(String fileName) {
        textLog.append(fileName + '\n');
    }

    @Override
    protected void onSingleTestFinished(String testName, boolean succeeded, int fileIndex, int numberOfFiles, int testNumber, int loopNumber, int numberOfLoops) {
        progressBar.incrementProgressBy(1);

        if (numberOfLoops != 1)
            percentText.setText(String.format(PROGRESS_TEXT_FORMAT_LOOPS, progressBar.getProgress() * 100.0 / progressBar.getMax(), fileIndex, numberOfFiles, testNumber,
                    loopNumber, numberOfLoops));
        else
            percentText.setText(String.format(PROGRESS_TEXT_FORMAT, progressBar.getProgress() * 100.0 / progressBar.getMax(), fileIndex, numberOfFiles, testNumber));
        textLog.append(String.format("        %s tests %s\n", testName, (succeeded ? "finished" : "failed")));
    }

    @Override
    protected void onVlcCrashed(String errorMessage, final Runnable continueTesting) {
        new AlertDialog.Builder(this) {{
            setTitle("VLC crashed on test");
            setCancelable(false);
        }}
                .setMessage(errorMessage)
                .setNeutralButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                continueTesting.run();
                            }
                        })
                .show();
    }

    @Override
    protected void onTestsFinished(List<TestInfo>[] results, double softScore, double hardScore) {
        Intent intent = new Intent(MainPage.this, ResultPage.class);
        intent.putExtra("resultsTest", results);
        intent.putExtra("soft", softScore);
        intent.putExtra("hard", hardScore);
        oneTest.setVisibility(View.VISIBLE);
        threeTests.setVisibility(View.VISIBLE);
        startActivity(intent);
    }

    @Override
    protected void onSaveUiData(Bundle saveInstanceState) {
        saveInstanceState.putInt("PROGRESS_VALUE", progressBar.getProgress());
        saveInstanceState.putInt("PROGRESS_MAX", progressBar.getMax());
    }

    @Override
    protected void onRestoreUiData(Bundle saveInstanceState) {
        super.onRestoreInstanceState(saveInstanceState);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setProgress(saveInstanceState.getInt("PROGRESS_VALUE", 0));
        progressBar.setMax(saveInstanceState.getInt("PROGRESS_MAX", 100));
        percentText = (TextView) findViewById(R.id.percentText);
        oneTest = (Button) findViewById(R.id.benchOne);
        threeTests = (Button) findViewById(R.id.benchThree);
        textLog = (TextView) findViewById(R.id.extractEditText);
    }

    @Override
    public void onBackPressed() {
    }
}
