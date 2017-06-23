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

import java.text.DateFormat;
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
        String str_date = DateFormat.getDateTimeInstance().format(System.currentTimeMillis());
        str_date = str_date.replaceAll(",", "_");
        str_date = str_date.replaceAll(" ", "_");
        str_date = str_date.replaceAll(":", "_");
        return str_date;
    }

    public static Date strDateToDate(String str_date) {
        SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());
        Date date = null;
        try {
            date = format.parse(str_date);
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse date: " + e.toString());
        }
        return date;
    }

    public static String bitRateToString(long bitRate) {
        if (bitRate <= 0)
            return "0 bps";

        double powOf10 = Math.round(Math.log10(bitRate));

        if (powOf10 < 3)
            return format2Dec(bitRate) + "bps";
        else if (powOf10 >= 3 && powOf10 < 6)
            return format2Dec(bitRate / 1_000d) + "kbps";
        else if (powOf10 >= 6 && powOf10 < 9)
            return format2Dec(bitRate / 1_000_000d) + "mbps";
        return format2Dec(bitRate / 1_000_000_000d) + "gbps";
    }
}
