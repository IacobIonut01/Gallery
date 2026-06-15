/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import androidx.core.content.edit
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.room.Room
import com.dot.gallery.core.metrics.StartupTracer
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.data.data_source.migration.MIGRATION_12_13
import com.dot.gallery.feature_node.presentation.util.printDebug
import com.dot.gallery.feature_node.presentation.util.printWarning
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Creates a Room [InternalDatabase] backed by SQLCipher encryption.
 *
 * On first use, if an existing unencrypted database exists, it is encrypted
 * in-place using `sqlcipher_export` so that all indexed media data is preserved.
 *
 * The encryption passphrase is a random 32-byte value wrapped with an
 * Android Keystore AES-GCM key and stored in SharedPreferences.
 */
object EncryptedDatabaseFactory {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "gallery_db_encryption_key"
    private const val PREFS_NAME = "encrypted_db_prefs"
    private const val PREF_WRAPPED_PASSPHRASE = "wrapped_passphrase"
    private const val FLAG_DB_ENCRYPTED = "db_encrypted"
    private const val GCM_TAG_LENGTH = 128
    private const val PREF_RAW_KEY_MIGRATED = "raw_key_migrated"

    fun create(context: Context): InternalDatabase {
        val createSpan = StartupTracer.begin("EncryptedDB.create")

        StartupTracer.trace("EncryptedDB.loadLibrary") {
            System.loadLibrary("sqlcipher")
        }

        // If the DB hasn't been encrypted yet, clear any stale passphrase from
        // previous failed attempts (which used raw bytes instead of hex).
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(FLAG_DB_ENCRYPTED, false)) {
            prefs.edit {remove(PREF_WRAPPED_PASSPHRASE)}
        }

        val passphrase = StartupTracer.trace("EncryptedDB.getOrCreatePassphrase") {
            getOrCreatePassphrase(context)
        }

        // Migrate existing unencrypted DB to encrypted if needed
        StartupTracer.trace("EncryptedDB.migrateIfNeeded") {
            migrateUnencryptedDbIfNeeded(context, passphrase)
        }

        // Convert the hex passphrase to raw-key format: x'<hex>'
        // This tells SQLCipher to use the 256-bit key directly, skipping
        // the expensive PBKDF2-HMAC-SHA512 (256k iterations, ~630ms).
        val hexString = String(passphrase, Charsets.UTF_8)
        val rawKeyPassphrase = "x'$hexString'".toByteArray(Charsets.UTF_8)

        // Migrate existing databases from passphrase-based KDF to raw key.
        // This must happen before Room opens the database.
        StartupTracer.trace("EncryptedDB.rekeyIfNeeded") {
            migrateToRawKeyIfNeeded(context, passphrase, hexString)
        }

        val factory = SupportOpenHelperFactory(rawKeyPassphrase)
        val db = StartupTracer.trace("EncryptedDB.roomBuilder") {
            Room.databaseBuilder(
                context,
                InternalDatabase::class.java,
                InternalDatabase.NAME
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_12_13)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .fallbackToDestructiveMigration(false)
                .build()
        }

        // Eagerly open the database connection in the background to start
        // SQLCipher key derivation early, rather than lazily on first query.
        // This overlaps the expensive PBKDF2 work with other startup tasks.
        Thread({
            val rawDb = StartupTracer.trace("EncryptedDB.warmup") {
                db.openHelper.writableDatabase
            }
            // Disable secure memory wiping — reduces per-page alloc/free overhead.
            // Safe for a gallery app where the DB key is already in process memory.
            try { rawDb.query("PRAGMA cipher_memory_security = OFF").close() } catch (_: Exception) {}
            // Warm SQLCipher's page cache by reading catalog + data tables.
            // Without this, the first Room query pays ~1.3s of cold-cache
            // overhead decrypting system pages from disk.
            StartupTracer.trace("EncryptedDB.cacheWarmup") {
                rawDb.query("SELECT * FROM room_master_table").close()
                rawDb.query("SELECT * FROM blacklist").close()
                try {
                    val c = rawDb.query("SELECT * FROM cloud_media WHERE trashed = 0 AND archived = 0 ORDER BY timestamp DESC")
                    while (c.moveToNext()) { /* iterate to decrypt SQLCipher pages into cache */ }
                    c.close()
                } catch (_: Exception) { }
            }
        }, "db-warmup").start()

