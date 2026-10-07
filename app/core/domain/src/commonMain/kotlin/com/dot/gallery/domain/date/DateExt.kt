/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.date

import com.dot.gallery.domain.parcel.Parcelize
import com.dot.gallery.domain.parcel.Parcelable

/** Localized month/day/year triple used by range headers. */
@Parcelize
data class DateExt(val month: String, val day: Int, val year: Int) : Parcelable

fun Long.getDateExt(): DateExt {
    val date = epochSecToLocalDate()
    return DateExt(
        month = formatEpochSec("MMMM"),
        day = date.day,
        year = date.year,
    )
}

fun getDateHeader(startDate: DateExt, endDate: DateExt): String =
    if (startDate.year == endDate.year) {
        if (startDate.month == endDate.month) {
            if (startDate.day == endDate.day) {
                "${startDate.month} ${startDate.day}, ${startDate.year}"
            } else "${startDate.month} ${startDate.day} - ${endDate.day}, ${startDate.year}"
        } else
            "${startDate.month} ${startDate.day} - ${endDate.month} ${endDate.day}, ${startDate.year}"
    } else {
        "${startDate.month} ${startDate.day}, ${startDate.year} - ${endDate.month} ${endDate.day}, ${endDate.year}"
    }
