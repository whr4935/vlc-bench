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
                        TextView software = (TextView) view.findViewById(R.id.test_software);
                        TextView hardware = (TextView) view.findViewById(R.id.test_hardware);
                        ArrayList<TestInfo>[] testInfoList = new ArrayList[]{new ArrayList<TestInfo>()};
                        testInfoList[0] = JsonHandler.load(fileName);
                        if (testInfoList[0] != null) {
                            Intent intent = new Intent(getActivity(), ResultPage.class);
                            //intent.putExtra("resultsTest", testInfoList);
                            intent.putExtra("soft", software.getText());
                            intent.putExtra("hard", hardware.getText());
                            intent.putExtra("name", text.getText());
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

    public class TestListAdapter extends RecyclerView.Adapter<TestListAdapter.ViewHolder> {

        ArrayList<String> mData;

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView mTitle;
            public TextView mSoftware;
            public TextView mHardware;

            public ViewHolder(View view) {
                super(view);
                mTitle = (TextView) view.findViewById(R.id.test_name);
                mSoftware = (TextView) view.findViewById(R.id.test_software);
                mSoftware.setTextSize(13);
                mHardware = (TextView) view.findViewById(R.id.test_hardware);
                mHardware.setTextSize(13);
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
            holder.mTitle.setText(mData.get(position));
            holder.mSoftware.setText("Software: " + String.valueOf(getSoftScore(test)));
            holder.mHardware.setText("Hardware: " + String.valueOf(getHardScore(test)));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }


}
