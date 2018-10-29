/*
 *****************************************************************************
 * BenchGLActivity.java
 *****************************************************************************
 * Copyright © 2017 - 2018 VLC authors and VideoLAN
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

package org.videolan.vlcbenchmark;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewTreeObserver;

/**
 *  Class intented to instanciate an GLSurfaceView
 *  to be able to get gpu information
 */
public class BenchGLActivity extends AppCompatActivity {

    BenchGLSurfaceView mSurfaceView;

    public void sendGlInfo() {
        Intent retIntent = new Intent();
        retIntent.putExtra("gl_renderer", mSurfaceView.getGlRenderer());
        retIntent.putExtra("gl_vendor", mSurfaceView.getGlVendor());
        retIntent.putExtra("gl_version", mSurfaceView.getGlVersion());
        retIntent.putExtra("gl_extensions", mSurfaceView.getGlExtension());
        setResult(RESULT_OK, retIntent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurfaceView = new BenchGLSurfaceView(this);
        this.setContentView(mSurfaceView);
    }

}
