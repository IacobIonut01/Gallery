/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.settings

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
/**
 * A typed settings key. Backends persist by [name]; [encode]/[decode] are the
 * string codec used by flat stores (localStorage, snapshots) while typed stores
 * (DataStore) use [type] to pick their native representation.
 */
class PrefKey<T> private constructor(
    val name: String,
    val type: PrefType,
    val default: T,
    private val enc: (T) -> String,
    private val dec: (String) -> T?,
) {
    fun encode(value: T): String = enc(value)

    fun decode(raw: String): T = dec(raw) ?: default

    /** Same key, different default — for platform-conditioned defaults (e.g. `SdkCompat.isSamsung`). */
    fun withDefault(newDefault: T): PrefKey<T> = PrefKey(name, type, newDefault, enc, dec)

    companion object {
        fun bool(name: String, default: Boolean) =
            PrefKey(name, PrefType.BOOLEAN, default, Boolean::toString) { it.toBooleanStrictOrNull() }

        fun int(name: String, default: Int) =
            PrefKey(name, PrefType.INT, default, Int::toString) { it.toIntOrNull() }

        fun long(name: String, default: Long) =
            PrefKey(name, PrefType.LONG, default, Long::toString) { it.toLongOrNull() }

        fun float(name: String, default: Float) =
            PrefKey(name, PrefType.FLOAT, default, Float::toString) { it.toFloatOrNull() }

        fun double(name: String, default: Double) =
            PrefKey(name, PrefType.DOUBLE, default, Double::toString) { it.toDoubleOrNull() }

        fun string(name: String, default: String) =
            PrefKey(name, PrefType.STRING, default, { it }) { it }

        fun stringSet(name: String, default: Set<String> = emptySet()) =
            PrefKey(
                name, PrefType.STRING_SET, default,
                { Json.encodeToString(SetSerializer(String.serializer()), it) },
                { raw -> runCatching { Json.decodeFromString(SetSerializer(String.serializer()), raw) }.getOrNull() },
            )

        /** JSON-encoded value — same wire format as the app's `rememberPreferenceSerializable`. */
        fun <T> serializable(name: String, default: T, serializer: KSerializer<T>) =
            PrefKey(
                name, PrefType.STRING, default,
                { Json.encodeToString(serializer, it) },
                { raw -> runCatching { Json.decodeFromString(serializer, raw) }.getOrNull() },
            )
    }
}
