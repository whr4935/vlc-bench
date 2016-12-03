package org.videolan.vlcbenchmark;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainPageFragment extends Fragment {

    IMainPageFragment mListener;

    public MainPageFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main_page, container, false);

        FloatingActionButton oneTest = (FloatingActionButton) view.findViewById(R.id.fab_test_x1);
        oneTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("VLCBench", "Pressed on one test button");
                CurrentTestFragment fragment = new CurrentTestFragment(); // tmp
                fragment.setCancelable(false);
                fragment.show(getFragmentManager(), "Current test"); // tmp
//                mListener.launchTests(1);
            }
        });

        FloatingActionButton threeTest = (FloatingActionButton) view.findViewById(R.id.fab_test_x3);
        threeTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("VLCBench", "Pressed on three test button");
            }
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IMainPageFragment) {
            mListener = (IMainPageFragment) context;
        } else {
            throw new RuntimeException(context.toString());
        }
    }

    public interface IMainPageFragment {
        void launchTests(int number);
    }

}
