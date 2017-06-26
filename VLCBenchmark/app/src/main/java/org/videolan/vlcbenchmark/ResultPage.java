/*****************************************************************************
 * ResultPage.java
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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.*;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.videolan.vlcbenchmark.service.BenchService;
import org.videolan.vlcbenchmark.service.BenchServiceDispatcher;
import org.videolan.vlcbenchmark.service.BenchServiceListener;
import org.videolan.vlcbenchmark.service.FAILURE_STATES;
import org.videolan.vlcbenchmark.service.MediaInfo;
import org.videolan.vlcbenchmark.service.ServiceActions;
import org.videolan.vlcbenchmark.tools.DialogInstance;
import org.videolan.vlcbenchmark.tools.GoogleConnectionHandler;
import org.videolan.vlcbenchmark.tools.JsonHandler;
import org.videolan.vlcbenchmark.tools.TestInfo;

import java.util.ArrayList;
import java.util.List;

import static org.videolan.vlcbenchmark.tools.FormatStr.format2Dec;

public class ResultPage extends AppCompatActivity implements BenchServiceListener{

    private final static String TAG = ResultPage.class.getName();

    ArrayList<TestInfo> results;
    String testName;

    private boolean hasSendData = true;

    private GoogleConnectionHandler mGoogleConnectionHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_page);

        setupUi();
        if (getIntent().getBooleanExtra("fromBench", false)) {
            hasSendData = false;
        }
    }

    private void setupUi() {
        if (!getIntent().hasExtra("name")) {
            Log.e(TAG, "setupUi: Failed to get name extra in intent");
            return;
        }
        testName = getIntent().getStringExtra("name");
        if (getSupportActionBar() == null) {
            Log.e(TAG, "setupUi: Failed to get action bar");
            return;
        }
        getSupportActionBar().setTitle(JsonHandler.toDatePrettyPrint(testName));

        results = JsonHandler.load(testName + ".txt");
        if (results == null) {
            Log.e(TAG, "setupUi: Failed to get results from file");
            return;
        }

        RecyclerView mRecyclerView;
        RecyclerView.LayoutManager mLayoutManager;
        RecyclerView.Adapter mAdapter;

        TextView softView = (TextView) findViewById(R.id.softAvg);
        String softText = "Software score : " + format2Dec(TestInfo.getSoftScore(results)) +
                " / " + format2Dec(TestInfo.SCORE_TOTAL * results.size());
        softView.setText(softText);

        TextView hardView = (TextView) findViewById(R.id.hardAvg);
        String hardText = "Hardware score : " + format2Dec(TestInfo.getHardScore(results)) +
                " / " + format2Dec(TestInfo.SCORE_TOTAL * results.size());
        hardView.setText(hardText);

        mRecyclerView = (RecyclerView) findViewById(R.id.test_result_list);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView .setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        mAdapter = new TestResultListAdapter(results);
        mRecyclerView.setAdapter(mAdapter);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e) {
            Log.e(TAG, e.toString());
        }

        /* Sending JSON results to server */
        /* But need to connect to google first to get user id */
        Button button = (Button) findViewById(R.id.uploadButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGoogleConnectionHandler.signIn();
            }
        });

        if (BuildConfig.DEBUG) {
            View separator = findViewById(R.id.result_page_separator);
            separator.setVisibility(View.VISIBLE);

            button.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.OPENGL) {
            JSONObject res;
            try {
                res = JsonHandler.dumpResults(results, data);
                if (res == null) {
                    Log.e(TAG, "onActivityResult: res is null");
                    return;
                }
                if (mGoogleConnectionHandler != null && mGoogleConnectionHandler.getAccount() != null) {
                        res.put("email", mGoogleConnectionHandler.getAccount().getEmail());
                } else {
                    if (mGoogleConnectionHandler == null) {
                        Log.d(TAG, "onActivityResult: mGoogleConnectionHandler is null");
                    } else {
                        Log.e(TAG, "onActivityResult: Failed to get google email");
                    }
                    DialogInstance dialogInstance = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_err_google);
                    dialogInstance.display(this);
                    return;
                }
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                return;
            }
            /* Starts the upload in BenchService */
            Intent intent = new Intent(ResultPage.this, BenchService.class);
            intent.putExtra("action", ServiceActions.SERVICE_POST);
            intent.putExtra("json", res.toString());
            startService(intent);
        } else if (requestCode == RequestCodes.GOOGLE_CONNECTION) {
            /* Starts the BenchGLActivity to get gpu information */
            mGoogleConnectionHandler.handleSignInResult(data);
            startActivityForResult(new Intent(ResultPage.this, BenchGLActivity.class),
                    RequestCodes.OPENGL);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleConnectionHandler = GoogleConnectionHandler.getInstance();
        mGoogleConnectionHandler.setGoogleApiClient(this, this);
        if (!hasSendData) {
            hasSendData = true;
            mGoogleConnectionHandler.signIn();
        }
        BenchServiceDispatcher.getInstance().startService(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGoogleConnectionHandler.unsetGoogleApiClient();
        BenchServiceDispatcher.getInstance().stopService();
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

    class TestResultListAdapter extends RecyclerView.Adapter<TestResultListAdapter.ViewHolder> {
        ArrayList<TestInfo> mData;

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView title;
            private TextView mResult;

            ViewHolder(View view) {
                super(view);

                title = (TextView) view.findViewById(R.id.test_name);
                mResult = (TextView) view.findViewById(R.id.test_result);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActionBar actionBar = getSupportActionBar();
                        if (actionBar == null || actionBar.getTitle() == null) {
                            Log.e(TAG, "onClickMethod: Failed to get action bar title");
                            return;
                        }
                        String filename = JsonHandler.fromDatePrettyPrint(actionBar.getTitle().toString()) + ".txt";
                        ArrayList<TestInfo> results = JsonHandler.load(filename);
                        if (results == null) {
                            Log.e(TAG, "onClickMethod: Failed to get results");
                            return;
                        }
                        TestInfo test = results.get(getAdapterPosition());
                        Intent intent = new Intent(ResultPage.this, ResultDetailPage.class);
                        intent.putExtra("result", test);
                        startActivity(intent);
                    }
                });
            }

            void setTitle(int position) {
                this.title.setText(mData.get(position).getName());
            }

            void setResult(int position) {
                this.mResult.setText(
                        (format2Dec(mData.get(position).getHardware() +
                                mData.get(position).getSoftware()) +
                                " / " + format2Dec(TestInfo.SCORE_TOTAL * 2)));
            }
        }

        TestResultListAdapter(ArrayList<TestInfo> data) {
            mData = data;
        }

        @Override
        public TestResultListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.test_sample_rows, parent, false);
            return new TestResultListAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.setTitle(position);
            holder.setResult(position);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    /* BenchServiceListener Implementation */

    @Override
    public void displayDialog(DialogInstance dialog) {
        dialog.display(this);
    }

    /* BenchServiceListener Unused methods */

    @Override
    public void failure(FAILURE_STATES reason, Exception exception) {}
    @Override
    public void doneReceived(List<MediaInfo> files) {}
    @Override
    public void updatePercent(double percent, long bitRate) {}
    @Override
    public void stepFinished(String message) {}
    @Override
    public void setFilesDownloaded(boolean hasDownloaded) {}
    @Override
    public void doneDownload() {}
    @Override
    public void setFilesChecked(boolean hasChecked) {}
}
