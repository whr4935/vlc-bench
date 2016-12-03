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
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.videolan.vlcbenchmark.tools.JsonHandler;
import org.videolan.vlcbenchmark.tools.TestInfo;
import org.videolan.vlcbenchmark.tools.resultPage.GridFragment;

import java.util.ArrayList;

public class ResultPage extends AppCompatActivity {//FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_page);

        setupUi();

        ArrayList<TestInfo>[] results = (ArrayList<TestInfo>[]) getIntent().getSerializableExtra("resultsTest");
        double soft = getIntent().getDoubleExtra("soft", 0f);
        double hard = getIntent().getDoubleExtra("hard", 0f);

        final FragmentTabHost mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), android.R.id.tabcontent);

        int index = 0;
        for (ArrayList<TestInfo> resultList : results) {
            for (TestInfo info : resultList) {
                info.display();
            }
            Bundle args = new Bundle();
            args.putSerializable("results", resultList);
            mTabHost.addTab(mTabHost.newTabSpec("tab" + index).setIndicator("Test number " + (index + 1)), GridFragment.class, args);
            index++;
        }

        TextView softView = (TextView) findViewById(R.id.softAvg);
        String softText = "Software score : " + soft;
        softView.setText(softText);

        TextView hardView = (TextView) findViewById(R.id.hardAvg);
        String hardText = "Hardware score : " + hard;
        hardView.setText(hardText);

    }

    private void setupUi() {
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

//    @Override
//    public void onBackPressed() {
//        new AlertDialog.Builder(this).setTitle("Are you sure?").setMessage("Are you sure you want to go back?\nYou'll have no way to come back here.")
//                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        ResultPage.super.onBackPressed();
//                    }
//                }).setNegativeButton(android.R.string.no, null).show();
//    }
}
