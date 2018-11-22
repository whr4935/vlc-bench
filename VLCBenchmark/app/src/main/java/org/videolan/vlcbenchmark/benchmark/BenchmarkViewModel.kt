package org.videolan.vlcbenchmark.benchmark

import androidx.lifecycle.ViewModel
import org.videolan.vlcbenchmark.tools.MediaInfo
import org.videolan.vlcbenchmark.tools.TestInfo

class BenchmarkViewModel : ViewModel() {

    companion object {
        @Suppress("UNUSED")
        private val TAG = this::class.java.name
    }

    lateinit var testResults: Array<List<TestInfo>>
    lateinit var testFiles: List<MediaInfo>
    lateinit var testIndex: TestTypes
    lateinit var lastTestInfo: TestInfo

    var fileIndex: Int = 0
    var loopNumber: Int = 0
    var loopTotal: Int = 1
    var running: Boolean = false


}