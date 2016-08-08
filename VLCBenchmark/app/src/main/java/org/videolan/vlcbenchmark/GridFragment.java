package org.videolan.vlcbenchmark;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class GridFragment extends Fragment {
    private List<TestInfo> results;

    public GridFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            results = (List<TestInfo>)getArguments().getSerializable("results");
        }
    }

    private void fillHeader(View view, int id, String text) {
        TextView textView = (TextView) view.findViewById(id);
        textView.setText(text);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setTextColor(Color.BLACK);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_grid, container, false);
        ListView gv = (ListView)v.findViewById(R.id.resultList);
        LayoutInflater rowInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = rowInflater.inflate(R.layout.result_row, null);
        fillHeader(rowView, R.id.fileName, "Video name");
        fillHeader(rowView, R.id.softScore, "Software");
        fillHeader(rowView, R.id.hardScore, "Hardware");
        rowView.setMinimumHeight(0);
        gv.addHeaderView(rowView);
        gv.setAdapter(new ResultAdapter(rowInflater, results));
        gv.setFocusable(false);
        return v;
    }
}
