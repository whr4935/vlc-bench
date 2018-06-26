/*****************************************************************************
 * BenchServiceDispatcher.java
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
import android.util.Log;
import android.util.Pair;

import org.videolan.vlcbenchmark.tools.DialogInstance;

import java.util.List;

/**
 * Created by penava_b on 18/07/16.
 */
public class BenchServiceDispatcher extends Handler {

    private Activity initContext = null;
    private BenchServiceListener listener;
    private ServiceConnection serviceConnection;
    private static BenchServiceDispatcher instance;

    public BenchServiceDispatcher() {
        super(Looper.getMainLooper());
    }

    public static BenchServiceDispatcher getInstance() {
        if (instance == null) {
            instance = new BenchServiceDispatcher();
        }
        return instance;
    }

    public boolean isStarted() {
        return (instance != null && initContext != null);
    }

    public void startService(Activity context) {
        if (initContext != null) {
            Log.w("BenchServiceDispatcher", "Can't create two BenchService from the same BenchServiceDispatcher, stop the previous one first");
            return;
        }
        initContext = context;
        listener = (BenchServiceListener) context;
        Intent intent = new Intent(context, BenchService.class);
        intent.putExtra("action", ServiceActions.SERVICE_CONNECT);
        context.startService(intent);
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
                listener.failure(FAILURE_STATES.values()[msg.arg1], (Exception) msg.obj);
                break;
            case BenchService.DONE_STATUS:
                listener.doneReceived((List<MediaInfo>) msg.obj);
                break;
            case BenchService.PERCENT_STATUS:
                listener.updatePercent((double) msg.obj, NO_BITRATE);
                break;
            case BenchService.PERCENT_STATUS_BITRATE:
                Pair<Double, Long> percentAndBitRate = (Pair<Double, Long>) msg.obj;
                listener.updatePercent(percentAndBitRate.first, percentAndBitRate.second);
                break;
            case BenchService.FILE_CHECK:
                listener.setFilesChecked((boolean)msg.obj);
                break;
            case BenchService.DONE_DOWNLOAD:
                listener.setFilesDownloaded((boolean)msg.obj);
                break;
            case BenchService.DIALOG:
                listener.displayDialog((DialogInstance)msg.obj);
                break;
            default:
                break;
        }
    }
}
