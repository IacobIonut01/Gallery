/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.date

import kotlinx.browser.window
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class IntlPart(val type: String, val value: String)

@OptIn(ExperimentalWasmJsInterop::class)
private fun intlPartsJson(millis: JsNumber, locale: JsString, options: JsString): JsString =
    js("JSON.stringify(new Intl.DateTimeFormat(locale || undefined, JSON.parse(options)).formatToParts(new Date(millis)))")

/**
 * Renders an LDML pattern via `Intl.DateTimeFormat.formatToParts`: each token
 * contributes an Intl option, parts are re-emitted in pattern order so literal
 * text (" • ", newlines) is preserved verbatim.
 */
private fun formatWithIntl(epochMillis: Long, pattern: String): String {
    val parts = mutableListOf<String>()
    val options = linkedMapOf<String, String>()
    var i = 0
    while (i < pattern.length) {
        val c = pattern[i]
        if (c.isLetter()) {
            var j = i
            while (j < pattern.length && pattern[j] == c) j++
            parts.add(pattern.substring(i, j))
            when (c) {
                'E' -> options["weekday"] = if (j - i >= 4) "long" else "short"
                'M', 'L' -> options["month"] = when (j - i) {
                    1 -> "numeric"; 2 -> "2-digit"; 3 -> "short"; 5 -> "narrow"; else -> "long"
                }
                'd' -> options["day"] = if (j - i >= 2) "2-digit" else "numeric"
                'y' -> options["year"] = if (j - i == 2) "2-digit" else "numeric"
                'H', 'k' -> { options["hour"] = "2-digit"; options["hourCycle"] = "h23" }
                'h', 'K' -> { options["hour"] = "2-digit"; options["hourCycle"] = "h12" }
                'm' -> options["minute"] = if (j - i >= 2) "2-digit" else "numeric"
                's' -> options["second"] = if (j - i >= 2) "2-digit" else "numeric"
                'a' -> { options["hour"] = "numeric"; options["hourCycle"] = "h12" }
            }
            i = j
        } else {
            var j = i
            while (j < pattern.length && !pattern[j].isLetter()) j++
            parts.add(pattern.substring(i, j))
            i = j
        }
    }
    if (options.isEmpty()) return pattern
    val optionsJson = options.entries.joinToString(",", "{", "}") { "\"${it.key}\":\"${it.value}\"" }
    val raw = intlPartsJson(
        epochMillis.toDouble().toJsNumber(),
        window.navigator.language.toJsString(),
        optionsJson.toJsString(),
    ).toString()
    val byType = Json.decodeFromString<List<IntlPart>>(raw).associate { it.type to it.value }
    val tokenToPart = mapOf(
        'E' to "weekday", 'M' to "month", 'L' to "month", 'd' to "day", 'y' to "year",
        'H' to "hour", 'k' to "hour", 'h' to "hour", 'K' to "hour",
        'm' to "minute", 's' to "second", 'a' to "dayPeriod",
    )
    return parts.joinToString("") { element ->
        if (element[0].isLetter()) {
            byType[tokenToPart[element[0]]] ?: element
        } else element
    }
}

actual fun Long.formatEpochSec(pattern: String): String =
    runCatching { formatWithIntl(this * 1000L, pattern) }.getOrElse { pattern }

actual fun systemDatePattern(skeleton: String): String = skeleton
