/*
 *****************************************************************************
 * MainPageResultListFragment.java
 *****************************************************************************
 * Copyright © 2016-2018 VLC authors and VideoLAN
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

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.videolan.vlcbenchmark.tools.DialogInstance;
import org.videolan.vlcbenchmark.tools.FormatStr;
import org.videolan.vlcbenchmark.tools.JsonHandler;
import org.videolan.vlcbenchmark.tools.StorageManager;
import org.videolan.vlcbenchmark.tools.TestInfo;
import org.videolan.vlcbenchmark.tools.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 */
public class MainPageResultListFragment extends Fragment {

    private final static String TAG = MainPageResultListFragment.class.getName();

    public MainPageResultListFragment() {}

    private static int scrollPosition = -1;

    RecyclerView mRecyclerView;
    ArrayList<Pair<String, String>> mData;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ArrayList<String> tmpdata;
        RecyclerView.Adapter mAdapter;
        RecyclerView.LayoutManager mLayoutManager;

        View view = inflater.inflate(R.layout.fragment_main_page_result_list_fragment, container, false);
        LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.no_results);
        linearLayout.setFocusable(false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.test_list_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setFocusable(true);

        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL));
        mData = new ArrayList<>();
        mAdapter = new MainPageResultListFragment.TestListAdapter(mData);
        mRecyclerView.setAdapter(mAdapter);

        ArrayList<String> filenames = JsonHandler.getFileNames();
        if (filenames != null) {
            tmpdata = orderBenchmarks(JsonHandler.getFileNames());

            if (tmpdata.isEmpty()) {
                view.findViewById(R.id.no_results).setVisibility(View.VISIBLE);
            } else {
                Util.runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        for (String filename : tmpdata) {
                            ArrayList<TestInfo> test = JsonHandler.load(filename + ".txt");
                            if (test != null) {
                                final String title = FormatStr.INSTANCE.toDatePrettyPrint(filename);
                                if (getContext() == null ) {
                                    Log.w(TAG, "No context for background json loading," +
                                            " fragment must have been replaced");
                                    return;
                                }
                                final String text = String.format(getResources().getString(R.string.result_score),
                                        FormatStr.INSTANCE.format2Dec(TestInfo.getGlobalScore(test)));
                                Util.runInUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        addResult(title, text);
                                    }
                                });
                            }
                        }
                    }
                });
            }
        } else {
            new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_loading_results_failure).display(getContext());
        }
        return view;
    }

    public void addResult(String name, String result) {
        mData.add(new Pair<>(name, result));
        mRecyclerView.getAdapter().notifyItemInserted(mData.size());
    }

    public boolean isEmpty() {
        return mData.isEmpty();
    }

    boolean isScrollingPositionTop() {
        if (mRecyclerView != null && mRecyclerView.findViewHolderForLayoutPosition(0) != null) {
            return mRecyclerView.findViewHolderForLayoutPosition(0).itemView.hasFocus();
        }
        return false;
    }

    void scrollingBackToTop() {
        if (mRecyclerView.findViewHolderForLayoutPosition(0) != null) {
            mRecyclerView.findViewHolderForLayoutPosition(0).itemView.requestFocus();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (scrollPosition != -1) {
            mRecyclerView.getLayoutManager().scrollToPosition(scrollPosition);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        scrollPosition = ((LinearLayoutManager)mRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
    }

    public class TestListAdapter extends RecyclerView.Adapter<TestListAdapter.ViewHolder> {

        ArrayList<Pair<String, String>> mData;

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView mTitle;
            TextView mResult;

            ViewHolder(View view) {
                super(view);
                mTitle = (TextView) view.findViewById(R.id.test_name);
                mResult = (TextView) view.findViewById(R.id.test_result);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        TextView text = (TextView) view.findViewById(R.id.test_name);
                        Intent intent = new Intent(getActivity(), ResultPage.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        intent.putExtra("name", FormatStr.INSTANCE.fromDatePrettyPrint(text.getText().toString()));
                        startActivityForResult(intent, Constants.RequestCodes.RESULTS);
                    }
                });
                view.setOnLongClickListener((view1) -> {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.dialog_title_warning)
                            .setMessage(R.string.dialog_text_confirm_bench_delete)
                            .setNeutralButton(R.string.dialog_btn_cancel, null)
                            .setNegativeButton(R.string.dialog_btn_continue, (dialog, w1) -> {
                                File sample = new File(StorageManager.INSTANCE.getInternalDirStr(StorageManager.INSTANCE.jsonFolder)
                                        + FormatStr.INSTANCE.fromDatePrettyPrint(mTitle.getText().toString())
                                        + ".txt");
                                StorageManager.INSTANCE.delete(sample);
                                mData.remove(getAdapterPosition());
                                mRecyclerView.removeViewAt(getAdapterPosition());
                                notifyItemRemoved(getAdapterPosition());
                            })
                            .show();
                    return true;
                });
            }
        }



        TestListAdapter(ArrayList<Pair<String, String>> data) {
            mData = data;
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            super.onDetachedFromRecyclerView(recyclerView);
        }

        @Override
        public TestListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.test_list_rows, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final TestListAdapter.ViewHolder holder, final int position) {
            holder.mTitle.setText(mData.get(position).first);
            holder.mResult.setText(mData.get(position).second);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    private ArrayList<String> orderBenchmarks(ArrayList<String> str_dates) {
        ArrayList<PairDateStr> data = new ArrayList<>();
        for (String str_date : str_dates) {
            Date date = FormatStr.INSTANCE.strDateToDate(str_date);
            if (date == null) {
                Log.i(TAG, "orderBenchmarks: skipping date");
                continue;
            }
            PairDateStr pair = new PairDateStr(date, str_date);
            if (pair.pair.first != null) {
                data.add(pair);
            }
        }
        Collections.sort(data);
        Collections.reverse(data);
        ArrayList<String> orderedDates = new ArrayList<>();
        for (PairDateStr element : data) {
            orderedDates.add(element.pair.second);
        }
        return orderedDates;
    }

    class PairDateStr implements Comparable<PairDateStr> {

        Pair<Date, String> pair;

        PairDateStr(Date date, String str) {
            pair = new Pair<>(date, str);
        }

        @Override
        public int compareTo(@NonNull PairDateStr o) {
            return pair.first.compareTo(o.pair.first);
        }
    }

}
