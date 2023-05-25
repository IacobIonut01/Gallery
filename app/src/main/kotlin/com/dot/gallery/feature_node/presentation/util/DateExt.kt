/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.util

import android.os.Parcelable
import android.text.format.DateFormat
import com.dot.gallery.core.Constants
import kotlinx.parcelize.Parcelize
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

fun Long.getDateExt(): DateExt {
    val mediaDate = Calendar.getInstance(Locale.US)
    mediaDate.timeInMillis = this * 1000L
    return DateExt(
        month = mediaDate.getDisplayName(Calendar.MONTH, Calendar.LONG_FORMAT, Locale.US)!!,
        day = mediaDate.get(Calendar.DAY_OF_MONTH),
        year = mediaDate.get(Calendar.YEAR)
    )
}

fun getDateHeader(startDate: DateExt, endDate: DateExt): String {
    return if (startDate.year == endDate.year) {
        if (startDate.month == endDate.month) {
            if (startDate.day == endDate.day) {
                "${startDate.month} ${startDate.day}, ${startDate.year}"
            } else "${startDate.month} ${startDate.day} - ${endDate.day}, ${startDate.year}"
        } else
            "${startDate.month} ${startDate.day} - ${endDate.month} ${endDate.day}, ${startDate.year}"
    } else {
        "${startDate.month} ${startDate.day}, ${startDate.year} - ${endDate.month} ${endDate.day}, ${endDate.year}"
    }
}

fun getMonth(date: String): String {
    return try {
        val dateFormatExtended = SimpleDateFormat(Constants.EXTENDED_DATE_FORMAT, Locale.US).parse(date)
        val cal = Calendar.getInstance(Locale.US).apply { timeInMillis = dateFormatExtended!!.time }
        val month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG_FORMAT, Locale.US)!!
        val year = cal.get(Calendar.YEAR)
        "$month $year"
    } catch (e: ParseException) {
        try {
            val dateFormat = SimpleDateFormat(Constants.DEFAULT_DATE_FORMAT, Locale.US).parse(date)
            val cal = Calendar.getInstance(Locale.US).apply { timeInMillis = dateFormat!!.time }
            cal.getDisplayName(Calendar.MONTH, Calendar.LONG_FORMAT, Locale.US)!!
        } catch (e: ParseException) {
            ""
        }
    }
}

fun Long.getDate(
    format: CharSequence = Constants.DEFAULT_DATE_FORMAT,
): String {
    val mediaDate = Calendar.getInstance(Locale.US)
    mediaDate.timeInMillis = this * 1000L
    return DateFormat.format(format, mediaDate).toString()
}

fun Long.getDate(
    format: CharSequence = Constants.DEFAULT_DATE_FORMAT,
    weeklyFormat: CharSequence = Constants.WEEKLY_DATE_FORMAT,
    extendedFormat: CharSequence = Constants.EXTENDED_DATE_FORMAT,
    stringToday: String,
    stringYesterday: String
): String {
    val currentDate = Calendar.getInstance(Locale.US)
    currentDate.timeInMillis = System.currentTimeMillis()
    val mediaDate = Calendar.getInstance(Locale.US)
    mediaDate.timeInMillis = this * 1000L
    val different: Long = System.currentTimeMillis() - mediaDate.timeInMillis
    val secondsInMilli: Long = 1000
    val minutesInMilli = secondsInMilli * 60
    val hoursInMilli = minutesInMilli * 60
    val daysInMilli = hoursInMilli * 24

    val daysDifference = different / daysInMilli

    return when (daysDifference.toInt()) {
        0 -> {
            stringToday
        }

        1 -> {
            stringYesterday
        }

        else -> {
            if (daysDifference.toInt() in 2..5) {
                DateFormat.format(weeklyFormat, mediaDate).toString()
            } else {
                if (currentDate.get(Calendar.YEAR) > mediaDate.get(Calendar.YEAR)) {
                    DateFormat.format(extendedFormat, mediaDate).toString()
                } else DateFormat.format(format, mediaDate).toString()
            }
        }
    }
}

fun Long.formatMinSec(): String {
    return if (this == 0L) {
        "00:00"
    } else {
        String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(this),
            TimeUnit.MILLISECONDS.toSeconds(this) -
                    TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(this)
                    )
        )
    }
}

fun String?.formatMinSec(): String {
    return when (val value = this?.toLong()) {
        null -> ""
        else -> value.formatMinSec()
    }
}

private val datePatternChecks = mapOf(
    "dd mm yyyy" to
            "^(3[01]|[12][0-9]|0[1-9]|[1-9]) (1[0-2]|0[1-9]|[1-9]) [0-9]{4}$".toRegex(),

    "dd mm yyyy hh:MM" to
            "^(3[01]|[12][0-9]|0[1-9]|[1-9]) (1[0-2]|0[1-9]|[1-9]) [0-9]{4} [0-9][0-9]:([0]?[0-5][0-9]|[0-9])$".toRegex(),

    "mm dd yyyy" to
            "^(1[0-2]|0[1-9]|[1-9]) (3[01]|[12][0-9]|0[1-9]|[1-9]) [0-9]{4}$".toRegex(),

    "mm dd yyyy hh:MM" to
            "^(1[0-2]|0[1-9]|[1-9]) (3[01]|[12][0-9]|0[1-9]|[1-9]) [0-9]{4} [0-9][0-9]:([0]?[0-5][0-9]|[0-9])$".toRegex(),

    "dd MMM yyyy" to
            "^(3[01]|[12][0-9]|0[1-9]|[1-9]) [a-zA-Z]{3} [0-9]{4}$".toRegex(),

    "MMM dd yyyy" to
            "^[a-zA-Z]{3} (3[01]|[12][0-9]|0[1-9]|[1-9]) [0-9]{4}$".toRegex(),

    "dd MMMM yyyy" to
            "^\\b(0?[1-9]|[12][0-9]|3[01]) \\p{L}+ [0-9]{4}\\b$".toRegex(),

    "dd MMMM" to
            "^\\b(0?[1-9]|[12][0-9]|3[01]) \\p{L}+\\b$".toRegex(),

    "MMMM dd" to
            "^\\b\\p{L}+ (0?[1-9]|[12][0-9]|3[01])\\b$".toRegex(),

    "MMMM" to
            "^\\b\\p{L}+\\b$".toRegex(),

    // etc.
).map(::DatePatternChecker)


fun String.isDate(): Boolean = datePatternChecks
    .firstOrNull { it.isValid(this) }
    ?.run { true } ?: false

@Parcelize
data class DateExt(val month: String, val day: Int, val year: Int): Parcelable

/* label is the date format and pattern is the regex for that format.
*  result() provides a message (string) when trying to match, but can be
*  modified to get a bool result, like in boolResult(). */
data class DatePatternChecker(val label: String, val pattern: Regex) {
    fun isValid(dateString: String): Boolean = pattern.matches(dateString)
    constructor(labelPattern: Map.Entry<String, Regex>) : this(labelPattern.key, labelPattern.value)
}