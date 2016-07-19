package org.videolan.vlcbenchmark;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.GridView;
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

        GridView gv = (GridView)findViewById(R.id.resultList);
        ResultAdapter resultAdapter = new ResultAdapter(this);
        resultAdapter.setResults(r1);
        gv.setAdapter(resultAdapter);
        gv.setFocusable(false);

        TextView softView = (TextView)findViewById(R.id.softAvg);
        softView.setText("Software score : " + soft);

        TextView hardView = (TextView)findViewById(R.id.hardAvg);
        hardView.setText("Hardware score : " + hard);

    }

}
