package org.videolan.vlcbenchmark.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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
        BenchService.startService(context, this, numberOfTests);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case BenchService.CHECKSUM_FAILURE:
                for (BenchServiceListener listener : listeners)
                    listener.checkSumFailed((Exception) intent.getParcelableExtra(BenchService.EXTRA_CONTENT));
                break;
            case BenchService.DOWNLOAD_FAILURE:
                for (BenchServiceListener listener : listeners)
                    listener.downloadFailed((Exception) intent.getParcelableExtra(BenchService.EXTRA_CONTENT));
                break;
            case BenchService.DONE_STATUS:
                for (BenchServiceListener listener : listeners)
                    listener.doneReceived(intent.getDoubleExtra(BenchService.EXTRA_CONTENT, 0.0));
                listeners.clear();
                break;
            case BenchService.TEST_PASSED_STATUS:
                for (BenchServiceListener listener : listeners)
                    listener.testPassed(intent.getStringExtra(BenchService.EXTRA_CONTENT));
                break;
            case BenchService.FILE_TESTED_STATUS:
                for (BenchServiceListener listener : listeners)
                    listener.filePassed(intent.getStringExtra(BenchService.EXTRA_CONTENT));
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
