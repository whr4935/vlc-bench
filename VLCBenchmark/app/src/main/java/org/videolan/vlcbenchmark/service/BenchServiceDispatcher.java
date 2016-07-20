package org.videolan.vlcbenchmark.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by penava_b on 18/07/16.
 */
public class BenchServiceDispatcher extends BroadcastReceiver {

    private List<BenchServiceListener> listeners = new ArrayList<BenchServiceListener>(1);

    public BenchServiceDispatcher(BenchServiceListener listener) {
        super();
        listeners.add(listener);
    }

    public BenchServiceDispatcher() {
        super();
    }

    public void add(BenchServiceListener listener) {
        listeners.add(listener);
    }

    public void remove(BenchServiceListener listener) {
        listeners.remove(listener);
    }

    public void startService(Context context, int numberOfTests) {
        if (numberOfTests <= 0)
            throw new IllegalArgumentException("BenchService cannot be started using a loop-number inferior of 1");
        Intent intent = new Intent(context, BenchService.class);
        intent.setAction(BenchService.ACTION_LAUNCH_SERVICE);
        intent.putExtra(BenchService.NUMBER_OF_TESTS, numberOfTests);
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(BenchService.DOWNLOAD_FAILURE));
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(BenchService.CHECKSUM_FAILURE));
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(BenchService.FILE_TESTED_STATUS));
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(BenchService.TEST_PASSED_STATUS));
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(BenchService.DONE_STATUS));
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(BenchService.PERCENT_STATUS));
        context.startService(intent);
    }

    public void stopService(Context context)
    {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
        Intent intent = new Intent(context, BenchService.class);
        context.stopService(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case BenchService.CHECKSUM_FAILURE:
                for (BenchServiceListener listener : listeners)
                    listener.checkSumFailed((Exception) intent.getSerializableExtra(BenchService.EXTRA_CONTENT));
                break;
            case BenchService.DOWNLOAD_FAILURE:
                for (BenchServiceListener listener : listeners)
                    listener.downloadFailed((Exception) intent.getSerializableExtra(BenchService.EXTRA_CONTENT));
                break;
            case BenchService.DONE_STATUS:
                for (BenchServiceListener listener : listeners)
                    listener.doneReceived((Score)intent.getSerializableExtra(BenchService.EXTRA_CONTENT));
                listeners.clear();
                break;
            case BenchService.TEST_PASSED_STATUS:
                for (BenchServiceListener listener : listeners)
                    listener.testPassed(intent.getStringExtra(BenchService.EXTRA_CONTENT));
                break;
            case BenchService.FILE_TESTED_STATUS:
                for (BenchServiceListener listener : listeners)
                    listener.filePassed((TestInfo) intent.getSerializableExtra(BenchService.EXTRA_CONTENT));
                break;
            case BenchService.PERCENT_STATUS:
                for (BenchServiceListener listener : listeners)
                    listener.updatePercent(intent.getDoubleExtra(BenchService.EXTRA_CONTENT, 0.0));
                break;
            default:
                return;
        }
    }
}
