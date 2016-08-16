package org.videolan.vlcbenchmark;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class GridFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {
    private List<TestInfo> results;
    private Dialog detailDialog;

    public GridFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        detailDialog = new Dialog(getContext());
        detailDialog.setContentView(R.layout.result_detail);
        detailDialog.findViewById(R.id.detail_dismiss).setOnClickListener(this);
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
    public void onClick(View view) {
        detailDialog.dismiss();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (l < 0 || l >= results.size())
            return ;

        TestInfo test = results.get((int)l);
        detailDialog.setTitle("Details for\n" + test.getName());
        TextView tmp;

        tmp = (TextView) detailDialog.findViewById(R.id.frames_dropped_software);
        tmp.setText("" + test.getFrameDropped(TestInfo.SOFT));
        tmp = (TextView) detailDialog.findViewById(R.id.frames_dropped_hardware);
        tmp.setText("" + test.getFrameDropped(TestInfo.HARD));

        tmp = (TextView) detailDialog.findViewById(R.id.bad_screenshots_software);
        tmp.setText(doubleToPercentString(test.getBadScreenshots(TestInfo.SOFT)));
        tmp = (TextView) detailDialog.findViewById(R.id.bad_screenshots_hardware);
        tmp.setText(doubleToPercentString(test.getBadScreenshots(TestInfo.HARD)));

        tmp = (TextView) detailDialog.findViewById(R.id.warnings_software);
        tmp.setText("" + test.getNumberOfWarnings(TestInfo.SOFT));
        tmp = (TextView) detailDialog.findViewById(R.id.warnings_hardware);
        tmp.setText("" + test.getNumberOfWarnings(TestInfo.HARD));

        tmp = (TextView) detailDialog.findViewById(R.id.crashes_software);
        tmp.setText("" + test.getCrashes(TestInfo.SOFT));
        tmp = (TextView) detailDialog.findViewById(R.id.crashes_hardware);
        tmp.setText("" + test.getCrashes(TestInfo.HARD));

        Window window = detailDialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, window.getAttributes().height);
        detailDialog.show();
    }

    private static String doubleToPercentString(double value) {
        return String.format("%.2f%%", value);
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
        gv.setOnItemClickListener(this);
        gv.setAdapter(new ResultAdapter(rowInflater, results));
        gv.setFocusable(false);
        return v;
    }
}
