package org.videolan.vlcbenchmark;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ProgressBar;

import org.videolan.vlcbenchmark.R;

/**
 * Created by noeldu_b on 7/11/16.
 */
public class TestPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_page);

        ProgressBar pb = (ProgressBar)findViewById(R.id.progressBar);
    }

}
