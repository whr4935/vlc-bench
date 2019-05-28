package org.videolan.vlcbenchmark.tools

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object AppExecutors  {

    private val mDiskIO = Executors.newSingleThreadExecutor()
    private val mNetworkIO = Executors.newFixedThreadPool(3)
    private val mMainThread= MainThreadExecutor()


    fun diskIO(): Executor {
        return mDiskIO
    }

    fun networkIO(): Executor {
        return mNetworkIO
    }

    fun mainThread(): Executor {
        return mMainThread
    }

    private class MainThreadExecutor : Executor {
        private val mainThreadHandler = Handler(Looper.getMainLooper())

        override fun execute(command: Runnable) {
            mainThreadHandler.post(command)
        }
    }
}
