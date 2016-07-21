package org.videolan.vlcbenchmark;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


public class ScreenshotActivity extends AppCompatActivity {

    /* Constants */
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
            | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private static final int REQUEST_SCREENSHOT = 666; //tmp

    private int mWidth;
    private int mHeight;
    private int mDensity;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private Intent mResultData;
    private int mResultCode;
    private ImageReader mImageReader;
    private Handler mHandler;
    private Intent mScreenshotIntent;

    /* static variables */
    private static int sScreenshotCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screenshot);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (sScreenshotCount >= 3)
            sScreenshotCount = 0;
        getScreenInfo();
        mProjectionManager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mImageReader != null) {
            try {
                mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);
            } catch (IllegalArgumentException e) {
                Log.e("VLCBenchmark", "Failed to destroy screenshot callback");
                return;
            }
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
    }

    /**
     * Takes the screenshot
     */
    public void onNewIntent(Intent intent) {
        boolean state;
        state = intent.getBooleanExtra(Boolean.class.getName(), false);
        if (state){
            if (mProjectionManager != null) {
                mScreenshotIntent = mProjectionManager.createScreenCaptureIntent();
                startActivityForResult(mScreenshotIntent, REQUEST_SCREENSHOT);
            }
        }
    }

    /**
     * Gets the results from activities launched from this activity.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SCREENSHOT) {
            if (resultCode == RESULT_OK) {
                mResultData = data;
                mResultCode = resultCode;
                mMediaProjection = mProjectionManager.getMediaProjection(mResultCode, mResultData);
                if (mMediaProjection == null) {
                    return;
                }
                mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
                if (mImageReader == null) {
                    return;
                }
                mVirtualDisplay =
                        mMediaProjection.createVirtualDisplay("testScreenshot", mWidth,
                                mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS,
                                mImageReader.getSurface(), null, mHandler);
                if (mVirtualDisplay == null) {
                    return;
                }

                try {
                    mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);
                } catch (IllegalArgumentException e) {
                    Log.e("VLC", "Failed to create screenshot callback");
                    return;
                }

                //mMediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
            }
        }

    }

    /**
     * Gets the screen information.
     * <p>
     * To be used when changes on screen
     * Resume app or change orientation
     */
    public void getScreenInfo() {
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        mWidth = size.x;
        mHeight = size.y;

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDensity = metrics.densityDpi;
    }

    /**
     * Opens an activity to visualize the screenshot
     *
     * @param imageFile File imageFile ...;
     *                  <p>
     *                  For testing only.
     */
    public void openScreenshot(File imageFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(imageFile);
        intent.setDataAndType(uri, "image/*");
        startActivity(intent);
    }

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            FileOutputStream outputStream = null;
            Image image = null;
            Bitmap bitmap;
            try {
                image = mImageReader.acquireLatestImage();
            } catch (IllegalArgumentException e) {
                Log.e("VLC", "Failed to acquire latest image for screenshot.");
            }
            if (image != null) {
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * mWidth;

                // create bitmap
                bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight,
                        Bitmap.Config.ARGB_8888);
                if (bitmap != null) {
                    bitmap.copyPixelsFromBuffer(buffer);

                    // write bitmap to a file
                    File imageFile = new File(getExternalFilesDir(null), "Screenshot_" + sScreenshotCount + ".jpg");
                    if (imageFile != null) {
                        try {
                            outputStream = new FileOutputStream(imageFile);
                        } catch (IOException e) {
                            // Do Something
                        }
                        if (outputStream != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        }

                        bitmap.recycle();
                        image.close();
                        if (outputStream != null) {
                            try {
                                outputStream.flush();
                                outputStream.close();
                            } catch (IOException e) {
                                // Do Something
                            }
                        }
                        sScreenshotCount += 1;
                    }
                }
                try {
                    mImageReader.setOnImageAvailableListener(null, null);
                } catch (IllegalArgumentException e) {
                    // Do Something
                }
                mVirtualDisplay.release();
            }
        }
    }

}

