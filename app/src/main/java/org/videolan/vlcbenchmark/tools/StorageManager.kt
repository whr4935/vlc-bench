package org.videolan.vlcbenchmark.tools

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import org.videolan.vlcbenchmark.BuildConfig
import java.io.*
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.util.*
import kotlin.collections.HashMap

object StorageManager {

    private val TAG = this::class.java.name

    const val jsonFolder = "jsonFolder"
    const val mediaFolder = "mediaFolder"
    const val screenshotFolder = "screenshotFolder"
    val tmpScreenshotDir = Environment.getExternalStorageDirectory().absolutePath + "/vlcBenchmarkScreenshotDir"
    val tmpStackTraceFile = Environment.getExternalStorageDirectory().absolutePath + "/vlcBenchmarkVlcStackTrace.txt"

    //Devices mountpoints management
    private val typeWL = Arrays.asList("vfat", "exfat", "sdcardfs", "fuse", "ntfs", "fat32", "ext3", "ext4", "esdfs")
    private val typeBL = listOf("tmpfs")
    val EXTERNAL_PUBLIC_DIRECTORY: String = Environment.getExternalStorageDirectory().path
    private val mountWL = arrayOf("/mnt", "/Removable", "/storage")
    private val mountBL = arrayOf(
            EXTERNAL_PUBLIC_DIRECTORY, "/mnt/secure", "/mnt/shell", "/mnt/asec", "/mnt/nand",
            "/mnt/runtime", "/mnt/obb", "/mnt/media_rw/extSdCard", "/mnt/media_rw/sdcard",
            "/storage/emulated", "/var/run/arc")
    private val deviceWL = arrayOf("/dev/block/vold", "/dev/fuse", "/mnt/media_rw", "passthrough")

    val externalStorageDirectories: List<String>
            get() {
                var bufReader: BufferedReader? = null
                val list = ArrayList<String>()
                try {
                    bufReader = BufferedReader(FileReader("/proc/mounts"))
                    var line = bufReader.readLine()
                    while (line != null) {
                        val tokens = StringTokenizer(line, " ")
                        val device = tokens.nextToken()
                        val mountpoint = tokens.nextToken().replace("\\\\040".toRegex(), " ")
                        val type = if (tokens.hasMoreTokens()) tokens.nextToken() else null
                        if (list.contains(mountpoint) || typeBL.contains(type) || mountBL.any { mountpoint.startsWith(it) }) {
                            line = bufReader.readLine()
                            continue
                        }
                        if (deviceWL.any { device.startsWith(it) } && (typeWL.contains(type) || mountWL.any { mountpoint.startsWith(it) })) {
                            val position = list.indexOfLast { it.endsWith(getFilenameFromPath(mountpoint)) }
                            if (position > -1) list.removeAt(position)
                            list.add(mountpoint)
                        }
                        line = bufReader.readLine()
                    }
                } catch (ignored: IOException) {
                } finally {
                    bufReader?.close    ()
                }
                list.remove(EXTERNAL_PUBLIC_DIRECTORY)
                return list
            }
    val hasExternalSdCard = externalStorageDirectories.isNotEmpty()

    var baseDir : String = ""
    var directoryListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    var mountpoint: String? = null
    var directory: String? = null

    fun getFilenameFromPath(path: String?) : String {
        var path: String? = path ?: return ""
        var index = path!!.lastIndexOf('/')
        if (index == path.length - 1) {
            path = path.substring(0, index)
            index = path.lastIndexOf('/')
        }
        return if (index > -1)
            path.substring(index + 1)
        else
            path
    }

    fun checkNewMountpointFreeSpace(oldDirectory: String, newDirectory: String): Boolean {
        val oldDirSize = getDirectoryMemoryUsage(oldDirectory)
        if (oldDirSize == 0L)
            return true
        val newDirSpace = File(newDirectory).freeSpace
        if (oldDirSize > newDirSpace) {
            Log.e(TAG, "checkNewMountpointFreeSpace: There isn't enough space on "
                    + "the new mountpoint to move the benchmark files")
            return false
        }
        return true
    }

