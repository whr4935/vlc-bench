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
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.videolan.vlcbenchmark.api.ApiCalls;
import org.videolan.vlcbenchmark.tools.FormatStr;
import org.videolan.vlcbenchmark.tools.GoogleConnectionHandler;
import org.videolan.vlcbenchmark.tools.JsonHandler;
import org.videolan.vlcbenchmark.tools.StorageManager;
import org.videolan.vlcbenchmark.tools.TestInfo;
import org.videolan.vlcbenchmark.tools.Util;

import java.util.ArrayList;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ResultPage extends AppCompatActivity {

    private final static String TAG = ResultPage.class.getName();

    ArrayList<TestInfo> results;
    String testName;
    RecyclerView mRecyclerView = null;

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        if (toolbar == null) {
            Log.e(TAG, "setupUi: Failed to get action bar");
            return;
        }
        toolbar.setTitle(FormatStr.INSTANCE.toDatePrettyPrint(testName));
        setSupportActionBar(toolbar);

        results = JsonHandler.load(testName + ".txt");
        if (results == null) {
            Log.e(TAG, "setupUi: Failed to get results from file");
            return;
        }

        RecyclerView.LayoutManager mLayoutManager;
        RecyclerView.Adapter mAdapter;

        TextView softView = (TextView) findViewById(R.id.softAvg);
        String softText = "Software score : " + FormatStr.INSTANCE.format2Dec(TestInfo.getSoftScore(results));
        softView.setText(softText);

        TextView hardView = (TextView) findViewById(R.id.hardAvg);
        String hardText = "Hardware score : " + FormatStr.INSTANCE.format2Dec(TestInfo.getHardScore(results));
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

    JSONObject addGoogleUser(JSONObject jsonObject) throws JSONException{
        if (mGoogleConnectionHandler != null && mGoogleConnectionHandler.getAccount() != null) {
            jsonObject.put("email", mGoogleConnectionHandler.getAccount().getEmail());
            return jsonObject;
        } else {
            if (mGoogleConnectionHandler == null) {
                Log.d(TAG, "onActivityResult: mGoogleConnectionHandler is null");
            } else {
                Log.e(TAG, "onActivityResult: Failed to get google email");
            }
            Toast.makeText(this, R.string.dialog_text_err_google, Toast.LENGTH_LONG).show();
            return null;
        }
    }

    void prepareBenchmarkUpload(Boolean withScreenshots, Intent data) {
        try {
            JSONObject jsonObject = JsonHandler.dumpResults(this, results, data, withScreenshots);
            jsonObject = addGoogleUser(jsonObject);
            if (withScreenshots) {
                ApiCalls.uploadBenchmarkWithScreenshots(this, jsonObject, results);
            } else {
                ApiCalls.uploadBenchmark(this, jsonObject);
            }

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            Toast.makeText(this, R.string.toast_text_error_prep_upload, Toast.LENGTH_LONG).show();
        }
    }

    void startUploadDialog(Intent data) {
        String directory = StorageManager.INSTANCE.getDirectory() + StorageManager.screenshotFolder;
        String size = FormatStr.INSTANCE.byteSizeToString(this,
                StorageManager.INSTANCE.getDirectoryMemoryUsage(directory));
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_result_upload)
                .setMessage(String.format(getString(R.string.dialog_text_result_upload), size))
                .setNegativeButton(R.string.dialog_btn_upload_with, (DialogInterface dialog, int which) ->
                        prepareBenchmarkUpload(true, data)
                )
                .setNeutralButton(R.string.dialog_btn_upload_without, (DialogInterface dialog, int which) ->
                        prepareBenchmarkUpload(false, data)
                )
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mGoogleConnectionHandler = GoogleConnectionHandler.getInstance();
        mGoogleConnectionHandler.setGoogleSignInClient(this, this);
        if (requestCode == Constants.RequestCodes.OPENGL) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_sample_deletion)
                    .setMessage(R.string.dialog_text_sample_free_space)
                    .setNegativeButton(R.string.dialog_btn_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            StorageManager.INSTANCE.deleteDirectory(StorageManager.INSTANCE.getDirectory() + StorageManager.mediaFolder);
                            startUploadDialog(data);
                        }
                    })
                    .setNeutralButton(R.string.dialog_btn_no, (DialogInterface dialog, int which) ->
                            startUploadDialog(data)
                    ).show();
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
    protected void onResume() {
        super.onResume();
        mGoogleConnectionHandler = GoogleConnectionHandler.getInstance();
        mGoogleConnectionHandler.setGoogleSignInClient(this, this);
        if (!hasSendData) {
            hasSendData = true;
            mGoogleConnectionHandler.signIn();
            if (mGoogleConnectionHandler.isConnected()) {
                startActivityForResult(new Intent(ResultPage.this, BenchGLActivity.class),
                        Constants.RequestCodes.OPENGL);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGoogleConnectionHandler.unsetGoogleSignInClient();
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

    @Override
    public void onBackPressed() {
        if (Util.isAndroidTV(this) &&
                mRecyclerView != null &&
                mRecyclerView.hasFocus() &&
                mRecyclerView.findViewHolderForLayoutPosition(0) != null &&
                !mRecyclerView.findViewHolderForLayoutPosition(0).itemView.hasFocus()) {
            mRecyclerView.findViewHolderForLayoutPosition(0).itemView.requestFocus();
            return ;
        }
        super.onBackPressed();
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
                        String filename = FormatStr.INSTANCE.fromDatePrettyPrint(actionBar.getTitle().toString()) + ".txt";
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
                        (FormatStr.INSTANCE.format2Dec(mData.get(position).getHardware() +
                                mData.get(position).getSoftware()) +
                                " / " + FormatStr.INSTANCE.format2Dec(TestInfo.SCORE_TOTAL * 2)));
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
