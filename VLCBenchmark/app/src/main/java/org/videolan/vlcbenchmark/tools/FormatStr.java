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
        Log.d("FormatStr", "date: " + str_date);
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
}