    fun getDirectoryMemoryUsage(directory: String): Long{
        val dir = File(directory)
        val files = dir.listFiles()
        var size = 0L
        if (files != null) {
            for (f in files) {
                if (f.isDirectory) {
                    size += getDirectoryMemoryUsage("$directory/${f.name}")
                } else {
                    size += f.length()
                }
            }
        }
        return size
    }

    fun copyFile(file: File, newPath: String): Boolean {
        val newFile = File(newPath)
        val size = 2 * 1024
        try {
            val fin = FileInputStream(file)
            val fout = FileOutputStream(newFile)
            val buffer = ByteArray(size)
            var read = fin.read(buffer, 0, size)
            while (read != -1) {
                fout.write(buffer, 0, read)
                read = fin.read(buffer, 0, size)
            }
            fin.close()
            fout.close()
            val oldChecksum = getFileChecksum(file)
            val newChecksum = getFileChecksum(newFile)
            if (oldChecksum != newChecksum) {
                Log.e(TAG, "copyFile: Copy has failed: file checkums differ")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "copyFile: $e")
            return false
        }
        return true
    }

    private fun getLastModifiedFromJsonResults(path: String): Long {
        val jsonStorage = File("$path$jsonFolder")
        val files = jsonStorage.listFiles()
        if (files == null)
            return -1
        val fileList: ArrayList<File> = files.toCollection(ArrayList())
        if (fileList.size == 0)
            return -1
        fileList.sortByDescending { it.lastModified() }
        return fileList[0].lastModified()
    }

    // This will parse the filesystem to find any VLCBenchmark directory
    private fun checkoutForPreviousLocation(): String? {
        val locationList = ArrayList<String>()
        locationList.add(EXTERNAL_PUBLIC_DIRECTORY)
        locationList.addAll(externalStorageDirectories)
        val benchDirHash = HashMap<String, Long>()
        val locToDelete = ArrayList<String>()
        for (dir in locationList) {
            val path = "$dir$baseDir"
            if (File(path).exists()) {
                val last = getLastModifiedFromJsonResults(path)
                if (last != -1L) {
                    benchDirHash[path] = last
                } else {
                    locToDelete.add(path)
                }
            }
        }
        if (benchDirHash.size == 0)
            return null
        // Order benchmark folders by date of their last benchmark result
        val benchDirList = benchDirHash.toList().sortedByDescending { (_, value) -> value }
        for (i in 1 until benchDirList.size) {
            locToDelete.add(benchDirList[i].first)
        }
        for (dir in locToDelete) {
            deleteDirectory(dir)
        }
        return benchDirList[0].first.replace(baseDir, "")
    }

