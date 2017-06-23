/*****************************************************************************
 * MainPageResultListFragment.java
 *****************************************************************************
 * Copyright © 2016-2017 VLC authors and VideoLAN
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
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.videolan.vlcbenchmark.tools.FormatStr;
import org.videolan.vlcbenchmark.tools.JsonHandler;
import org.videolan.vlcbenchmark.tools.TestInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import static org.videolan.vlcbenchmark.tools.FormatStr.format2Dec;

/**
 * A simple {@link Fragment} subclass.
 */
public class MainPageResultListFragment extends Fragment {

    public MainPageResultListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ArrayList<String> data;
        RecyclerView mRecyclerView;
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

        data = orderBenchmarks(JsonHandler.getFileNames());
        mAdapter = new MainPageResultListFragment.TestListAdapter(data);

        if (data.isEmpty()) {
            view.findViewById(R.id.no_results).setVisibility(View.VISIBLE);
        }

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this.getContext(), mRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        onClickMethod(view);
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                    }
                }
                ));
        return view;
    }

    private void onClickMethod(View view) {
        TextView text = (TextView) view.findViewById(R.id.test_name);
        Intent intent = new Intent(getActivity(), ResultPage.class);
        intent.putExtra("name", JsonHandler.fromDatePrettyPrint(text.getText().toString()));
        startActivityForResult(intent, RequestCodes.RESULTS);
    }

    public class TestListAdapter extends RecyclerView.Adapter<TestListAdapter.ViewHolder> {

        ArrayList<String> mData;

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView mTitle;
            TextView mResult;

            ViewHolder(View view) {
                super(view);
                mTitle = (TextView) view.findViewById(R.id.test_name);
                mResult = (TextView) view.findViewById(R.id.test_result);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onClickMethod(v);
                    }
                });
            }

        }

        TestListAdapter(ArrayList<String> data) {
            mData = data;
        }

        @Override
        public TestListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.test_list_rows, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(TestListAdapter.ViewHolder holder, int position) {
            ArrayList<TestInfo> test = JsonHandler.load(mData.get(position) + ".txt");
            if (test != null) {
                holder.mTitle.setText(JsonHandler.toDatePrettyPrint(mData.get(position)));
                TestInfo.getGlobalScore(test);
                holder.mResult.setText(String.format(getResources().getString(R.string.result_score),
                        format2Dec(TestInfo.getGlobalScore(test)), format2Dec(test.size() * 2 * TestInfo.SCORE_TOTAL)));
            }
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    private ArrayList<String> orderBenchmarks(ArrayList<String> str_dates) {
        ArrayList<PairDateStr> data = new ArrayList<>();
        for (String str_date : str_dates) {
            PairDateStr pair = new PairDateStr(FormatStr.strDateToDate(JsonHandler.toDatePrettyPrint(str_date)), str_date);
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
