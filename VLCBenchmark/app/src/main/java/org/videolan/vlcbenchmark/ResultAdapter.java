package org.videolan.vlcbenchmark;

import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        tv1.setText(test.name);
        tv2.setText(String.valueOf(test.software[0] + test.software[1]));
        tv3.setText(String.valueOf(test.hardware[0] + test.hardware[1]));

        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Dialog dialog = new Dialog(v.getContext());
                dialog.setContentView(R.layout.result_detail);
                dialog.setTitle("Details for\n" + test.name);
                TextView tmp;

                tmp = (TextView) dialog.findViewById(R.id.frames_dropped_software);
                tmp = (TextView) dialog.findViewById(R.id.frames_dropped_hardware);

                tmp = (TextView) dialog.findViewById(R.id.bad_screenshots_software);
                tmp = (TextView) dialog.findViewById(R.id.bad_screenshots_hardware);

                tmp = (TextView) dialog.findViewById(R.id.warnings_software);
                tmp = (TextView) dialog.findViewById(R.id.warnings_hardware);

                tmp = (TextView) dialog.findViewById(R.id.crashes_software);
                tmp = (TextView) dialog.findViewById(R.id.crashes_hardware);

                ((Button) dialog.findViewById(R.id.detail_dismiss)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
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
}
