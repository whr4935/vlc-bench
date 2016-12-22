/*****************************************************************************
 * BenchServiceDispatcher.java
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

package org.videolan.vlcbenchmark.service;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by penava_b on 18/07/16.
 */
public class BenchServiceDispatcher extends Handler {

    private List<BenchServiceListener> listeners = new ArrayList<BenchServiceListener>(1);
    private Activity initContext = null;

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

    public void startService(Activity context, Intent intent) {
        if (initContext != null)
            throw new RuntimeException("Can't create two BenchService from the same BenchServiceDispatcher, stop the previous one first");
        initContext = context;
        context.startService(intent);
        if (serviceConnection != null)
            return ;
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder binder) {
                if (binder == null)
                    return;
                ((BenchService.Binder) binder).sendData(BenchServiceDispatcher.this);
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

    public static final long NO_BITRATE = -1L;

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case BenchService.FAILURE:
                for (BenchServiceListener listener : listeners)
                    listener.failure(FAILURE_STATES.values()[msg.arg1], (Exception) msg.obj);
                break;
            case BenchService.DONE_STATUS:
                stopService();
                for (BenchServiceListener listener : listeners)
                    listener.doneReceived((List<MediaInfo>) msg.obj);
                break;
            case BenchService.PERCENT_STATUS:
                for (BenchServiceListener listener : listeners)
                    listener.updatePercent((double) msg.obj, NO_BITRATE);
                break;
            case BenchService.PERCENT_STATUS_BITRATE:
                Pair<Double, Long> percentAndBitRate = (Pair<Double, Long>) msg.obj;
                for (BenchServiceListener listener : listeners)
                    listener.updatePercent(percentAndBitRate.first, percentAndBitRate.second);
                break;
            case BenchService.STEP_FINISHED:
                for (BenchServiceListener listener : listeners)
                    listener.stepFinished((String)msg.obj);
                break;
            case BenchService.FILE_CHECK:
                for (BenchServiceListener listener : listeners)
                    listener.setDownloaded((boolean)msg.obj);
                break;
            case BenchService.DONE_DOWNLOAD:
                for (BenchServiceListener listener : listeners) {
                    listener.doneDownload();
                }
            default:
                break;
        }
    }
}
