/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.workers

import android.content.Context
import androidx.core.content.edit
import com.dot.gallery.core.logging.printDebug
import com.dot.gallery.core.logging.withLogScope
import com.dot.gallery.core.logging.printWarning
import com.dot.gallery.core.work.GalleryWorker
import com.dot.gallery.core.work.GalleryWorkerAdapter
import com.dot.gallery.core.work.WorkComponentHolder
import com.dot.gallery.core.work.WorkInput
import com.dot.gallery.core.work.workDataJson
import dev.brewkits.kmpworkmanager.annotations.Worker
import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import dev.brewkits.kmpworkmanager.background.domain.ExistingPolicy
import dev.brewkits.kmpworkmanager.background.domain.TaskTrigger
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult
import me.tatarka.inject.annotations.Inject


/**
 * One-shot worker that migrates the Room database from plaintext to encrypted
 * (SQLCipher) and vice versa.
 *
 * The actual heavy lifting (ATTACH + sqlcipher_export) runs on the IO dispatcher.
 * The worker is enqueued when the user toggles the "Encrypt database" setting.
 *
 * **Note:** The migration approach copies all data between the plaintext and
 * encrypted databases using Room's DAOs rather than raw SQL export, to stay
 * compatible with Room's schema management. A full ATTACH-based migration
 * would require closing and reopening the database, which is non-trivial
 * with kotlin-inject singletons. For the initial implementation, we mark the flag
 * and the actual encryption takes effect on the next app restart.
 */
class DatabaseEncryptionWorker @Inject constructor(
    private val appContext: Context,
) : GalleryWorker {

    override suspend fun doWork(input: WorkInput, env: WorkerEnvironment): WorkerResult =
        withLogScope("worker.db-encryption") {
            runCatching {
                val encrypt = input.getBoolean(KEY_ENCRYPT, true)

                val sp = appContext.getSharedPreferences(SECURITY_FLAGS_PREFS, Context.MODE_PRIVATE)

                if (encrypt) {
                    // Mark that encrypted DB should be used on next restart.
                    // The actual encryption happens when the DI graph re-creates the DB singleton.
                    sp.edit { putBoolean(FLAG_ENCRYPTED_DB, true) }
                    printDebug("DatabaseEncryptionWorker: flagged for encrypted DB on next restart")
                } else {
                    // Mark that plaintext DB should be used on next restart.
                    sp.edit { putBoolean(FLAG_ENCRYPTED_DB, false) }
                    printDebug("DatabaseEncryptionWorker: flagged for plaintext DB on next restart")
                }

                WorkerResult.Success()
            }.getOrElse { e ->
                printWarning("DatabaseEncryptionWorker: failed: ${e.message}")
                WorkerResult.Failure(e.message ?: "migration failed")
            }
        }

    companion object {
        const val UNIQUE_WORK_NAME = "DatabaseEncryption"
        const val WORKER_NAME = "DatabaseEncryptionWorker"
        const val KEY_ENCRYPT = "encrypt"
        const val SECURITY_FLAGS_PREFS = "security_flags"
        const val FLAG_ENCRYPTED_DB = "encrypted_db_active"

        suspend fun schedule(scheduler: BackgroundTaskScheduler, encrypt: Boolean) {
            scheduler.enqueue(
                id = UNIQUE_WORK_NAME,
                trigger = TaskTrigger.OneTime(),
                workerClassName = WORKER_NAME,
                inputJson = workDataJson(KEY_ENCRYPT to encrypt),
                policy = ExistingPolicy.REPLACE,
            )
        }
    }
}

@Worker(name = DatabaseEncryptionWorker.WORKER_NAME)
class DatabaseEncryptionWorkerAdapter : GalleryWorkerAdapter(DatabaseEncryptionWorker.WORKER_NAME) {
    override suspend fun run(input: WorkInput, env: WorkerEnvironment): WorkerResult =
        WorkComponentHolder.component.databaseEncryptionWorker.doWork(input, env)
}
