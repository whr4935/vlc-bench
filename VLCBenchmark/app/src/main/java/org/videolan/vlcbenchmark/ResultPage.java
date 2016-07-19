package org.videolan.vlcbenchmark;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.GridView;
import android.widget.TabHost;
import android.widget.TextView;

import org.videolan.vlcbenchmark.service.TestInfo;

import java.util.ArrayList;
import java.util.List;

public class ResultPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_page);

        ArrayList<TestInfo> r1 = (ArrayList<TestInfo>)getIntent().getSerializableExtra("resultsTestOne");
        ArrayList<TestInfo> r2 = (ArrayList<TestInfo>)getIntent().getSerializableExtra("resultsTestTwo");
        ArrayList<TestInfo> r3 = (ArrayList<TestInfo>)getIntent().getSerializableExtra("resultsTestThree");
        double soft = getIntent().getDoubleExtra("soft", 0);
        double hard = getIntent().getDoubleExtra("hard", 0);

        GridView gv = (GridView)findViewById(R.id.resultList1);
        ResultAdapter resultAdapter = new ResultAdapter(this);
        resultAdapter.setResults(r1);
        gv.setAdapter(resultAdapter);
        gv.setFocusable(false);

        if (r2.size() > 0) {
            GridView gv2 = (GridView)findViewById(R.id.resultList2);
            ResultAdapter resultAdapter2 = new ResultAdapter(this);
            resultAdapter2.setResults(r2);
            gv2.setAdapter(resultAdapter2);
            gv2.setFocusable(false);
        }

        if (r3.size() > 0) {
            GridView gv3 = (GridView)findViewById(R.id.resultList3);
            ResultAdapter resultAdapter3 = new ResultAdapter(this);
            resultAdapter3.setResults(r3);
            gv3.setAdapter(resultAdapter3);
            gv3.setFocusable(false);
        }

        TextView softView = (TextView)findViewById(R.id.softAvg);
        softView.setText("Software score : " + soft);

        TextView hardView = (TextView)findViewById(R.id.hardAvg);
        hardView.setText("Hardware score : " + hard);

    }

}
