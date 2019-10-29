package org.videolan.vlcbenchmark.tools

import android.os.AsyncTask
import android.util.Log
import androidx.fragment.app.Fragment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class CopyFilesTask(_fragment: Fragment): AsyncTask<String, Pair<Long, Long>, Boolean>() {

    private val fragment: Fragment = _fragment
    private lateinit var oldDirectory: String
    private lateinit var newDirectory: String
    private var totalCopySize: Long = 0L

    private fun transferMountpoints(oldDirectory: String, newDirectory: String, _downloadSize: Long): Long {
        val oldDir = File(oldDirectory)
        val files = oldDir.listFiles()
        var downloadSize = _downloadSize
        StorageManager.checkFolderLocation(newDirectory)
        if (files != null) {
            for (f in files) {
                if (this.isCancelled) {
                    Log.w(TAG, "transferMountpoints: Task was cancelled")
                    return -1
                }
                if (f.isDirectory) {
                    downloadSize = transferMountpoints("$oldDirectory/${f.name}",
                            "$newDirectory/${f.name}", downloadSize)
                } else {
                    val newFile = File("$newDirectory/${f.name}")
                    try {
                        val size = 2 * 1024
                        try {
                            val fin = FileInputStream(f)
                            val fout = FileOutputStream(newFile)
                            val buffer = ByteArray(size)
                            var read = fin.read(buffer, 0, size)
                            var fromTime = System.nanoTime()
                            var passedTime: Long
                            var passedSize: Long = 0
                            var fileDownloadSize: Long = 0
                            while (read != -1) {
                                if (this.isCancelled) {
                                    Log.w(TAG, "transferMountpoints: Task was cancelled")
                                    return -1
                                }
                                passedTime = System.nanoTime()
                                fout.write(buffer, 0, read)
                                passedSize += read.toLong()
                                if (passedTime - fromTime >= 1_000_000_000) {
                                    fileDownloadSize += passedSize
                                    onProgressUpdate(Pair(downloadSize + fileDownloadSize, passedSize))
                                    fromTime = System.nanoTime()
                                    passedSize = 0
                                }
                                read = fin.read(buffer, 0, size)
                            }
                            fin.close()
                            fout.close()
                            val oldChecksum = StorageManager.getFileChecksum(f)
                            val newChecksum = StorageManager.getFileChecksum(newFile)
                            if (oldChecksum != newChecksum) {
                                Log.e(TAG, "copyFile: Copy has failed: file checkums differ")
                                return -1
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "transferMountpoints: $e")
                            return -1
                        }
                        if (!StorageManager.copyFile(f, "$newDirectory/${f.name}")) {
                            Log.e(TAG, "transferMountpoints: Failed to move file: ${newFile.absolutePath}")
                            return -1
                        }
                        downloadSize += f.length()
                        onProgressUpdate(Pair(downloadSize, 0L))
                    } catch (e: Exception) {
                        Log.e(TAG, "transferMountpoints: $e")
                        return -1
                    }
                }
            }
        } else {
            Log.i(TAG, "transferMountpoints: There are no files to transfert")
        }
        return downloadSize
    }

    override fun doInBackground(vararg params: String?): Boolean {
        if (params.size != 2) {
            return false
        }
        this.oldDirectory = params[0]!!
        this.newDirectory = params[1]!!
        totalCopySize = StorageManager.getDirectoryMemoryUsage(this.oldDirectory)
        return (transferMountpoints(this.oldDirectory, this.newDirectory, 0) != -1L)
    }

    override fun onProgressUpdate(vararg values: Pair<Long, Long>?) {
        super.onProgressUpdate(*values)
        if (values.size == 1 && fragment is IOnFilesCopied) {
            (fragment as IOnFilesCopied).updateProgress(values[0]!!.first, values[0]!!.second)
        }
    }

    override fun onCancelled() {
        if (fragment is IOnFilesCopied) {
            StorageManager.deleteDirectory(this.newDirectory)
            (fragment as IOnFilesCopied).onFileCopied(isCancelled, this.oldDirectory)
        }
        super.onCancelled()
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        if (fragment is IOnFilesCopied) {
            val listener = fragment as IOnFilesCopied
            if (result!!) {
                StorageManager.deleteDirectory(this.oldDirectory)
                listener.onFileCopied(true, this.newDirectory)
            } else {
                StorageManager.deleteDirectory(this.newDirectory)
                listener.onFileCopied(false, this.oldDirectory)
            }
        }
    }

    interface IOnFilesCopied {
        fun onFileCopied(success: Boolean, newValue: String)
        fun updateProgress(downloadSize: Long, downloadSpeed: Long)
    }

    companion object {
        @Suppress("UNUSED")
        private val TAG = this::class.java.name
    }
}