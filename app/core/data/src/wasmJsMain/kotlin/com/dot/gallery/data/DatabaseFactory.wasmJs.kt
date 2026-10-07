/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker

/**
 * wasmJs builder for [InternalDatabase]. The SQLite driver runs in a web
 * worker — the worker script ships with androidx.sqlite:sqlite-web.
 */
fun getWasmDatabaseBuilder(worker: Worker): RoomDatabase.Builder<InternalDatabase> =
    Room.databaseBuilder<InternalDatabase>(InternalDatabase.NAME)
        .setDriver(WebWorkerSQLiteDriver(worker))
