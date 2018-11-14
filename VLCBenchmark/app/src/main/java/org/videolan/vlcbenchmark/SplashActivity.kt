/*
 *****************************************************************************
 * DividerItemDecoration.java
 *****************************************************************************
 * Copyright © 2018 VLC authors and VideoLAN
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

package org.videolan.vlcbenchmark

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat

class SplashActivity : AppCompatActivity() {

    companion object {
        @Suppress("UNUSED")
        private val TAG = this::class.java.name

        private const val SHARED_PREFERENCE_WARNING = "org.videolan.vlc.gui.video.benchmark.WARNING"
        private const val PERMISSION_REQUEST = 1
    }


    private var mPermissions: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        val hasWarned = sharedPref.getBoolean(SHARED_PREFERENCE_WARNING, false)

        if (!hasWarned) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_hello)
                    .setMessage(R.string.dialog_text_initial_warning)
                    .setNeutralButton(R.string.dialog_btn_ok, { _, _ ->
                        requestPermissions()
                    })
                    .show()
            val editor = sharedPref.edit()
            editor.putBoolean(SHARED_PREFERENCE_WARNING, true)
            editor.apply()
        } else {
            requestPermissions()
        }

    }

    private fun startMainPage() {
        val intent = Intent(this, MainPage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (mPermissions.size != 0) {
            val arrayPermissions: Array<String> = mPermissions.toTypedArray()
            ActivityCompat.requestPermissions(this, arrayPermissions, PERMISSION_REQUEST)
        } else {
            startMainPage()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST && grantResults.isNotEmpty()) {
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    mPermissions.remove(permissions[i])
                }
            }
            if (mPermissions.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_error)
                    .setMessage(R.string.dialog_text_error_permission)
                    .setNeutralButton(R.string.dialog_btn_ok, {_, _ ->
                        finishAndRemoveTask()
                    })
                    .show()
            } else {
               startMainPage()
            }
        }
    }
}
