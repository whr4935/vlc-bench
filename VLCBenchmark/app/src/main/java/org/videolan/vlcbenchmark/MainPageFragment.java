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
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import org.videolan.vlcbenchmark.tools.DialogInstance;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainPageFragment extends Fragment {

    private final static String TAG = MainPageFragment.class.getName();

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
            Log.e(TAG, "Could not find VLC Media Player");
            new AlertDialog.Builder(getContext())
                    .setTitle(getResources().getString(R.string.dialog_title_missing_vlc))
                    .setMessage(getResources().getString(R.string.dialog_text_missing_vlc))
                    .setNeutralButton(getResources().getString(R.string.dialog_btn_cancel), null)
                    .setNegativeButton(getResources().getString(R.string.dialog_btn_continue), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            redirectToVlcStore();
                        }
                    })
                    .show();
            return;
        }
        if (!mListener.checkVlcVersion()) {
            Log.e(TAG, "Outdated version of VLC Media Player detected");
            new AlertDialog.Builder(getContext())
                    .setTitle(getResources().getString(R.string.dialog_title_outdated_vlc))
                    .setMessage(getResources().getString(R.string.dialog_text_outdated_vlc))
                    .setNeutralButton(getResources().getString(R.string.dialog_btn_cancel), null)
                    .setNegativeButton(getResources().getString(R.string.dialog_btn_continue), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            redirectToVlcStore();
                        }
                    })
                    .show();
            return;
        }
        mListener.startCurrentTestFragment();
        if (!mListener.launchTests(testNumber)) {
            Log.e(TAG, "Failed to start the benchmark");
            mListener.doneDownload();
            new AlertDialog.Builder(getContext())
                    .setTitle(getResources().getString(R.string.dialog_title_oups))
                    .setMessage(getResources().getString(R.string.dialog_text_oups))
                    .setNeutralButton(getResources().getString(R.string.dialog_btn_ok), null)
                    .show();
        }
    }

    private void checkForTestStart(final int testNumber) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getContext().registerReceiver(null, ifilter);
        if (batteryStatus == null) {
            Log.e(TAG, "checkForTestStart: battery intent is null");
            new DialogInstance(R.string.dialog_title_oups, R.string.dialog_text_oups).display(getActivity());
            return;
        }
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float)scale * 100f;

        if (batteryPct <= 50f && !isCharging) {
            new AlertDialog.Builder(getContext())
                    .setTitle(getResources().getString(R.string.dialog_title_warning))
                    .setMessage(String.format(getResources().getString(R.string.dialog_text_battery_warning), batteryPct))
                    .setNeutralButton(getResources().getString(R.string.dialog_btn_cancel), null)
                    .setNegativeButton(getResources().getString(R.string.dialog_btn_continue), new DialogInterface.OnClickListener() {
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

    private void fillDeviceLayout(View view) {
        TextView model = (TextView) view.findViewById(R.id.specs_model_text);
        TextView android = (TextView) view.findViewById(R.id.specs_android_text);
        TextView cpu = (TextView) view.findViewById(R.id.specs_cpu_text);
        TextView cpuspeed = (TextView) view.findViewById(R.id.specs_cpuspeed_text);
        TextView memory = (TextView) view.findViewById(R.id.specs_memory_text);

        model.setText(Build.MODEL);
        android.setText(Build.VERSION.RELEASE);
        cpu.setText(SystemPropertiesProxy.getCpuModel());
        cpuspeed.setText(SystemPropertiesProxy.getCpuMinFreq() + " - " + SystemPropertiesProxy.getCpuMaxFreq());
        memory.setText(SystemPropertiesProxy.getRamTotal());
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

        ScrollView specs = (ScrollView) view.findViewById(R.id.specs_scrollview);
        specs.setFocusable(false);

        ScrollView explanations = (ScrollView) view.findViewById(R.id.test_explanation_scrollview);
        explanations.setFocusable(false);

        fillDeviceLayout(view);

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
        void startCurrentTestFragment();
        boolean launchTests(int number);
        boolean checkSignature();
        boolean checkVlcVersion();
        void doneDownload();
    }

}
