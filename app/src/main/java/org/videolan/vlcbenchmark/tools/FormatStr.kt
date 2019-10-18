package org.videolan.vlcbenchmark.tools

import android.util.Log
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object FormatStr {

    private val TAG = FormatStr::class.java.name

    private val decimalFormat_2 = DecimalFormat("#.##")

    val dateStr: String
        get() {
            val format = SimpleDateFormat("yyMMddHHmmssZ", Locale.getDefault())
            return format.format(System.currentTimeMillis())
        }

    fun format2Dec(number: Double): String {
        return decimalFormat_2.format(number)
    }

    fun strDateToDate(str_date: String): Date? {
        val format = SimpleDateFormat("yyMMddHHmmssZ", Locale.getDefault())
        var date: Date? = null
        try {
            date = format.parse(str_date)
        } catch (e: ParseException) {
            Log.e(TAG, "strDateToDate: Failed to parse date: $str_date\n$e")
        }

        return date
    }

    /**
     * Converts date string used for filenames
     * to pretty readable date string ("dd MMM yyyy HH:mm:ss")
     * @param date_str filename date string
     * @return readable date string
     */
    fun toDatePrettyPrint(date_str: String): String {
        var date_str = date_str
        var format = SimpleDateFormat("yyMMddHHmmssZ", Locale.getDefault())
        var date: Date? = null
        try {
            date = format.parse(date_str)
        } catch (e: ParseException) {
            Log.e(TAG, "Failed to parse date: $date_str\n$e")
        }

        format = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
        date_str = format.format(date)
        return date_str
    }

    /**
     * Converts the readable date string ("dd MMM yyyy HH:mm:ss")
     * to the date string for filenames
     * @param date_str pretty readable date
     * @return underscore separated date string
     */
    fun fromDatePrettyPrint(date_str: String): String {
        var format = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
        var date: Date? = null
        try {
            date = format.parse(date_str)
        } catch (e: ParseException) {
            Log.e(TAG, "Failed to parse date: $e")
        }

        format = SimpleDateFormat("yyMMddHHmmssZ", Locale.getDefault())
        return format.format(date)
    }

    fun bitRateToString(bitRate: Long): String {
        if (bitRate <= 0)
            return "0 bps"
        val powOf10 = Math.round(Math.log10(bitRate.toDouble())).toDouble()

        if (powOf10 < 3)
            return format2Dec(bitRate.toDouble()) + "bps"
        else if (powOf10 >= 3 && powOf10 < 6)
            return format2Dec(bitRate / 1_000.0) + "Kbps"
        else if (powOf10 >= 6 && powOf10 < 9)
            return format2Dec(bitRate / 1_000_000.0) + "Mbps"
        return format2Dec(bitRate / 1_000_000_000.0) + "Gbps"
    }

    fun sizeToString(size: Long): String {
        val unit: String
        val prettySize: Double
        if (size / 1_000_000_000 > 0) {
            unit = "Go"
            prettySize = size / 1_000_000_000.0
        } else {
            unit = "Mo"
            prettySize = size / 1_000_000.0
        }
        return format2Dec(prettySize) + " " + unit
    }
}
