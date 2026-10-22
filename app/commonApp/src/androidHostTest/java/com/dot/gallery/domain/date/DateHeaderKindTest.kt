/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.date

import org.junit.Assert.assertEquals
import org.junit.Test

class DateHeaderKindTest {

    @Test
    fun aFutureCalendarDayKeepsItsOwnDate() {
        assertEquals(DateHeaderKind.DATE, dateHeaderKind(daysFromMediaToToday = -1))
        assertEquals(DateHeaderKind.DATE, dateHeaderKind(daysFromMediaToToday = -400))
    }

    @Test
    fun todayAndTheRecentPastKeepTheirLabels() {
        assertEquals(DateHeaderKind.TODAY, dateHeaderKind(0))
        assertEquals(DateHeaderKind.YESTERDAY, dateHeaderKind(1))
        assertEquals(DateHeaderKind.WEEKDAY, dateHeaderKind(6))
        assertEquals(DateHeaderKind.DATE, dateHeaderKind(7))
    }
}
