package org.videolan.vlcbenchmark;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.widget.TabHost;
import android.widget.TextView;

import org.videolan.vlcbenchmark.service.TestInfo;

import java.util.ArrayList;

import values.GridFragment;

public class ResultPage extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_page);

        ArrayList<TestInfo> r1 = (ArrayList<TestInfo>)getIntent().getSerializableExtra("resultsTestOne");
        ArrayList<TestInfo> r2 = (ArrayList<TestInfo>)getIntent().getSerializableExtra("resultsTestTwo");
        ArrayList<TestInfo> r3 = (ArrayList<TestInfo>)getIntent().getSerializableExtra("resultsTestThree");
        double soft = getIntent().getDoubleExtra("soft", 0);
        double hard = getIntent().getDoubleExtra("hard", 0);

        final FragmentTabHost mTabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

        Bundle args = new Bundle();
        args.putSerializable("results", r1);
        mTabHost.addTab(mTabHost.newTabSpec("tab1").setIndicator("Test 1"), GridFragment.class, args);
        if (r2.size() > 0) {
            args = new Bundle();
            args.putSerializable("results", r2);
            mTabHost.addTab(mTabHost.newTabSpec("tab2").setIndicator("Test 2"), GridFragment.class, args);
        }
        if (r3.size() > 0) {
            args = new Bundle();
            args.putSerializable("results", r3);
            mTabHost.addTab(mTabHost.newTabSpec("tab3").setIndicator("Test 3"), GridFragment.class, args);
        }

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener(){
            @Override
            public void onTabChanged(String tabId) {
            }});

        TextView softView = (TextView)findViewById(R.id.softAvg);
        String softText = "Software score : " + soft;
        softView.setText(softText);

        TextView hardView = (TextView)findViewById(R.id.hardAvg);
        String hardText = "Hardware score : " + hard;
        hardView.setText(hardText);

    }

}
