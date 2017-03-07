package org.videolan.vlcbenchmark.tools;

import java.text.DecimalFormat;

public class FormatStr {
    private static DecimalFormat decimalFormat_2 = new DecimalFormat("#.##");

    public static String format2Dec(double number) {
        return decimalFormat_2.format(number);
    }
}
