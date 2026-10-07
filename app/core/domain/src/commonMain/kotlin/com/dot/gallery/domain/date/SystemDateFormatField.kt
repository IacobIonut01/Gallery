/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.date

/**
 * Identifies each user-configurable date/time format field. Used to derive a
 * locale- and clock-aware default pattern from the system when the user has not
 * set a custom value.
 */
enum class SystemDateFormatField {
    /** Weekday name only (media grid weekly groups). */
    WEEKLY,

    /** Weekday + month + day (media grid classic groups). */
    DEFAULT,

    /** Weekday + month + day + year (media grid, previous years). */
    EXTENDED,

    /** Two lines: "month day" + time (media view header, current year). */
    HEADER,

    /** Two lines: "month day year" + time (media view header, previous years). */
    EXTENDED_HEADER,

    /** "month day, year • time" (EXIF / media info). */
    EXIF,
}
