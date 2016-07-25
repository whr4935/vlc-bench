package org.videolan.vlcbenchmark.service;

import android.content.Intent;

import java.util.concurrent.CountDownLatch;

/**
 * Created by penava_b on 25/07/16.
 */
public class VlcPlaybackData {

    private int returnCode = 42;
    private CountDownLatch sleeper = new CountDownLatch(1);
    private Intent launcher;

    public VlcPlaybackData(Intent launcher) {
        this.launcher = launcher;
    }

    public int getCode() {
        return 42;
    }

    public void finished(int returnCode) {
        this.returnCode = returnCode;
        sleeper.countDown();
    }

    public void await() throws InterruptedException {
        sleeper.await();
    }

    public Intent getLauncher() {
        return launcher;
    }
}
