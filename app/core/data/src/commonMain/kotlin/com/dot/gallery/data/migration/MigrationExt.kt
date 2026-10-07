/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.migration

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.prepare
import androidx.sqlite.async.step

/** Args-style exec — androidx.sqlite's executeSQL only takes raw SQL, so bind manually. */
internal suspend fun SQLiteConnection.executeSQL(sql: String, args: Array<out Any?>) {
    prepare(sql).use { stmt ->
        args.forEachIndexed { i, arg ->
            when (arg) {
                null -> stmt.bindNull(i + 1)
                is String -> stmt.bindText(i + 1, arg)
                is Long -> stmt.bindLong(i + 1, arg)
                is Int -> stmt.bindInt(i + 1, arg)
                is Double -> stmt.bindDouble(i + 1, arg)
                is Float -> stmt.bindFloat(i + 1, arg)
                is Boolean -> stmt.bindBoolean(i + 1, arg)
                is ByteArray -> stmt.bindBlob(i + 1, arg)
                else -> error("Unsupported bind argument: $arg")
            }
        }
        stmt.step()
    }
}
