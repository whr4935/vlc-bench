package org.videolan.vlcbenchmark;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 */
public class CurrentTestFragment extends DialogFragment {

    TestView mListener;

    public CurrentTestFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.e("VLCBench", "CurrentTestFragment onCreateView");
        View view = inflater.inflate(R.layout.fragment_current_test, container, false);
        Button cancel = (Button) view.findViewById(R.id.current_test_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof TestView) {
            mListener = (TestView) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onStart() {
        Log.e("VLCBench", "CurrentTestFragment onStart()");
        mListener.setDialogFragment(this);
        super.onStart();
    }

    @Override
    public void onDetach() {
        Log.e("VLCBench", "CurrentTestFragment onDetach()");
        mListener.setDialogFragment(null);
        super.onDetach();
    }

    public void setText(String text) {
        TextView textView = (TextView) this.getView().findViewById(R.id.test_text);
        textView.setText(text);
    }

    public interface TestView {
        void setDialogFragment(CurrentTestFragment fragment);
    }
}
