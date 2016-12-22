/*****************************************************************************
 * MainPageFragment.java
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
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
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
                CurrentTestFragment fragment = new CurrentTestFragment(); // tmp
                fragment.setCancelable(false);
                fragment.show(getFragmentManager(), "Current test"); // tmp
                mListener.launchTests(1);
            }
        });

        FloatingActionButton threeTest = (FloatingActionButton) view.findViewById(R.id.fab_test_x3);
        threeTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CurrentTestFragment fragment = new CurrentTestFragment();
                fragment.setCancelable(false);
                fragment.show(getFragmentManager(), "Current test");
                mListener.launchTests(3);
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
