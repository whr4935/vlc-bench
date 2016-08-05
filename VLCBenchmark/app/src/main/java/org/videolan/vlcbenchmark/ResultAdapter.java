package org.videolan.vlcbenchmark;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class ResultAdapter extends BaseAdapter {
    private List<TestInfo> results;
    private LayoutInflater mInflater;

    public ResultAdapter(Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return results.size();
    }

    @Override
    public Object getItem(int i) {
        return results.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        if (view == null)
            view = mInflater.inflate(R.layout.result_row, null);
        TextView tv1 = (TextView) view.findViewById(R.id.fileName);
        TextView tv2 = (TextView) view.findViewById(R.id.softScore);
        TextView tv3 = (TextView) view.findViewById(R.id.hardScore);

        tv1.setText(results.get(i).name);
        tv2.setText(String.valueOf(results.get(i).software));
        tv3.setText(String.valueOf(results.get(i).hardware));

        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                new AlertDialog.Builder(v.getContext())
                        .setTitle("Test details")
                        .setMessage("Test name : " + results.get(i).name
                                + "\nFrames dropped : " + doubleToPercentString(results.get(i).percentOfFrameDrop)
                                + "\nBad screenshots : " + doubleToPercentString(results.get(i).percentOfBadScreenshots)
                                + "\nBad seeks : " + doubleToPercentString(results.get(i).percentOfBadSeek)
                                + "\nWarnings : " + results.get(i).numberOfWarnings
                        )
                        .setPositiveButton(android.R.string.yes, null)
                        .show();
            }
        });

        return view;
    }

    private static String doubleToPercentString(double value) {
        return String.format("%.2f%%", value);
    }

    @Override
    public boolean isEmpty() {
        return results.isEmpty();
    }

    public void setResults(List<TestInfo> r) {
        results = r;
    }
}