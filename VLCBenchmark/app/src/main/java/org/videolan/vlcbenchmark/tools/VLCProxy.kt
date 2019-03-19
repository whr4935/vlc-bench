package org.videolan.vlcbenchmark.tools

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import org.videolan.vlcbenchmark.BuildConfig
import org.videolan.vlcbenchmark.R

/**
 * Handles all checks on VLC-Android that have to be done before starting a Benchmark:
 * checking if vlc is installed, has the right signature, and version
 */
class VLCProxy {

    companion object {
        private fun checkVersion(str1: String, str2: String): Int {
            val vals1 = str1.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val vals2 = str2.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var i = 0

            while (i < vals1.size && i < vals2.size && vals1[i] == vals2[i]) {
                i++
            }

            if (i < vals1.size && i < vals2.size) {
                // Removes potential "-RCx" at the end of the version string
                val cmp1 = vals1[i].split("-")[0]
                val cmp2 = vals2[i].split("-")[0]

                val diff = Integer.valueOf(cmp1).compareTo(Integer.valueOf(cmp2))
                return Integer.signum(diff)
            }

            return Integer.signum(vals1.size - vals2.size)
        }

        fun checkVlcVersion(context: Context): Boolean {
            if (!BuildConfig.DEBUG) {
                try { // tmp during the VLCBenchmark alpha, using the vlc beta
                    if (checkVersion(context.packageManager
                                    .getPackageInfo(context.getString(R.string.vlc_package_name), 0)
                                    .versionName, BuildConfig.VLC_VERSION) < 0) {
                        return false
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    return false
                }

            }
            return true
        }

        /**
         * Tool method to check if VLC's signature and ours match.
         *
         * @return true if VLC's signature matches our else false
         */
        fun checkSignature(context:Context): Boolean {
            val benchPackageName = context.packageName
            val vlcSignature: Int
            val benchSignature: Int
            val sigsVlc: Array<Signature>
            val sigs: Array<Signature>


            /* Getting application signature*/
            try {
                sigs = context.packageManager.getPackageInfo(benchPackageName, PackageManager.GET_SIGNATURES).signatures
            } catch (e: PackageManager.NameNotFoundException) {
                return false
            }

            /* Checking to see if there is any signature */
            if (sigs != null && sigs.isNotEmpty())
                benchSignature = sigs[0].hashCode()
            else
                return false

            /* Getting vlc's signature */
            try {
                sigsVlc = context.packageManager.getPackageInfo(context.getString(R.string.vlc_package_name), PackageManager.GET_SIGNATURES).signatures
            } catch (e: PackageManager.NameNotFoundException) {
                return false
            }

            /* checking to see if there is are any signatures */
            if (sigsVlc != null && sigsVlc.isNotEmpty())
                vlcSignature = sigsVlc[0].hashCode()
            else
                return false

            return benchSignature == vlcSignature
        }
    }


}