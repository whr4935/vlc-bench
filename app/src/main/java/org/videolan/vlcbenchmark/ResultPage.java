/*
 *****************************************************************************
 * ResultPage.java
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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.videolan.vlcbenchmark.tools.DialogInstance;
import org.videolan.vlcbenchmark.tools.FormatStr;
import org.videolan.vlcbenchmark.tools.GoogleConnectionHandler;
import org.videolan.vlcbenchmark.tools.JsonHandler;
import org.videolan.vlcbenchmark.tools.TestInfo;
import org.videolan.vlcbenchmark.tools.UploadResultsTask;

import java.util.ArrayList;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static org.videolan.vlcbenchmark.tools.FormatStr.format2Dec;

public class ResultPage extends AppCompatActivity implements UploadResultsTask.IUploadResultsTask {

    private final static String TAG = ResultPage.class.getName();

    ArrayList<TestInfo> results;
    String testName;

    private boolean hasSendData = true;

    private GoogleConnectionHandler mGoogleConnectionHandler;
    private UploadResultsTask uploadResultsTask;
    private AlertDialog progressDialog;

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        if (toolbar == null) {
            Log.e(TAG, "setupUi: Failed to get action bar");
            return;
        }
        toolbar.setTitle(FormatStr.toDatePrettyPrint(testName));
        setSupportActionBar(toolbar);

        results = JsonHandler.load(testName + ".txt");
        if (results == null) {
            Log.e(TAG, "setupUi: Failed to get results from file");
            return;
        }

        RecyclerView mRecyclerView;
        RecyclerView.LayoutManager mLayoutManager;
        RecyclerView.Adapter mAdapter;

        TextView softView = (TextView) findViewById(R.id.softAvg);
        String softText = "Software score : " + format2Dec(TestInfo.getSoftScore(results));
        softView.setText(softText);

        TextView hardView = (TextView) findViewById(R.id.hardAvg);
        String hardText = "Hardware score : " + format2Dec(TestInfo.getHardScore(results));
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

        if (BuildConfig.BUILD_TYPE.equals("debug") || BuildConfig.BUILD_TYPE.equals("debug_prod")) {
            /* Sending JSON results to server */
            /* But need to connect to google first to get user id */
            Button button = (Button) findViewById(R.id.uploadButton);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mGoogleConnectionHandler.isConnected()) {
                        startActivityForResult(new Intent(ResultPage.this, BenchGLActivity.class),
                                Constants.RequestCodes.OPENGL);
                    } else {
                        mGoogleConnectionHandler.signIn();
                    }
                }
            });

            View separator = findViewById(R.id.result_page_separator);
            separator.setVisibility(View.VISIBLE);

            button.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mGoogleConnectionHandler = GoogleConnectionHandler.getInstance();
        mGoogleConnectionHandler.setGoogleSignInClient(this, this);
        if (requestCode == Constants.RequestCodes.OPENGL) {
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
            progressDialog = new AlertDialog.Builder(this)
                    .setView(R.layout.layout_upoad_progress_dialog)
                    .setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (uploadResultsTask != null) {
                                uploadResultsTask.cancel(true);
                            }
                        }
                    }).show();
            uploadResultsTask = new UploadResultsTask(this);
            uploadResultsTask.execute(res.toString());
        } else if (requestCode == Constants.RequestCodes.GOOGLE_CONNECTION) {
            /* Starts the BenchGLActivity to get gpu information */
            if (mGoogleConnectionHandler.handleSignInResult(data)) {
                startActivityForResult(new Intent(ResultPage.this, BenchGLActivity.class),
                        Constants.RequestCodes.OPENGL);
            } else {
                Log.e(TAG, "onActivityResult: failed to log in google");
            }
        }
    }

    @Override
    public void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleConnectionHandler = GoogleConnectionHandler.getInstance();
        mGoogleConnectionHandler.setGoogleSignInClient(this, this);
        if (!hasSendData) {
            hasSendData = true;
            mGoogleConnectionHandler.signIn();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGoogleConnectionHandler.unsetGoogleSignInClient();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (uploadResultsTask != null) {
            uploadResultsTask.cancel(true);
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
                        String filename = FormatStr.fromDatePrettyPrint(actionBar.getTitle().toString()) + ".txt";
                        ArrayList<TestInfo> results = JsonHandler.load(filename);
                        if (results == null) {
                            Log.e(TAG, "onClickMethod: Failed to get results");
                            return;
                        }
                        TestInfo test = results.get(getAdapterPosition());
                        Intent intent = new Intent(ResultPage.this, ResultDetailPage.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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
}
