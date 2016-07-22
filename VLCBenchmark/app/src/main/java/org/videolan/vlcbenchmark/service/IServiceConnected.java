package org.videolan.vlcbenchmark.service;

import android.content.ServiceConnection;

/**
 * Created by penava_b on 22/07/16.
 */
public interface IServiceConnected {
    public void onConnect(ServiceConnection connection, Runnable finishCallback);
}
