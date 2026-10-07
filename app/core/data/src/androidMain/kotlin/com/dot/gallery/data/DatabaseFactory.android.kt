/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * Android builder for [InternalDatabase]. Pass [SqlCipherDriver] for the
 * production encrypted database, or [BundledSQLiteDriver] for tests /
 * unencrypted fallbacks.
 */
fun getAndroidDatabaseBuilder(
    context: Context
): RoomDatabase.Builder<InternalDatabase> =
    Room.databaseBuilder<InternalDatabase>(context, InternalDatabase.NAME)

/** Plaintext fallback for devices without a usable hardware keystore. */
fun getBundledDatabase(context: Context): InternalDatabase =
    getInternalDatabase(
        getAndroidDatabaseBuilder(context)
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .fallbackToDestructiveMigration(true)
    )
