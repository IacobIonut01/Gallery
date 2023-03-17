package com.dot.gallery.feature_node.presentation.util

import android.text.format.DateFormat
import com.dot.gallery.core.Constants
import java.util.Calendar
import java.util.Locale

fun Long.getDate(format: CharSequence = Constants.DEFAULT_DATE_FORMAT): String {
    val calendar = Calendar.getInstance(Locale.getDefault())
    calendar.timeInMillis = this * 1000L
    return DateFormat.format(format, calendar).toString()
}

fun updateDate(date: String, stringToday: String): String {
    val currentCal = Calendar.getInstance(Locale.getDefault())
    currentCal.timeInMillis = System.currentTimeMillis()

    return if (DateFormat.format(Constants.DEFAULT_DATE_FORMAT, currentCal).toString() == date) {
        stringToday
    } else date
}