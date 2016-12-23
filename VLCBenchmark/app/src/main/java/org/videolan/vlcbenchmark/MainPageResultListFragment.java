/*****************************************************************************
 * MainPageResultListFragment.java
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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.videolan.vlcbenchmark.tools.JsonHandler;
import org.videolan.vlcbenchmark.tools.TestInfo;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class MainPageResultListFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    public MainPageResultListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main_page_result_list_fragment, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.test_list_view);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL));

        mAdapter = new MainPageResultListFragment.TestListAdapter(JsonHandler.getFileNames());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this.getContext(), mRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        TextView text = (TextView) view.findViewById(R.id.test_name);
                        Intent intent = new Intent(getActivity(), ResultPage.class);
                        intent.putExtra("name", JsonHandler.fromDatePrettyPrint(text.getText().toString()));
                        startActivityForResult(intent, getResources().getInteger(R.integer.requestResults));
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                    }
                }
                ));
        return view;
    }



    public class TestListAdapter extends RecyclerView.Adapter<TestListAdapter.ViewHolder> {

        ArrayList<String> mData;

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView mTitle;
            public TextView mResult;

            public ViewHolder(View view) {
                super(view);
                mTitle = (TextView) view.findViewById(R.id.test_name);
                mTitle.setTextSize(16);
                mResult = (TextView) view.findViewById(R.id.test_result);
                mResult.setTextSize(12);
            }

        }

        public TestListAdapter(ArrayList<String> data) {
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
            holder.mTitle.setText(JsonHandler.toDatePrettyPrint(mData.get(position)));
            TestInfo.getGlobalScore(test);
            holder.mResult.setText("Result: " + TestInfo.getGlobalScore(test) + " / " + test.size() * 2 * TestInfo.SCORE_TOTAL);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }


}
