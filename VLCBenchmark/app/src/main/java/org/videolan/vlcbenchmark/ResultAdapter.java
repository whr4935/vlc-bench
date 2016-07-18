package org.videolan.vlcbenchmark;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class ResultAdapter extends BaseAdapter {
    List<String> results;
    private Context mContext;

    public ResultAdapter(Context c) {
        mContext = c;
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
        return i / 3;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        TextView resultView;
        if (view == null)
            resultView = new TextView(mContext);
        else
            resultView = (TextView) view;

        resultView.setText(results.get(i));
        resultView.setBackgroundColor(Color.WHITE);
        resultView.setGravity(Gravity.CENTER);
        return resultView;
    }

    @Override
    public boolean isEmpty() {
        return results.isEmpty();
    }

    public void setResults(List<String> r) {
        results = r;
    }
}
