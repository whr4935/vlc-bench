/*****************************************************************************
 * MainPageDownloadFragment.java
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.videolan.vlcbenchmark.service.BenchService;
import org.videolan.vlcbenchmark.service.BenchServiceDispatcher;


/**
 * A simple {@link Fragment} subclass.
 */

public class MainPageDownloadFragment extends Fragment {

    IMainPageDownloadFragment mListener;

    public MainPageDownloadFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_page_download, container, false);
        FloatingActionButton dlButton = (FloatingActionButton) view.findViewById(R.id.fab_download);
        dlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener.getHasChecked()) {
                    Intent intent = new Intent(getActivity(), BenchService.class);
                    intent.putExtra("action", 1);
                    getActivity().startService(intent);
                    CurrentTestFragment fragment = new CurrentTestFragment(); // tmp
                    fragment.setCancelable(false);
                    fragment.show(getFragmentManager(), "Download dialog");
                } else {
                    Intent intent = new Intent(getActivity(), BenchService.class);
                    intent.putExtra("action", ServiceActions.SERVICE_CHECKFILES);
                    intent.putExtra("context",BenchService.FileCheckContext.download);
                    getActivity().startService(intent);
                }
            }
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IMainPageDownloadFragment) {
            mListener = (IMainPageDownloadFragment) context;
        } else {
            throw new RuntimeException(context.toString());
        }
    }

    public interface IMainPageDownloadFragment {
        boolean getHasChecked();
    }

}