        StartupTracer.end(createSpan)
        return db
    }

    /**
     * One-time migration: rekey an existing database from passphrase-based
     * KDF (PBKDF2, ~630ms per open) to raw-key format (~0ms per open).
     *
     * Opens the DB with the old passphrase, then issues PRAGMA rekey with
     * the raw key prefix so subsequent opens skip KDF entirely.
     */
    private fun migrateToRawKeyIfNeeded(context: Context, passphrase: ByteArray, hexString: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_RAW_KEY_MIGRATED, false)) return

        val dbFile = context.getDatabasePath(InternalDatabase.NAME)
        if (!dbFile.exists() || dbFile.length() == 0L) {
            // New database — will be created with raw key directly
            prefs.edit { putBoolean(PREF_RAW_KEY_MIGRATED, true) }
            return
        }

        try {
            // Open with the old passphrase (triggers PBKDF2 one last time)
            val oldDb = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                String(passphrase, Charsets.UTF_8),
                null,
                SQLiteDatabase.OPEN_READWRITE,
                null,
                null
            )
            try {
                // Verify the old passphrase works
                oldDb.rawQuery("SELECT COUNT(*) FROM sqlite_schema", null)
                    .use { it.moveToFirst() }
                // Rekey to raw-key format — rewrites the file header so future
                // opens with x'<hex>' skip KDF entirely.
                oldDb.rawExecSQL("PRAGMA rekey = \"x'$hexString'\"")
                printDebug("EncryptedDatabaseFactory: rekeyed DB from passphrase → raw key")
            } finally {
                oldDb.close()
            }
            prefs.edit { putBoolean(PREF_RAW_KEY_MIGRATED, true) }
        } catch (e: Exception) {
            // If rekey fails, we'll fall back to raw-key open which will fail,
            // and Room will recreate the DB. Log but don't crash.
            printWarning("EncryptedDatabaseFactory: raw-key migration failed: ${e.message}")
        }
    }

    /**
     * If the plaintext database exists and hasn't been encrypted yet,
     * encrypt it using `sqlcipher_export`.
     *
     * Steps:
     * 1. Move the plaintext DB to a backup path
     * 2. Open the backup with SQLCipher in plaintext mode (`null` password)
     * 3. ATTACH a new encrypted DB at the original path with the passphrase
     * 4. `sqlcipher_export` all data from plaintext → encrypted
     * 5. Copy the schema version and clean up
     */
    private fun migrateUnencryptedDbIfNeeded(context: Context, passphrase: ByteArray) {
        val flags = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (flags.getBoolean(FLAG_DB_ENCRYPTED, false)) return

        val dbFile = context.getDatabasePath(InternalDatabase.NAME)
        if (!dbFile.exists() || dbFile.length() == 0L) {
            // No existing DB — mark as encrypted (will be created encrypted by Room)
            flags.edit {putBoolean(FLAG_DB_ENCRYPTED, true)}
            return
        }

        val backupFile = File(dbFile.parentFile, "${InternalDatabase.NAME}_plaintext_backup")
        try {
            // Move plaintext DB + journal files to backup
            dbFile.renameTo(backupFile)
            File(dbFile.absolutePath + "-wal").let {
                if (it.exists()) it.renameTo(File(backupFile.absolutePath + "-wal"))
            }
            File(dbFile.absolutePath + "-shm").let {
                if (it.exists()) it.renameTo(File(backupFile.absolutePath + "-shm"))
            }

            val hexString = String(passphrase, Charsets.UTF_8)
            val rawKeyStr = "x'$hexString'"

            // Pre-create an empty encrypted database at the original path.
            // SQLCipher's ATTACH cannot create a new file when the main
            // connection was opened in plaintext mode (null key) — the
            // internal open() call omits O_CREAT, causing SQLITE_CANTOPEN.
            // By pre-creating the target, ATTACH just opens an existing file.
            // Use raw-key format directly so no PBKDF2 rekey is needed later.
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                rawKeyStr,
                null,
                SQLiteDatabase.CREATE_IF_NECESSARY or SQLiteDatabase.OPEN_READWRITE,
                null,
                null
            ).close()

            // Open the plaintext backup without a key.
            // null password → sqlcipher skips sqlite3_key → plaintext mode.
            val plainDb = SQLiteDatabase.openDatabase(
                backupFile.absolutePath,
                null as String?,
                null,
                SQLiteDatabase.OPEN_READWRITE,
                null,
                null
            )

            try {
                val version = plainDb.version

                // Attach the pre-created encrypted DB at the original path.
                // SupportOpenHelperFactory converts byte[] → String via new String(byte[]),
                // so we must use the same conversion for the KEY clause.
                plainDb.rawExecSQL(
                    "ATTACH DATABASE '${dbFile.absolutePath}' AS encrypted KEY \"$rawKeyStr\""
                )

                // Export all tables, indexes, triggers from plaintext → encrypted
                plainDb.rawExecSQL("SELECT sqlcipher_export('encrypted')")

                // Preserve Room's schema version
                plainDb.rawExecSQL("PRAGMA encrypted.user_version = $version")

                plainDb.rawExecSQL("DETACH DATABASE encrypted")
            } finally {
                plainDb.close()
            }

            // Clean up backup
            File(backupFile.absolutePath + "-wal").delete()
            File(backupFile.absolutePath + "-shm").delete()
            backupFile.delete()

            flags.edit {
                putBoolean(FLAG_DB_ENCRYPTED, true)
                putBoolean(PREF_RAW_KEY_MIGRATED, true)
            }
            printDebug("EncryptedDatabaseFactory: migrated plaintext DB → encrypted (raw key) via sqlcipher_export")
        } catch (e: Exception) {
            printWarning("EncryptedDatabaseFactory: migration failed: ${e.message}")
            // Delete any partially-created encrypted DB at the original path
            dbFile.delete()
            File(dbFile.absolutePath + "-wal").delete()
            File(dbFile.absolutePath + "-shm").delete()
            // Restore plaintext backup
            if (backupFile.exists()) {
                backupFile.renameTo(dbFile)
                File(backupFile.absolutePath + "-wal").let {
                    if (it.exists()) it.renameTo(File(dbFile.absolutePath + "-wal"))
                }
                File(backupFile.absolutePath + "-shm").let {
                    if (it.exists()) it.renameTo(File(dbFile.absolutePath + "-shm"))
                }
            }
            // Clear stale passphrase so next attempt generates a fresh one.
            // Do NOT mark as encrypted — let the plaintext fallback handle it.
            flags.edit {remove(PREF_WRAPPED_PASSPHRASE)}
            throw e
        }
    }

    /**
     * Quick-check that the existing DB file can actually be opened with
     * [passphrase]. If it can't (e.g. a previous buggy migration set
     * [FLAG_DB_ENCRYPTED] but left the DB unencrypted), reset the flag
     * and passphrase so the next launch retries, then throw to fall
     * back to the plaintext Room builder in [AppModule].
     */
    private fun validatePassphrase(context: Context, passphrase: ByteArray) {
        val dbFile = context.getDatabasePath(InternalDatabase.NAME)
        if (!dbFile.exists() || dbFile.length() == 0L) return

        try {
            val testDb = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                String(passphrase),
                null,
                SQLiteDatabase.OPEN_READONLY,
                null,
                null
            )
            try {
                testDb.rawQuery("SELECT COUNT(*) FROM sqlite_schema", null)
                    .use { it.moveToFirst() }
            } finally {
                testDb.close()
            }
        } catch (e: Exception) {
            printWarning("EncryptedDatabaseFactory: passphrase validation failed, resetting: ${e.message}")
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                putBoolean(FLAG_DB_ENCRYPTED, false)
                remove(PREF_WRAPPED_PASSPHRASE)
            }
            throw e
        }
    }

    /**
     * Returns the passphrase for SQLCipher as UTF-8 bytes of a hex string.
     *
     * On first call, 32 random bytes are generated and hex-encoded to produce
     * a 64-character ASCII passphrase. This is then encrypted with the Keystore
     * key and stored in SharedPreferences. Using hex ensures the passphrase
     * survives the `byte[]` → `String` conversion that [SupportOpenHelperFactory]
     * performs internally.
     *
     * Works with both software-backed and hardware-backed (TEE/StrongBox)
     * Keystore keys because we never call [SecretKey.getEncoded].
     */
    private fun getOrCreatePassphrase(context: Context): ByteArray {
        val key = getOrCreateKey()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(PREF_WRAPPED_PASSPHRASE, null)

        return if (stored != null) {
            unwrapPassphrase(key, stored)
        } else {
            // Generate 32 random bytes → 64 hex chars (ASCII-safe)
            val random = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val passphrase = random.joinToString("") { "%02x".format(it) }
                .toByteArray(Charsets.UTF_8)
            val wrapped = wrapPassphrase(key, passphrase)
            prefs.edit {putString(PREF_WRAPPED_PASSPHRASE, wrapped)}
            printDebug("EncryptedDatabaseFactory: generated and wrapped new passphrase")
            passphrase
        }
    }

    // Cache the Keystore key to avoid repeated expensive Keystore lookups.
    private val cachedKey: SecretKey by lazy {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } else {
            val keyGen = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
            )
            keyGen.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            keyGen.generateKey()
        }
    }

    private fun getOrCreateKey(): SecretKey = cachedKey

    private fun wrapPassphrase(key: SecretKey, passphrase: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(passphrase)
        val blob = iv + ciphertext
        return Base64.encodeToString(blob, Base64.NO_WRAP)
    }

    private fun unwrapPassphrase(key: SecretKey, wrapped: String): ByteArray {
        val blob = Base64.decode(wrapped, Base64.NO_WRAP)
        val iv = blob.copyOfRange(0, 12)
        val ciphertext = blob.copyOfRange(12, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertext)
    }
}