    fun setStoragePreference(context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        baseDir = "/Android/data/${BuildConfig.APPLICATION_ID}/VLCBenchmark/"
        mountpoint = sharedPreferences.getString("storage_dir", "unset")
        if (mountpoint == "unset") {
            mountpoint = checkoutForPreviousLocation()
        }
        if (mountpoint == null) {
            mountpoint = EXTERNAL_PUBLIC_DIRECTORY
        }
        directory = mountpoint + baseDir
        Environment.getExternalStorageDirectory()

        //Creates the app directory if doesn't exist
        context.getExternalFilesDir(null)

        Log.w(TAG, "setStoragePreference: ${context.filesDir.absolutePath}")
        if (directoryListener != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(directoryListener)
        }
        directoryListener = SharedPreferences.OnSharedPreferenceChangeListener { _sharedPreferences, key ->
            if (key == "storage_dir") {
                Log.w(TAG, "setStoragePreference: OnpreferencceChange")
                mountpoint = _sharedPreferences.getString("storage_dir", EXTERNAL_PUBLIC_DIRECTORY)
                directory = mountpoint + baseDir
                //Creates the app directory if doesn't exist
                context.getExternalFilesDir(null)
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(directoryListener)
    }

    fun createDirectory(_name: String): Boolean {
        var name = _name
        if (name[name.length - 1] == '/')
            name = name.dropLast(1)
        val split = name.split("/")
        if (split.size > 2) {
            val parentPath = split.subList(0, split.size - 1).joinToString(separator = "/")
            val success = createDirectory(parentPath)
            if (!success) {
                Log.e(TAG, "createDirectory: Failed to create folder: $parentPath")
                return false
            }
        }
        val folderPath = split.joinToString(separator = "/")
        val folder = File(folderPath)
        if (folder.exists()) {
            return true
        }
        return folder.mkdir()
    }

    fun checkFolderLocation(name: String?): Boolean {
        if (name == null) {
            Log.w(TAG, "checkFolderLocation: name is null")
            return false
        }
        val folder = File(name)
        var ret = true
        if (!folder.exists()) {
            ret = folder.mkdir()
        }
        return ret
    }

    fun getInternalDirStr(name: String): String? {
        val folderStr = "$directory$name/"
        if (!createDirectory(folderStr)) {
            Log.e(TAG, "getInternalDirStr: Failed to create directory base directory")
            return null
        } else
            return folderStr
    }

    // Adding a nomedia to the media folder stops the vlc medialibrary from indexing the files
    // in the folder. Stops the benchmark from polluting the user's vlc library
    fun setNoMediaFile() {
        var path = getInternalDirStr(mediaFolder)
        if (path == null) {
            Log.e(TAG, "setNoMediaFile: path is null")
            return
        }
        path += ".nomedia"
        val nomediafile = File(path)
        try {
            if (!nomediafile.exists() && !nomediafile.createNewFile()) {
                Log.e(TAG, "setNoMediaFile: nomedia file was not created")
            }
        } catch (e: IOException) {
            Log.e(TAG, "setNoMediaFile: failed to create nomedia file: $e")
        }
    }

    fun delete(file: File) {
        Util.runInBackground {
            if (!file.delete()) {
                Log.e(TAG, "Failed to delete file: " + file.absolutePath)
            }
        }
    }

    fun getFileChecksum(file: File): String {
        val algorithm: MessageDigest
        var stream: FileInputStream? = null

        try {
            stream = FileInputStream(file)
            algorithm = MessageDigest.getInstance("SHA512")
            var buff = ByteArray(2048)
            var read = stream.read(buff, 0, 2048)
            while (read != -1) {
                algorithm.update(buff, 0, read)
                read = stream.read(buff, 0, 2048)
            }
            buff = algorithm.digest()
            val sb = StringBuilder()
            for (b in buff) {
                sb.append(Integer.toString((b.toInt() and 0xff) + 0x100.toInt(), 16).substring(1))
            }
            return sb.toString()
        } finally {
            stream?.close()
        }
    }

    /**
     * Check if a file correspond to a sha512 key.
     *
     * @param file     the file to compare
     * @param checksum the wished value of the transformation of the file by using the sha512 algorithm.
     * @return true if the result of sha512 transformation is identical to the given String in argument (checksum).
     * @throws GeneralSecurityException if the algorithm is not found.
     * @throws IOException              if an IO error occurs while we read the file.
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    fun checkFileSum(file: File, checksum: String): Boolean {
            return getFileChecksum(file) == checksum
    }

    fun deleteScreenshots() {
        Util.runInBackground {
            val dir = File(getInternalDirStr(screenshotFolder)!!)
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (!file.delete()) {
                        Log.e("VLCBench", "Failed to delete sample " + file.name)
                        break
                    }
                }
            }
        }
    }

    fun deleteDirectory(directory: String?) {
        if (directory == null) {
            Log.e(TAG, "deleteDirectory: directory is null")
            return
        }
        val dir = File(directory)
        val files = dir.listFiles()
        if (files != null) {
            for (f in files) {
                if (f.isDirectory)
                    deleteDirectory("$directory/${f.name}")
                else
                    f.delete()
            }
        }
        dir.delete()
    }

    fun checkFileSumAsync(file: File, checksum: String, listener: IOnFileCheckedListener) {
        AppExecutors.diskIO().execute {
            try {
                val fileChecked = checkFileSum(file, checksum)
                Util.runInUiThread { listener.onFileChecked(fileChecked) }
            } catch (e: GeneralSecurityException) {
                Util.runInUiThread { listener.onFileChecked(false) }
            } catch (e: IOException) {
                Util.runInUiThread { listener.onFileChecked(false) }
            }
        }
    }

    interface IOnFileCheckedListener {
        fun onFileChecked(valid: Boolean?)
    }
}