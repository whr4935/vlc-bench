package org.videolan.vlcbenchmark.tools

import android.content.Context
import android.util.Log
import org.videolan.vlcbenchmark.R
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

    fun byteRateToString(context: Context, bitRate: Long): String {
        if (bitRate <= 0)
            return "0 " + context.getString(R.string.size_unit_per_second_byte)
        val powOf10 = Math.round(Math.log10(bitRate.toDouble())).toDouble()

        if (powOf10 < 3)
            return format2Dec(bitRate.toDouble())
        else if (powOf10 >= 3 && powOf10 < 6)
            return format2Dec(bitRate / 1_000.0 / 8.0) + " " +
                    context.getString(R.string.size_unit_per_second_kilo)
        else if (powOf10 >= 6 && powOf10 < 9)
            return format2Dec(bitRate / 1_000_000.0 / 8.0) + " " +
                    context.getString(R.string.size_unit_per_second_mega)
        return format2Dec(bitRate / 1_000_000_000.0 / 8.0) + " " +
                context.getString(R.string.size_unit_per_second_giga)
    }

    fun byteSizeToString(context: Context, size: Long): String {
        val unit: String
        val prettySize: Double
        if (size / 1_000_000_000 > 0) {
            unit = context.getString(R.string.size_unit_giga)
            prettySize = size / 1_000_000_000.0
        } else {
            unit = context.getString(R.string.size_unit_mega)
            prettySize = size / 1_000_000.0
        }
        return format2Dec(prettySize) + " " + unit
    }
}
