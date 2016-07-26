package values;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;

import org.videolan.vlcbenchmark.R;
import org.videolan.vlcbenchmark.ResultAdapter;
import org.videolan.vlcbenchmark.service.TestInfo;

import java.util.List;

public class GridFragment extends Fragment {
    private List<TestInfo> results;

    public GridFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            results = (List<TestInfo>)getArguments().getSerializable("results");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_grid, container, false);
        ListView gv = (ListView)v.findViewById(R.id.resultList);
        ResultAdapter resultAdapter = new ResultAdapter(getActivity());
        resultAdapter.setResults(results);
        gv.setAdapter(resultAdapter);
        gv.setFocusable(false);
        return v;
    }

}
