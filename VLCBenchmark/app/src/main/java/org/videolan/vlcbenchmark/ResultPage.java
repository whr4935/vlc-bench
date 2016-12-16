/*****************************************************************************
 * ResultPage.java
 *****************************************************************************
 * Copyright © 2011-2014 VLC authors and VideoLAN
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
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.*;
import android.support.v7.widget.DividerItemDecoration;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.videolan.vlcbenchmark.tools.JsonHandler;
import org.videolan.vlcbenchmark.tools.TestInfo;
import org.videolan.vlcbenchmark.tools.resultPage.GridFragment;

import java.util.ArrayList;

public class ResultPage extends AppCompatActivity {//FragmentActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView.Adapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_page);

        setupUi();
    }

    private void setupUi() {
        if (!getIntent().hasExtra("name")) {
            Log.e("VLCBench", "Failed to get name extra in intent");
            return;
        }
        String name = getIntent().getStringExtra("name");
        getSupportActionBar().setTitle(JsonHandler.toDatePrettyPrint(name));

        ArrayList<TestInfo> results = JsonHandler.load(name + ".txt");
        if (results == null) {
            return;
        }

        TextView softView = (TextView) findViewById(R.id.softAvg);
        String softText = "Software score : " + TestInfo.getSoftScore(results) + " / " + (TestInfo.SCORE_TOTAL * results.size());
        softView.setText(softText);

        TextView hardView = (TextView) findViewById(R.id.hardAvg);
        String hardText = "Hardware score : " + TestInfo.getHardScore(results) + " / " + (TestInfo.SCORE_TOTAL * results.size());
        hardView.setText(hardText);

        mRecyclerView = (RecyclerView) findViewById(R.id.test_result_list);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView .setLayoutManager(mLayoutManager);

        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        mAdapter = new TestResultListAdapter(results);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, mRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        String filename = JsonHandler.fromDatePrettyPrint(getSupportActionBar().getTitle().toString()) + ".txt";
                        ArrayList<TestInfo> results = JsonHandler.load(filename);
                        TestInfo test = results.get(position);
                        Intent intent = new Intent(ResultPage.this, ResultDetailPage.class);
                        intent.putExtra("result", test);
                        startActivity(intent);
                        Log.e("VLCBench", "onItemClick called");
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        Log.e("VLCBench", "onLongItemClick called");
                    }
                })
        );

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e) {
            Log.e("VLCBenchmark", e.toString());
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

    public class TestResultListAdapter extends RecyclerView.Adapter<TestResultListAdapter.ViewHolder> {
        ArrayList<TestInfo> mData;

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView title;
            public TextView mResult;

            public ViewHolder(View view) {
                super(view);

                title = (TextView) view.findViewById(R.id.test_name);
                title.setTextSize(16);
                mResult = (TextView) view.findViewById(R.id.test_result);
                mResult.setTextSize(12);
            }
        }

        public TestResultListAdapter(ArrayList<TestInfo> data) {
            Log.e("VLCBench", "in TestResultListAdapter()");
            for (TestInfo info : data) {
                Log.e("VLCBench", "test name is " + info.getName());
            }
            mData = data;
        }

        @Override
        public TestResultListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.test_list_rows, parent, false);
            Log.e("VLCBench", "onCreateViewHolder");
            return new TestResultListAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Log.e("VLCBench", "onBindViewHolder");
            holder.title.setText(mData.get(position).getName());
            holder.mResult.setText("Result: " +
                    ((mData.get(position).getHardware() + mData.get(position).getSoftware() + " / " + (TestInfo.SCORE_TOTAL * 2))));
        }

        @Override
        public int getItemCount() {
            Log.e("VLCBench", "size = " + mData.size());
            return mData.size();
        }
    }
}
