/*****************************************************************************
 * ScreenshotValidator.java
 *****************************************************************************
 * Copyright © 2011-2014 VLC authors and VideoLAN
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

    /* Error margin on color difference percentage */
    /* It is set quite high for now to balance differences between devices */
    //TODO find source of differences / better algorithm
    private static final double MAX_SCREENSHOT_COLOR_DIFFERENCE_PERCENT = 20.0;

    /* number of blocks in image width */
    private final static int wbNumber = 6;
    /* number of blocks in image height */
    private final static int hbNumber = 5;

    /**
     * Takes an int with RGB values encoded int and decodes it
     * @param rgbInt int encoded with RGB values
     * @return arrat with RGB values decoded
     */
    private static int[] getRGB(int rgbInt) {
        int rgb[] = new int[3];

        rgb[0] = (rgbInt >> 16) & 0xFF;
        rgb[1] = (rgbInt >>  8) & 0xFF;
        rgb[2] = (rgbInt      ) & 0xFF;

        return rgb;
    }

    /**
     * Calculates the averages RGB for a block
     * @param pix image pixel two dimensional array
     * @param width image width
     * @param height image height
     * @param iWidth block index in width
     * @param iHeight block index in height
     * @return RGB array for the average color of the block
     */
    private static int[] getBlockColorValue(int[][] pix, int width, int height, int iWidth, int iHeight) {
        int wbSize = width / wbNumber;
        int hbSize = height / hbNumber;

        int red = 0;
        int green = 0;
        int blue = 0;

        for (int inc = iHeight * hbSize ; inc < (iHeight + 1) * hbSize ; inc++) {
            for (int i = iWidth * wbSize ; i < (iWidth + 1) * wbSize ; i++) {
                red += Color.red(pix[inc][i]);
                green += Color.green(pix[inc][i]);
                blue += Color.blue(pix[inc][i]);
            }
        }

        red /= wbSize * hbSize;
        green /= wbSize * hbSize;
        blue /= wbSize * hbSize;

        return new int[]{red, green, blue};
    }

    /**
     * Converts each RGB encoded ints in the array to an RGB array
     * @param array of RGB encoded int
     * @return array of RGB decoded array
     */
    private static int[][] convertRGBintArray(int[] array) {
        int[][] refArray = new int[array.length][3];

        for (int i = 0 ; i < array.length ; i++) {
            refArray[i] = getRGB(array[i]);
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
        double valuePercent = colorValue / 255d * 100d;
        double refPercent = reference / 255d * 100d;
        double color = Math.abs(valuePercent / refPercent * 100d);
        return Math.abs(100d - color);
    }

    /**
     * Computes the RGB difference between the screenshot block and the reference block
     * @param colorValue screenshot block RGB array
     * @param reference reference block RGB array
     * @return RGB block average difference
     */
    private static double compareBlockColorValues(int[] colorValue, int[] reference) {
        double red = getDiffPercentage(colorValue[0], reference[0]);
        double green = getDiffPercentage(colorValue[1], reference[1]);
        double blue = getDiffPercentage(colorValue[2], reference[2]);
        return (red + green + blue) / 3d;
    }

    /**
     * Compares screenshot blocks and reference blocks and computes the average difference
     * @param colorValues screenshot array of block's RGB arrays
     * @param reference reference array block's RGB arrays
     * @return average color difference between screenshot and reference
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
        diff /= colorValues.length;

        return diff;
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

        return colorValues;
    }

    /**
     * Computes the percentage of color difference between screenshot and reference
     * and validates screenshot or not
     * @param filepath screenshot filepath
     * @param reference RGB encoded int array of reference from the sample
     * @return color difference percentage between screenshot and reference
     */
    public static boolean validateScreenshot(String filepath, int[] reference) {
        try {
            int[][] colorValues = getColorValues(filepath);
            int[][] colorReference = convertRGBintArray(reference);
            double diff = compareImageBlocks(colorValues, colorReference);
            return diff < MAX_SCREENSHOT_COLOR_DIFFERENCE_PERCENT;
        } catch (RuntimeException e) {
            Log.e(TAG, "getValidityPercent has failed: ");
            e.printStackTrace();
            return false;
        }
    }

}
