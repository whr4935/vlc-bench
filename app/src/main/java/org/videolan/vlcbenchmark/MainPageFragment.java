/*
 *****************************************************************************
 * MainPageFragment.java
 *****************************************************************************
 * Copyright © 2016-2017 VLC authors and VideoLAN
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
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.videolan.vlcbenchmark.tools.CheckFilesTask;
import org.videolan.vlcbenchmark.tools.DialogInstance;
import org.videolan.vlcbenchmark.tools.DownloadFilesTask;
import org.videolan.vlcbenchmark.tools.FormatStr;
import org.videolan.vlcbenchmark.tools.MediaInfo;
import org.videolan.vlcbenchmark.tools.ProgressSaver;
import org.videolan.vlcbenchmark.tools.TestInfo;
import org.videolan.vlcbenchmark.tools.VLCProxy;

import java.util.List;

import kotlin.Unit;

/**
 * Fragment where the user can start the benchmark
 * When started VLCBenchmark will check:
 * - If the user has VLC
 * - If the user has the right version
 * - Check the battery level
 * - Check file integrity / download if missing
 * - Check the presence of a previous benchmark that was interrupted
 */
public class MainPageFragment extends Fragment {

    private final static String TAG = MainPageFragment.class.getName();

    IMainPageFragment mListener;
    private AsyncTask task;
    private int mTestNumber = 0;
    ProgressDialog progressDialog;

    public MainPageFragment() {}

    private void redirectToVlcStore() {
        Intent viewIntent;
        viewIntent = new Intent("android.intent.action.VIEW",
                Uri.parse("https://play.google.com/store/apps/details?id=org.videolan.vlc&hl=en"));
        startActivity(viewIntent);
    }

