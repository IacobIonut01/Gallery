/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.subsettings

/**
 * App-provided backend for [EditBackupsViewModel]: the WorkManager edit-backup pipeline
 * stays app-side while the ViewModel lives in this module.
 */
interface EditBackupsRuntime {

    /** Deletes expired backups; returns the deleted count (0 on failure/cancel). */
    suspend fun autoCleanupEditBackups(): Int

    /** Deletes all backups; returns true when the work finishes successfully. */
    suspend fun deleteAllEditBackups(): Boolean

    /** Deletes backups for [mediaIds]; returns true when the work finishes successfully. */
    suspend fun deleteSelectedEditBackups(mediaIds: LongArray): Boolean
}
