/*****************************************************************************
 * FormatStr.java
 *****************************************************************************
 * Copyright © 2017 VLC authors and VideoLAN
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

import android.util.Log;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatStr {

    private final static String TAG = FormatStr.class.getName();

    private static DecimalFormat decimalFormat_2 = new DecimalFormat("#.##");

    public static String format2Dec(double number) {
        return decimalFormat_2.format(number);
    }

    public static String getDateStr() {
        SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmssZ", Locale.getDefault());
        return format.format(System.currentTimeMillis());
    }

    public static Date strDateToDate(String str_date) {
        SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmssZ", Locale.getDefault());
        Date date = null;
        try {
            date = format.parse(str_date);
        } catch (ParseException e) {
            Log.e(TAG, "strDateToDate: Failed to parse date: " + str_date + "\n" + e.toString() );
        }
        return date;
    }

    /**
     * Converts date string used for filenames
     * to pretty readable date string ("dd MMM yyyy HH:mm:ss")
     * @param date_str filename date string
     * @return readable date string
     */
    public static String toDatePrettyPrint(String date_str) {
        SimpleDateFormat format = new SimpleDateFormat( "yyMMddHHmmssZ", Locale.getDefault());
        Date date = null;
        try {
            date = format.parse(date_str);
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse date: " + date_str + "\n" + e.toString());
        }
        format = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault());
        date_str = format.format(date);
        return date_str;
    }

    /**
     * Converts the readable date string ("dd MMM yyyy HH:mm:ss")
     * to the date string for filenames
     * @param date_str pretty readable date
     * @return underscore separated date string
     */
    public static String fromDatePrettyPrint(String date_str) {
        SimpleDateFormat format = new SimpleDateFormat( "dd MMM yyyy HH:mm:ss", Locale.getDefault());
        Date date = null;
        try {
            date = format.parse(date_str);
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse date: " + e.toString());
        }
        format = new SimpleDateFormat("yyMMddHHmmssZ", Locale.getDefault());
        return format.format(date);
    }

    public static String bitRateToString(long bitRate) {
        if (bitRate <= 0)
            return "0 bps";
        double powOf10 = Math.round(Math.log10(bitRate));

        if (powOf10 < 3)
            return format2Dec(bitRate) + "bps";
        else if (powOf10 >= 3 && powOf10 < 6)
            return format2Dec(bitRate / 1_000d) + "Kbps";
        else if (powOf10 >= 6 && powOf10 < 9)
            return format2Dec(bitRate / 1_000_000d) + "Mbps";
        return format2Dec(bitRate / 1_000_000_000d) + "Gbps";
    }
}
