/*
 *****************************************************************************
 * ScreenshotValidator.java
 *****************************************************************************
 * Copyright © 2016-2018 VLC authors and VideoLAN
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

package org.videolan.vlcbenchmark.tools;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import androidx.core.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * ScreenshotValidator compares the screenshots taken during the benchmark
 * with the reference for the sample.
 * Each screenshot is divided into wbNumber blocks in width and hbNumber blocks in height
 * On each blocks the average color is computed and compared to the reference to
 * get a percentage of difference
 * Depending on that error margin percentage, the screenshot is validated or not
 */
public class ScreenshotValidator {

    private final static String TAG = ScreenshotValidator.class.getName();

    /* Error margin on color difference cumulative percentage */
    /* here set fairly high as to handle frame difference due to vlc imprecision */
    private static final double MAX_SCREENSHOT_COLOR_DIFFERENCE_PERCENT = 55.0;

    /* number of blocks in image width */
    private final static int wbNumber = 6;
    /* number of blocks in image height */
    private final static int hbNumber = 5;

    /**
     * Takes an int with YCbCr values encoded int and decodes it
     * @param ycbcrInt int encoded with YCbCr values
     * @return array with YCbCr values decoded
     */
    private static int[] getYCbCr(int ycbcrInt) {
        int[] ycbcrArray = new int[3];

        ycbcrArray[0] = (ycbcrInt >> 16) & 0xFF;
        ycbcrArray[1] = (ycbcrInt >>  8) & 0xFF;
        ycbcrArray[2] = (ycbcrInt      ) & 0xFF;

        return ycbcrArray;
    }

    /**
     * Calculates the averages YCbCr from an RGB block
     * @param pix image pixel two dimensional array
     * @param width image width
     * @param height image height
     * @param iWidth block index in width
     * @param iHeight block index in height
     * @return YCbCr array for the average color of the block
     */
    private static int[] getBlockColorValue(int[][] pix, int width, int height, int iWidth, int iHeight) {
        int wbSize = width / wbNumber;
        int hbSize = height / hbNumber;

        double totalY = 0;
        double totalCb = 0;
        double totalCr = 0;

        for (int inc = iHeight * hbSize ; inc < (iHeight + 1) * hbSize ; inc++) {
            for (int i = iWidth * wbSize ; i < (iWidth + 1) * wbSize ; i++) {
                double r = Color.red(pix[inc][i]);
                double g = Color.green(pix[inc][i]);
                double b = Color.blue(pix[inc][i]);

                double y = 0.299 * r + 0.587 * g + 0.114 * b;
                double cb = 128 - 0.168736 * r - 0.331264 * g + 0.5 * b;
                double cr = 128 + 0.5 * r - 0.418688 * g - 0.081312 * b;

                totalY += y;
                totalCb += cb;
                totalCr += cr;
            }
        }

        totalY /= wbSize * hbSize;
        totalCb /= wbSize * hbSize;
        totalCr /= wbSize * hbSize;

        return new int[]{(int)totalY, (int)totalCb, (int)totalCr};
    }

    /**
     * Converts each RGB encoded ints in the array to an RGB array
     * @param array of RGB encoded int
     * @return array of RGB decoded array
     */
    private static int[][] convertYcbcrIntArray(int[] array) {
        int[][] refArray = new int[array.length][3];

        for (int i = 0 ; i < array.length ; i++) {
            refArray[i] = getYCbCr(array[i]);
        }

        return refArray;
    }

    /**
     * Computes the color (R, G, or B) difference between the screenshot block and the reference block
     * @param colorValue screenshot block color average
     * @param reference reference block color average
     * @return color difference percentage
     */
    private static double getDiffPercentage(int colorValue, int reference) {
        return Math.abs(reference - colorValue) / 255d * 100d;
    }

    /**
     * Computes the RGB difference between the screenshot block and the reference block
     * @param colorValue screenshot block RGB array
     * @param reference reference block RGB array
     * @return RGB block average difference
     */
    private static double compareBlockColorValues(int[] colorValue, int[] reference) {
        double y = getDiffPercentage(colorValue[0], reference[0]);
        double cb = getDiffPercentage(colorValue[1], reference[1]);
        double cr = getDiffPercentage(colorValue[2], reference[2]);
        return (y + cb + cr) / 3d;
    }

