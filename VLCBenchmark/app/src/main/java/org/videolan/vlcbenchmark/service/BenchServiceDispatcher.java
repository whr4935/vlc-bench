package org.videolan.vlcbenchmark.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by penava_b on 18/07/16.
 */
public class BenchServiceDispatcher extends Handler {

    private List<BenchServiceListener> listeners = new ArrayList<BenchServiceListener>(1);

    public BenchServiceDispatcher(BenchServiceListener listener) {
        super(Looper.getMainLooper());
        listeners.add(listener);
    }

    public BenchServiceDispatcher() {
        super(Looper.getMainLooper());
    }

    public void add(BenchServiceListener listener) {
        listeners.add(listener);
    }

    public void remove(BenchServiceListener listener) {
        listeners.remove(listener);
    }

    private ServiceConnection serviceConnection;

    public void startService(Context context, final int numberOfTests) {
        if (numberOfTests <= 0)
            throw new IllegalArgumentException("BenchService cannot be started using a loop-number inferior of 1");
        Intent intent = new Intent(context, BenchService.class);
        context.startService(intent);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder binder) {
                if (binder == null)
                    return;
                ((BenchService.Binder) binder).sendData(numberOfTests, BenchServiceDispatcher.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        };
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void stopService(Context context) {
        context.unbindService(serviceConnection);
        context.stopService(new Intent(context, BenchService.class));
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case BenchService.DOWNLOAD_FAILURE:
                for (BenchServiceListener listener : listeners)
                    listener.downloadFailed((Exception) msg.obj);
                break;
            case BenchService.CHECKSUM_FAILURE:
                for (BenchServiceListener listener : listeners)
                    listener.checkSumFailed((Exception) msg.obj);
                break;
            case BenchService.DONE_STATUS:
                for (BenchServiceListener listener : listeners)
                    listener.doneReceived((Score) msg.obj);
                listeners.clear();
                break;
            case BenchService.TEST_PASSED_STATUS:
                for (BenchServiceListener listener : listeners)
                    listener.testPassed((String) msg.obj);
                break;
            case BenchService.FILE_TESTED_STATUS:
                for (BenchServiceListener listener : listeners)
                    listener.filePassed((TestInfo) msg.obj);
                break;
            case BenchService.PERCENT_STATUS:
                for (BenchServiceListener listener : listeners)
                    listener.updatePercent((double) msg.obj);
                break;
            default:
                return;
        }
    }
}
