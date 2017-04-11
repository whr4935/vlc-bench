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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
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

    private void redirectToVlcStore() {
        Intent viewIntent;
        viewIntent = new Intent("android.intent.action.VIEW",
                Uri.parse("https://play.google.com/store/apps/details?id=org.videolan.vlc&hl=en"));
        startActivity(viewIntent);
    }

    private void startTestDialog(int testNumber) {
        if (!mListener.checkSignature()) {
            Log.e("VLCBench", "Could not find VLC Media Player");
            new AlertDialog.Builder(getContext())
                    .setTitle("Missing VLC")
                    .setMessage("You need the VLC Media Player to start a benchmark\nPlease install it to continue")
                    .setNeutralButton("Cancel", null)
                    .setNegativeButton("Continue", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            redirectToVlcStore();
                        }
                    })
                    .show();
            return;
        }
        if (!mListener.checkVlcVersion()) {
            Log.e("VLCBench", "Outdated version of VLC Media Player detected");
            new AlertDialog.Builder(getContext())
                    .setTitle("Outdated VLC")
                    .setMessage("You need the latest version of VLC Media Player to start a benchmark\nPlease update it to continue")
                    .setNeutralButton("Cancel", null)
                    .setNegativeButton("Continue", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            redirectToVlcStore();
                        }
                    })
                    .show();
            return;
        }
        if (mListener.launchTests(testNumber)) {
            CurrentTestFragment fragment = new CurrentTestFragment();
            fragment.setCancelable(false);
            fragment.show(getFragmentManager(), "Current test");
        } else {
            Log.e("VLCBench", "Failed to start the benchmark");
            new AlertDialog.Builder(getContext())
                    .setTitle("Oups ...")
                    .setMessage("There was an unexpected problem when starting the benchmark")
                    .setNeutralButton("Ok", null)
                    .show();
        }
    }

    private void checkForTestStart(final int testNumber) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getContext().registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float)scale * 100f;

        if (batteryPct <= 50f && !isCharging) {
            new AlertDialog.Builder(getContext())
                    .setTitle("WARNING")
                    .setMessage("You only have " + batteryPct + " % of battery charge left, you should plug your phone")
                    .setNeutralButton("Cancel", null)
                    .setNegativeButton("Continue", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startTestDialog(testNumber);
                        }
                    })
                    .show();
        } else {
            startTestDialog(testNumber);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main_page, container, false);

        FloatingActionButton oneTest = (FloatingActionButton) view.findViewById(R.id.fab_test_x1);
        oneTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkForTestStart(1);
            }
        });

        FloatingActionButton threeTest = (FloatingActionButton) view.findViewById(R.id.fab_test_x3);
        threeTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkForTestStart(3);
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
        boolean launchTests(int number);
        boolean checkSignature();
        boolean checkVlcVersion();
    }

}
