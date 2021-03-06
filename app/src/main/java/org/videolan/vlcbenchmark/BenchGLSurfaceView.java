/*
 *****************************************************************************
 * BenchGLSurfaceView.java
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

import android.content.Context;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class allows us to get a handle on the GL context
 * and get gpu information
 */
public class BenchGLSurfaceView extends GLSurfaceView {

    private Context glActivityContext;

    /* Gpu information*/
    private String glRenderer;
    private String glVendor;
    private String glVersion;
    private String glExtension;

    public BenchGLSurfaceView(Context context) {
        super(context);
        glActivityContext = context;

        setEGLContextClientVersion(2);

        BenchGLRenderer mRenderer = new BenchGLRenderer();

        setRenderer(mRenderer);
    }

    private class BenchGLRenderer implements GLSurfaceView.Renderer {
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            glRenderer = gl.glGetString(GL10.GL_RENDERER);
            glVendor = gl.glGetString(GL10.GL_VENDOR);
            glVersion = gl.glGetString(GL10.GL_VERSION);
            glExtension = gl.glGetString(GL10.GL_EXTENSIONS);
            ((BenchGLActivity)glActivityContext).sendGlInfo();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
        }

        @Override
        public void onDrawFrame(GL10 gl) {
        }
    }

    public String getGlRenderer() {
        return glRenderer;
    }

    public String getGlVersion() {
        return glVersion;
    }

    public String getGlVendor() {
        return glVendor;
    }

    public String getGlExtension() { return glExtension; }
}
