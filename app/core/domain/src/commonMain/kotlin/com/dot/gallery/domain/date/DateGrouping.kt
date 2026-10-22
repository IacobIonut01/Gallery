/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.date

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

fun Long.getDate(
    format: String,
    weeklyFormat: String,
    extendedFormat: String,
    stringToday: String,
    stringYesterday: String,
): String {
    val mediaDate = epochSecToLocalDate()
    val today = todayLocalDate()
    // Calendar-day difference (not raw time division) so photos from the same
    // day land in one group regardless of hour-of-day.
    return dateBucket(
        mediaDate, today, mediaDate.daysUntil(today),
        format, weeklyFormat, extendedFormat, stringToday, stringYesterday,
    )
}

/** Which label a calendar-day distance from the media's date to today takes. */
enum class DateHeaderKind {
    TODAY,
    YESTERDAY,
    WEEKDAY,
    DATE,
}

/**
 * `daysFromMediaToToday` is negative when the media's calendar day is still ahead.
 * That day keeps its own date. Only the current day is Today.
 */
fun dateHeaderKind(daysFromMediaToToday: Int): DateHeaderKind = when {
    daysFromMediaToToday < 0 -> DateHeaderKind.DATE
    daysFromMediaToToday == 0 -> DateHeaderKind.TODAY
    daysFromMediaToToday == 1 -> DateHeaderKind.YESTERDAY
    daysFromMediaToToday in 2..6 -> DateHeaderKind.WEEKDAY
    else -> DateHeaderKind.DATE
}

private fun Long.dateBucket(
    mediaDate: LocalDate,
    today: LocalDate,
    daysDifference: Int,
    format: String,
    weeklyFormat: String,
    extendedFormat: String,
    stringToday: String,
    stringYesterday: String,
): String = when (dateHeaderKind(daysDifference)) {
    DateHeaderKind.TODAY -> stringToday
    DateHeaderKind.YESTERDAY -> stringYesterday
    DateHeaderKind.WEEKDAY -> formatEpochSec(weeklyFormat)
    DateHeaderKind.DATE -> {
        if (today.year != mediaDate.year) formatEpochSec(extendedFormat)
        else formatEpochSec(format)
    }
}

/**
 * Pre-computes today's date once, then classifies timestamps without
 * re-deriving the current day — the hot-path counterpart of [getDate].
 */
class DateGrouper(
    private val format: String,
    private val weeklyFormat: String,
    private val extendedFormat: String,
    private val stringToday: String,
    private val stringYesterday: String,
) {
    private val today: LocalDate = todayLocalDate()

    fun classify(timestampSec: Long): String {
        val mediaDate = timestampSec.epochSecToLocalDate()
        return timestampSec.dateBucket(
            mediaDate, today, mediaDate.daysUntil(today),
            format, weeklyFormat, extendedFormat, stringToday, stringYesterday,
        )
    }

    /** Calendar-day distance from today — the label-vs-date distinction [classify] hides. */
    fun daysUntilToday(timestampSec: Long): Int =
        timestampSec.epochSecToLocalDate().daysUntil(today)

    val currentYear: Int get() = today.year
}