    /**
     * Compares screenshot blocks and reference blocks and computes the cumulative difference
     * @param colorValues screenshot array of block's RGB arrays
     * @param reference reference array block's RGB arrays
     * @return cumulative color difference between screenshot and reference
     */
    private static double compareImageBlocks(int[][] colorValues, int[][] reference) {
        if (colorValues.length != reference.length) {
            Log.e(TAG, "The color arrays are not of the same size");
            throw new RuntimeException();
        }

        double diff = 0;
        for (int i = 0 ; i < colorValues.length ; i++) {
            diff += compareBlockColorValues(colorValues[i], reference[i]);
        }

        return diff;
    }

    /**
     *  Checks for presence of vertical transparent ligns on the sides of the screenshot,
     *  and removes them if present.
     *  Some screenshots can have transparent side if the devices a deactivated notch, or
     *  sometimes it can be caused by the navigation bar at the bottom.
     *  Failing to remove them at the time the screenshot is taken, they can be removed before
     *  analysis.
     * @param filepath screenshot path
     * @param bitmap screenshot bitmap
     * @return a bitmap cropped of it's lignes or the original if their aren't any or if there was
     *  a problem.
     */
    private static Bitmap cropUselessTransparentSides(String filepath, Bitmap bitmap) {
        int[] pix = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pix, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int[][] pixTab = new int[bitmap.getHeight()][bitmap.getWidth()];
        for (int i = 0 ; i < bitmap.getHeight() ; i++) {
            for (int inc = 0 ; inc < bitmap.getWidth() ; inc++) {
                pixTab[i][inc] = pix[inc + i * bitmap.getWidth()];
            }
        }

        /* Determine where the transparent vertical lines are */
        int minWidth = 0;
        while (minWidth < bitmap.getWidth() && pixTab[0][minWidth] == 0)
            minWidth++;
        int maxWidth = bitmap.getWidth() - 1;
        while(maxWidth > 0 && pixTab[0][maxWidth] == 0)
            maxWidth--;
        if (minWidth == bitmap.getWidth() || (minWidth == 0 && maxWidth == bitmap.getWidth() - 1))
            return bitmap;

        /* Replacing the old screenshot with the new bitmap*/
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, minWidth, 0, maxWidth - minWidth, bitmap.getHeight());
        try {
            File image = new File(filepath);
            if (!image.delete()) {
                Log.e(TAG, "cropUselessTransparentSides: Failed to delete old file");
                return bitmap;
            }
            File newFile = new File(filepath);
            FileOutputStream outputStream = new FileOutputStream(newFile);
            newBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "cropUselessTransparentSides: Failed to save cropped image: " + e.toString());
            return bitmap;
        }
        bitmap.recycle();
        return newBitmap;
    }

    /**
     * Opens screenshot and computes color averages for each blocks
     * @param filepath screenshot filepath
     * @return screenshot's array of block's RGB array
     */
    private static int[][] getColorValues(String filepath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(filepath, options);
        if (bitmap == null) {
            Log.e(TAG, "Failed to get bitmap from screenshot");
            throw new RuntimeException();
        }
        bitmap = cropUselessTransparentSides(filepath, bitmap);

        int[] pix = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pix, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int[][] pixTab = new int[bitmap.getHeight()][bitmap.getWidth()];
        for (int i = 0 ; i < bitmap.getHeight() ; i++) {
            for (int inc = 0 ; inc < bitmap.getWidth() ; inc++) {
                pixTab[i][inc] = pix[inc + i * bitmap.getWidth()];
            }
        }

        int[][] colorValues = new int[wbNumber * hbNumber][3];
        for (int i = 0 ; i < wbNumber ; i++) {
            for (int inc = 0 ; inc < hbNumber ; inc++) {
                colorValues[i * hbNumber + inc] =
                        getBlockColorValue(pixTab, bitmap.getWidth(), bitmap.getHeight(), i, inc);
            }
        }
        bitmap.recycle();
        return colorValues;
    }

    /**
     * Computes the percentage of color difference between screenshot and reference
     * and validates screenshot or not
     * @param filepath screenshot filepath
     * @param reference RGB encoded int array of reference from the sample
     * @return color difference percentage between screenshot and reference
     */
    public static Pair<Boolean, Integer> validateScreenshot(String filepath, int[] reference) {
        try {
            int[][] colorValues = getColorValues(filepath);
            int[][] colorReference = convertYcbcrIntArray(reference);
            double diff = compareImageBlocks(colorValues, colorReference);
            Log.i(TAG, "validateScreenshot: screenshots difference percentage: " + diff);
            return new Pair<>(diff < MAX_SCREENSHOT_COLOR_DIFFERENCE_PERCENT, (int)Math.floor(diff));
        } catch (RuntimeException e) {
            Log.e(TAG, "getValidityPercent has failed: ");
            e.printStackTrace();
            return new Pair<>(false, -1);
        }
    }

}
