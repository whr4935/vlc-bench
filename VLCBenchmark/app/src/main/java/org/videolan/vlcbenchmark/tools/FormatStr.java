package org.videolan.vlcbenchmark.tools;

import java.text.DateFormat;
import java.text.DecimalFormat;

public class FormatStr {
    private static DecimalFormat decimalFormat_2 = new DecimalFormat("#.##");

    public static String format2Dec(double number) {
        return decimalFormat_2.format(number);
    }

    public static String getDateStr() {
        String str_date = DateFormat.getDateTimeInstance().format(System.currentTimeMillis());
        str_date = str_date.replaceAll(",", "");
        str_date = str_date.replaceAll(" ", "_");
        str_date = str_date.replaceAll(":", "_");
        return str_date;
    }
}
