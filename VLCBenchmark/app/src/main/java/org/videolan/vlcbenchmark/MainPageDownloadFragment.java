package org.videolan.vlcbenchmark;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.videolan.vlcbenchmark.service.BenchService;


/**
 * A simple {@link Fragment} subclass.
 */

public class MainPageDownloadFragment extends Fragment {

    public MainPageDownloadFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_page_download, container, false);
        FloatingActionButton dlButton = (FloatingActionButton) view.findViewById(R.id.fab_download);
        dlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), BenchService.class);
                intent.putExtra("action", 1);
                getActivity().startService(intent);
                CurrentTestFragment fragment = new CurrentTestFragment(); // tmp
                fragment.setCancelable(false);
                fragment.show(getFragmentManager(), "Download dialog");
            }
        });
        return view;
    }
}
