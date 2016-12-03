package org.videolan.vlcbenchmark;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.videolan.vlcbenchmark.tools.JsonHandler;
import org.videolan.vlcbenchmark.tools.TestInfo;

import java.util.ArrayList;

public class ResultListActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private BottomNavigationView bottomNavigationView;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_list);

        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Results"); //Not working ?? Oo
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));

        mRecyclerView = (RecyclerView) findViewById(R.id.test_list_view);

        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        mAdapter = new TestListAdapter(JsonHandler.getFileNames());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, mRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        Log.e("VLCBench", "Called onItemClick");
                        TextView text = (TextView) view.findViewById(R.id.test_name);
                        String fileName = text.getText() + ".txt";
                        ArrayList<TestInfo>[] testInfoList = new ArrayList[]{new ArrayList<TestInfo>()};
                        testInfoList[0] = JsonHandler.load(fileName);
                        if (testInfoList[0] != null) {
                            Intent intent = new Intent(ResultListActivity.this, ResultPage.class);
                            intent.putExtra("resultsTest", testInfoList);
                            intent.putExtra("soft", getSoftScore(testInfoList[0]));
                            intent.putExtra("hard", getHardScore(testInfoList[0]));
                            startActivityForResult(intent, getResources().getInteger(R.integer.requestResults));
                        }
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        Log.e("VLCBench", "Called onLongItemClick");
                    }
                }
        ));

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_bar);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.home_nav:
                                Intent homeIntent = new Intent(ResultListActivity.this, MainPage.class);
                                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                startActivity(homeIntent);
                                break;
                            case R.id.results_nav:
                                break;
                            case R.id.settings_nav:
                                Intent settingsIntent = new Intent(ResultListActivity.this, SettingsPage.class);
                                settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                startActivity(settingsIntent);
                                break;
                        }
                        return false;
                    }
                }
        );
    }

    private double getHardScore(ArrayList<TestInfo> testInfo) {
        double hardware = 0;
        for (TestInfo info : testInfo) {
            hardware += info.getHardware();
            Log.e("VLCBench", "name = " + info.getName());
        }
        hardware /= testInfo.size();
        return hardware;
    }

    private double getSoftScore(ArrayList<TestInfo> testInfo) {
        double software = 0;
        for (TestInfo info : testInfo) {
            Log.e("VLCBench", "name = " + info.getName());
            software += info.getSoftware();
        }
        software /= testInfo.size();
        return software;
    }

    public class TestListAdapter extends RecyclerView.Adapter<TestListAdapter.ViewHolder> {

        ArrayList<String> mData;

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView mTextView;
            public ViewHolder(View view) {
                super(view);
                mTextView = (TextView) view.findViewById(R.id.test_name);
            }
        }

        public TestListAdapter(ArrayList<String> data) {
            mData = data;
        }

        @Override
        public TestListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            android.view.View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.test_list_rows, parent, false);
            return new TestListAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mTextView.setText(mData.get(position));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }
}
