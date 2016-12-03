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
                        Log.e("VLCBench", "Called onItemClick");
                        TextView text = (TextView) view.findViewById(R.id.test_name);
                        String fileName = text.getText() + ".txt";
                        ArrayList<TestInfo>[] testInfoList = new ArrayList[]{new ArrayList<TestInfo>()};
                        testInfoList[0] = JsonHandler.load(fileName);
                        if (testInfoList[0] != null) {
                            Intent intent = new Intent(getActivity(), ResultPage.class);
                            intent.putExtra("resultsTest", testInfoList);
                            intent.putExtra("soft", getSoftScore(testInfoList[0]));
                            intent.putExtra("hard", getHardScore(testInfoList[0]));
                            startActivityForResult(intent, getResources().getInteger(R.integer.requestResults));
                        }
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        Log.e("VLCBench", "Called onLongItemClick");
                    }
                }
                ));
        return view;
    }

    private double getHardScore(ArrayList<TestInfo> testInfo) {
        double hardware = 0;
        for (TestInfo info : testInfo) {
            hardware += info.getHardware();
            Log.e("VLCBench", "name = " + info.getName());
        }
        hardware /= testInfo.size();
        return hardware;
    }

    private double getSoftScore(ArrayList<TestInfo> testInfo) {
        double software = 0;
        for (TestInfo info : testInfo) {
            Log.e("VLCBench", "name = " + info.getName());
            software += info.getSoftware();
        }
        software /= testInfo.size();
        return software;
    }

    public class TestListAdapter extends RecyclerView.Adapter<MainPageResultListFragment.TestListAdapter.ViewHolder> {

        ArrayList<String> mData;

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView mTextView;

            public ViewHolder(View view) {
                super(view);
                mTextView = (TextView) view.findViewById(R.id.test_name);
            }

        }

        public TestListAdapter(ArrayList<String> data) {
            mData = data;
        }

        @Override
        public MainPageResultListFragment.TestListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            android.view.View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.test_list_rows, parent, false);
            return new MainPageResultListFragment.TestListAdapter.ViewHolder(view);
        }



        @Override
        public void onBindViewHolder(MainPageResultListFragment.TestListAdapter.ViewHolder holder, int position) {
            holder.mTextView.setText(mData.get(position));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }


}
