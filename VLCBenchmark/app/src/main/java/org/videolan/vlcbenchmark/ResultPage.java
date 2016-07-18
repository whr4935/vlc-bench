package org.videolan.vlcbenchmark;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;

public class ResultPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_page);

        List<String> r = new ArrayList<>();
        r.add(0, "Name");
        r.add(1, "Software");
        r.add(2, "Hardware");
        r.add(3, "test_mp4");
        r.add(4, "35");
        r.add(5, "28");
        r.add(6, "test_mpg2");
        r.add(7, "34");
        r.add(8, "35");

        GridView gv = (GridView)findViewById(R.id.resultList);
        ResultAdapter resultAdapter = new ResultAdapter(this);
        resultAdapter.setResults(r);
        gv.setAdapter(resultAdapter);
        gv.setFocusable(false);
    }

}