    private void checkAndroidVersion() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            new AlertDialog.Builder(getContext())
                    .setTitle(getResources().getString(R.string.dialog_title_error))
                    .setMessage(getResources().getString(R.string.dialog_text_android_10))
                    .setNegativeButton(getResources().getString(R.string.dialog_btn_ok), null)
                    .show();
            return;
        }
        checkForVLC();
    }

    private void checkForVLC() {
        if (getActivity() == null) {
            Log.e(TAG, "checkForVLC: null context");
            return;
        }
        Boolean vlcSignature = VLCProxy.Companion.checkSignature(getActivity());
        Boolean vlcVersion = VLCProxy.Companion.checkVlcVersion(getActivity());
        if (!vlcSignature || !vlcVersion) {
            if (!vlcSignature) {
                Log.e(TAG, "Could not find VLC Media Player");
            } else {
                Log.e(TAG, "Outdated VLC Media Player");
            }
            new AlertDialog.Builder(getContext())
                    .setTitle(getResources().getString(R.string.dialog_title_missing_vlc))
                    .setMessage(String.format(getResources().getString(R.string.dialog_text_missing_vlc), BuildConfig.VLC_VERSION))
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
        checkBattery();
    }

    private void checkBattery() {
        if (getContext() == null) {
            Log.e(TAG, "checkBattery: null context");
            return;
        }
        IntentFilter intentfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getContext().registerReceiver(null, intentfilter);
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
                    .setMessage(String.format(getResources().getString(R.string.dialog_text_battery_warning), Math.round(batteryPct)))
                    .setNeutralButton(getResources().getString(R.string.dialog_btn_cancel), null)
                    .setNegativeButton(getResources().getString(R.string.dialog_btn_continue), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            checkFiles();
                        }
                    })
                    .show();
        } else {
            checkFiles();
        }
    }

    private void checkFiles() {
        task = new CheckFilesTask(this);
        ((CheckFilesTask)task).execute();
    }

    public void onFilesChecked(long size) {
        if (size > 0) {
            if (getContext() == null) {
                Log.e(TAG, "onFilesChecked: null context");
                return;
            }
            new AlertDialog.Builder(getContext())
                    .setTitle(getResources().getString(R.string.dialog_title_warning))
                    .setMessage(String.format(getString(R.string.dialog_text_download_warning),
                            FormatStr.INSTANCE.byteSizeToString(getContext(), size)))
                    .setNeutralButton(getResources().getString(R.string.dialog_btn_cancel), null)
                    .setNegativeButton(getResources().getString(R.string.dialog_btn_continue), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            downloadFiles();
                        }
                    })
                    .show();
        } else {
            downloadFiles();
        }
    }

    private void downloadFiles() {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        task = new DownloadFilesTask(this);
        ((DownloadFilesTask)task).execute();
        progressDialog = new ProgressDialog(); // tmp
        progressDialog.setCancelable(false);
        progressDialog.setTitle(R.string.dialog_title_downloading);
        progressDialog.setCancelCallback(this::cancelDownload);
        progressDialog.show(getFragmentManager(), "Download dialog");
    }

    public void onFilesDownloaded(List<MediaInfo> files) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        if (mTestNumber == 1 || mTestNumber == 3) {
            mListener.setBenchmarkFiles(files);
            int testNumber = mTestNumber;
            mTestNumber = 0;
            checkForPreviousBench(testNumber, files.size());
        } else {
            Log.e(TAG, "onFilesDownloaded: invalid test number: " + mTestNumber);
            mTestNumber = 0;
        }
    }

    private void checkForPreviousBench(final int numberOfTests, final int fileNumber) {
        final List<TestInfo>[] previousTest = ProgressSaver.load(getContext());
        if (previousTest == null) {
            startTestWarning(numberOfTests);
        } else {
            int loopIndex = 0;
            // finding the benchmark loop
            if (numberOfTests == 3) {
                while( loopIndex < numberOfTests) {
                    if (previousTest[loopIndex].size() == 0)
                        break;
                    loopIndex += 1;
                }
                loopIndex -= 1;
            }
            new AlertDialog.Builder(getContext())
                    .setTitle(getResources().getString(R.string.dialog_title_previous_bench))
                    .setMessage(String.format(getResources().
                            getString(R.string.dialog_text_previous_bench),
                            previousTest[loopIndex].size(), fileNumber))
                    .setNeutralButton(getResources().getString(R.string.dialog_btn_discard), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startTestWarning(numberOfTests);
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.dialog_btn_continue), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mListener.startProgressDialog();
                            mListener.launchTests(0, previousTest);
                        }
                    })
                    .show();
        }
    }

    private void startTestWarning(final int testNumber) {
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.dialog_title_warning))
                .setMessage(getString(R.string.dialog_text_no_touch_warning))
                .setNeutralButton(getString(R.string.dialog_btn_cancel), null)
                .setNegativeButton(getString(R.string.dialog_btn_continue), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.startProgressDialog();
                        mListener.launchTests(testNumber, null);
                    }
                })
                .show();
    }

    public void updateProgress(double progress, String progressText, String sampleName) {
        if (progressDialog != null) {
            progressDialog.updateProgress(progress, progressText, sampleName);
        }
    }

    private void fillDeviceLayout(View view) {
        TextView model = view.findViewById(R.id.specs_model_text);
        TextView android = view.findViewById(R.id.specs_android_text);
        TextView cpu = view.findViewById(R.id.specs_cpu_text);
        TextView cpuspeed = view.findViewById(R.id.specs_cpuspeed_text);
        TextView memory = view.findViewById(R.id.specs_memory_text);
        TextView resolution = view.findViewById(R.id.specs_resolution_text);
        TextView freeSpace = view.findViewById(R.id.specs_free_space_text);

        model.setText(Build.MODEL);
        android.setText(Build.VERSION.RELEASE);
        cpu.setText(SystemPropertiesProxy.getCpuModel());
        cpuspeed.setText(SystemPropertiesProxy.getCpuMinFreq() + " - " + SystemPropertiesProxy.getCpuMaxFreq());
        memory.setText(SystemPropertiesProxy.getRamTotal());
        if (getActivity() != null)
            resolution.setText(SystemPropertiesProxy.getResolution(getActivity()));
        else
            Log.e(TAG, "fillDeviceLayout: null activity");
        if (getContext() != null)
            freeSpace.setText(FormatStr.INSTANCE.byteSizeToString(getContext(),
                    SystemPropertiesProxy.getFreeSpace()));
        else
            Log.e(TAG, "fillDeviceLayout: null context");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_page, container, false);

        FloatingActionButton oneTest = view.findViewById(R.id.fab_test_x1);
        oneTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTestNumber = 1;
                checkAndroidVersion();
            }
        });

        FloatingActionButton threeTest = view.findViewById(R.id.fab_test_x3);
        threeTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTestNumber = 3;
                checkAndroidVersion();
            }
        });

        ScrollView specs = view.findViewById(R.id.specs_scrollview);
        specs.setFocusable(false);

        ScrollView explanations = view.findViewById(R.id.test_explanation_scrollview);
        explanations.setFocusable(false);

        fillDeviceLayout(view);

        return view;
    }

    private Unit cancelDownload() {
        if (task != null) {
            task.cancel(true);
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        }
        return Unit.INSTANCE;
    }

    // the isVisible condition is supposed to stop an IllegalStateException occurring in some
    // cases when dismissing the dialog from DownloadFilesTask.onPostExecute method.
    // Waiting for user validation as I cannot reproduce.
    public void dismissDialog() {
        if (progressDialog != null && progressDialog.isVisible()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        if (task != null) {
            task.cancel(true);
        }
        super.onDestroy();
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
        void startProgressDialog();
        void setBenchmarkFiles(List<MediaInfo> files);
        void launchTests(int number, List<TestInfo>[] previousBench);
        void dismissDialog();
    }

}
