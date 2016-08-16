package org.videolan.vlcbenchmark;

import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class ResultAdapter extends BaseAdapter {
    private List<TestInfo> results;
    private LayoutInflater mInflater;

    public ResultAdapter(LayoutInflater inflater, List<TestInfo> results) {
        mInflater = inflater;
        this.results = results;
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

        final TestInfo test = results.get(i);
        tv1.setText(test.getName());
        tv2.setText(String.valueOf(test.getSoftware()));
        tv3.setText(String.valueOf(test.getHardware()));

        return view;
    }

    @Override
    public boolean isEmpty() {
        return results.isEmpty();
    }
}
