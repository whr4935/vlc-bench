package org.videolan.vlcbenchmark;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.videolan.vlcbenchmark.service.TestInfo;

import java.util.List;

public class ResultAdapter extends BaseAdapter {
    List<TestInfo> results;
    private Context mContext;

    public ResultAdapter(Context c) {
        mContext = c;
    }

    @Override
    public int getCount() {
        return results.size() * 3;
    }

    @Override
    public Object getItem(int i) {
        return results.get(i / 3);
    }

    @Override
    public long getItemId(int i) {
        return i / 3;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        final TextView resultView;
        if (view == null)
            resultView = new TextView(mContext);
        else
            resultView = (TextView) view;

        switch (i % 3) {
            case 0:
                resultView.setText(results.get(i / 3).getName());
                break;
            case 1:
                resultView.setText(String.valueOf(results.get(i / 3).getHardwareScore()));
                break;
            default:
                resultView.setText(String.valueOf(results.get(i / 3).getSoftwareScore()));
                break;
        }
        resultView.setBackgroundColor(Color.WHITE);
        resultView.setGravity(Gravity.CENTER);

        resultView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                new AlertDialog.Builder(v.getContext())
                        .setTitle("Test details")
                        .setMessage("Test name : " + results.get(i / 3).getName()
                                + "\nFrames dropped : " + results.get(i / 3).getFrameDropped()
                                + "\nBad screenshots : " + results.get(i / 3).getPercentOfBadScreenshots() + "%"
                                + "\nBad seeks : " + results.get(i / 3).getPercentOfBadSeek() + "%"
                                + "\nWarnings : " + results.get(i / 3).getNumberOfWarnings()
                        )
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

        return resultView;
    }

    @Override
    public boolean isEmpty() {
        return results.isEmpty();
    }

    public void setResults(List<TestInfo> r) {
        results = r;
    }
}
