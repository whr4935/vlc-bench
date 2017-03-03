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
    int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurfaceView = new BenchGLSurfaceView(this);
        this.setContentView(mSurfaceView);

        ViewTreeObserver vto = mSurfaceView.getViewTreeObserver();

        /* Calls onGlobalLayout when a modification is performed
        *  on the layout. That allows us to know when the informations
        *  we seek have been instanciated.*/
        /* This is not the cleanest way to do it */
        // TODO find cleaner way to get gl values
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (count == 1) {
                    Intent retIntent = new Intent();
                    retIntent.putExtra("gl_renderer", mSurfaceView.getGlRenderer());
                    retIntent.putExtra("gl_vendor", mSurfaceView.getGlVendor());
                    retIntent.putExtra("gl_version", mSurfaceView.getGlVersion());
                    setResult(RESULT_OK, retIntent);
                    finish();
                }
                count += 1;
            }
        });
    }

}
