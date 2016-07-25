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
    private Context initContext = null;

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
        if (initContext != null || serviceConnection != null)
            throw new RuntimeException("Can't create two BenchService from the same BenchServiceDispatcher, stop the previous one first");
        initContext = context;
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

    public void stopService() {
        if (serviceConnection == null || initContext == null)
            return ;
        initContext.unbindService(serviceConnection);
        serviceConnection = null;
        initContext.stopService(new Intent(initContext, BenchService.class));
        initContext = null;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case BenchService.FAILURE_STATE:
                stopService();
                for (BenchServiceListener listener : listeners)
                    listener.failure(FAILURE_STATES.values()[msg.arg1], (Exception) msg.obj);
                break;
            case BenchService.DONE_STATUS:
                stopService();
                for (BenchServiceListener listener : listeners)
                    listener.doneReceived((Score) msg.obj);
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
