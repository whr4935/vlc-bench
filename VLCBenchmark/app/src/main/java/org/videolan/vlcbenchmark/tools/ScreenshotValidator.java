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
import android.support.annotation.Size;
import android.support.v4.graphics.ColorUtils;

/**
 * Created by penava_b on 02/08/16.
 */
public class ScreenshotValidator {

    /**
     * Implementation of the CIE94 algorithm to find the difference between two color.
     *
     * @param leftLab  a color converted to the <a href="https://en.wikipedia.org/wiki/Lab_color_space">Lab color space</a> format.
     * @param rightLab an other color converted to the <a href="https://en.wikipedia.org/wiki/Lab_color_space">Lab color space</a>  format.
     * @return a value between 0 and 255.
     * 0 when the two given colors are identical.
     * The higher the value, the bigger the difference between the two given colors
     * @see <a href="https://en.wikipedia.org/wiki/Color_difference#CIE94">wikipedia CIE94</a>
     */
    public static double CIE94ColorDifference(@Size(min = 3) double[] leftLab, @Size(min = 3) double[] rightLab) {
        if (leftLab == null || rightLab == null)
            return 255f;
        double deltaLStar = leftLab[0] - rightLab[0];
        double cOneStar = sqrt(pow2(leftLab[1]) + pow2(rightLab[2]));
        double cTwoStar = sqrt(pow2(leftLab[2]) + pow2(rightLab[1]));
        double deltaCStar = cOneStar - cTwoStar;
        double deltaA = leftLab[1] - rightLab[1];
        double deltaB = leftLab[2] - rightLab[2];
        double deltaHStarAb = sqrt(pow2(deltaA) + pow2(deltaB) - pow2(deltaCStar));
        double Sl = 1;
        double Sc = 1 + 0.045 * cOneStar;
        double Sh = 1 + 0.015 * cTwoStar;
        return sqrt(pow2(deltaLStar) + pow2(deltaCStar / Sc) + pow2(deltaHStarAb / Sh));
    }

    private static double pow2(double val) {
        return val * val;
    }

    private static double sqrt(double val) {
        return Math.sqrt(val);
    }

    public static double[] rgbToLab(int red, int green, int blue) {
        double[] lab = new double[3];
        ColorUtils.RGBToLAB(red, green, blue, lab);
        return lab;
    }

    public static double[] rgbToLab(int color) {
        double[] lab = new double[3];
        ColorUtils.RGBToLAB(Color.red(color), Color.green(color), Color.blue(color), lab);
        return lab;
    }

    public static int averageColorForImage(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        if (bitmap == null)
            throw new RuntimeException();
        long red = 0;
        long green = 0;
        long blue = 0;
        int[] pixels = new int[bitmap.getHeight() * bitmap.getWidth()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < pixels.length; i++) {
            int tmpColor = pixels[i];
            red += Color.red(tmpColor);
            green += Color.green(tmpColor);
            blue += Color.blue(tmpColor);
        }
        red /= pixels.length;
        green /= pixels.length;
        blue /= pixels.length;
        return Color.rgb((int) red, (int) green, (int) blue);
    }

    public static double getValidityPercent(String filePath, Integer color) {
        try {
            return CIE94ColorDifference(rgbToLab(averageColorForImage(filePath)), rgbToLab(color)) * 100f / 255f;
        } catch (RuntimeException e) {
            return 100.0;
        }
    }
}
