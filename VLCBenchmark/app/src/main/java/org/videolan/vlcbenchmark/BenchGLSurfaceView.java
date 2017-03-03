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
    private final BenchGLRenderer mRenderer;

    /* Gpu information*/
    private String glRenderer;
    private String glVendor;
    private String glVersion;

    public BenchGLSurfaceView(Context context) {
        super(context);

        setEGLContextClientVersion(2);

        mRenderer = new BenchGLRenderer();

        setRenderer(mRenderer);
    }

    private class BenchGLRenderer implements GLSurfaceView.Renderer {
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            glRenderer = gl.glGetString(GL10.GL_RENDERER);
            glVendor = gl.glGetString(GL10.GL_VENDOR);
            glVersion = gl.glGetString(GL10.GL_VERSION);
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
}
