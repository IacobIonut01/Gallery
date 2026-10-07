package com.dot.gallery.domain.date

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * JVM formatting via `java.time` — LDML pattern letters overlap the Android
 * subset this app uses (E/M/d/y/H/h/m/s/a).
 */
actual fun Long.formatEpochSec(pattern: String): String =
    DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        .format(Instant.ofEpochSecond(this).atZone(ZoneId.systemDefault()))

/**
 * JVM has no ICU skeleton resolution without ICU4J — the skeletons this app
 * passes ("MMMMd", "yMMMEEEd") are already valid LDML patterns, so the
 * skeleton is returned unchanged.
 */
actual fun systemDatePattern(skeleton: String): String = skeleton
