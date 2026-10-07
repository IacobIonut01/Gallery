/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data

import android.database.Cursor
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import net.zetetic.database.sqlcipher.SQLiteDatabase

/**
 * [SQLiteDriver] backed by SQLCipher for Android (net.zetetic:sqlcipher-android).
 * Room 3 dropped the SupportSQLite API, so encryption rides through this adapter:
 * every opened [SQLiteConnection] wraps a passphrase-protected [SQLiteDatabase].
 */
class SqlCipherDriver(private val passphrase: ByteArray) : SQLiteDriver {

    // Each open() returns a SQLCipher SQLiteDatabase, which internally owns a
    // native connection pool (thread-confined sessions). Declaring the pool
    // makes Room use its PassthroughConnectionPool — a single logical
    // connection — instead of stacking a 4-reader/1-writer pool of separate
    // SQLiteDatabases on top. Besides being the correct layering, the stacked
    // pool's reader handles attached WAL -shm read-only and crashed every
    // BEGIN DEFERRED with SQLiteReadOnlyDatabaseException.
    override val hasConnectionPool: Boolean get() = true

    override fun open(fileName: String): SQLiteConnection {
        // Room's connection pool opens several connections concurrently, each
        // a full SQLCipher SQLiteDatabase (itself backed by a native conn pool).
        // Two hazards at cold start:
        //  1. Journal churn: without ENABLE_WRITE_AHEAD_LOGGING every internal
        //     connection tries `PRAGMA journal_mode=delete` on first use and
        //     they lock each other out (the file is WAL on disk). Keeping WAL
        //     makes that pragma a no-op.
        //  2. WAL recovery race: after a killed process the -wal/-shm files are
        //     rebuilt lazily on first page access — concurrently across the
        //     pool. The loser attaches read-only and the first write crashes
        //     with SQLiteReadOnlyDatabaseException. A trivial query under the
        //     open lock forces key derivation + recovery while it's the only
        //     connection, so every later attach hits a clean -shm.
        val db = synchronized(OPEN_LOCK) {
            SQLiteDatabase.openDatabase(
                fileName, passphrase, null,
                SQLiteDatabase.CREATE_IF_NECESSARY or SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING,
                null, null
            ).also {
                // Touch a real catalog page under the open lock: forces key
                // derivation + WAL -shm attach while no other connections
                // exist, so later attaches race nothing.
                it.rawQuery("SELECT count(*) FROM sqlite_master").close()
            }
        }
        return SqlCipherConnection(db)
    }

    private companion object {
        val OPEN_LOCK = Any()
    }
}

private class SqlCipherConnection(
    private val db: SQLiteDatabase
) : SQLiteConnection {

    override fun inTransaction(): Boolean = db.inTransaction()

    override fun prepare(sql: String): SQLiteStatement = SqlCipherStatement(db, sql)

    override fun close() = db.close()
}

/**
 * Statement that defers execution until the first [step]: row-bearing
 * statements go through a cursor (`rawQuery` with typed `Object[]` bindings),
 * everything else through a compiled statement (`execute`). Bindings made
 * after execution flag the statement dirty so the next [step] re-queries.
 */
private class SqlCipherStatement(
    private val db: SQLiteDatabase,
    private val sql: String
) : SQLiteStatement {

    private var args = arrayOfNulls<Any>(INITIAL_ARG_CAPACITY)
    private var boundCount = 0
    private var cursor: Cursor? = null
    private var compiled: net.zetetic.database.sqlcipher.SQLiteStatement? = null
    private var executed = false
    private var dirty = false
    private var closed = false

    private val isQuery = KEYWORD_REGEX.find(sql.trimStart())?.value?.let {
        it == "SELECT" || it == "WITH" || it == "PRAGMA" || it == "EXPLAIN" || it == "VALUES"
    } ?: false

    // SQLite reports zero bindable parameters for statements like
    // `PRAGMA busy_timeout` even though callers (Room's connection
    // configuration) may have bound values on the wrapper. Passing them to
    // rawQuery crashes with "index out of range", so clamp to the number of
    // `?` placeholders actually present in the SQL.
    private val placeholderCount = sql.count { it == '?' }

    private fun record(index: Int, value: Any?) {
        check(!closed) { "Statement is closed" }
        if (index > args.size) args = args.copyOf(index + INITIAL_ARG_CAPACITY)
        args[index - 1] = value
        if (index > boundCount) boundCount = index
        // Room's statement cache re-binds an already-executed statement (e.g.
        // EntityInsertAdapter loops bind→step per row) — flag it dirty so the
        // next step() re-executes with the new bindings.
        if (executed) dirty = true
    }

    override fun bindBlob(index: Int, value: ByteArray) = record(index, value)
    override fun bindDouble(index: Int, value: Double) = record(index, value)
    override fun bindLong(index: Int, value: Long) = record(index, value)
    override fun bindText(index: Int, value: String) = record(index, value)
    override fun bindNull(index: Int) = record(index, null)

    /**
     * Opens (or re-queries when dirty) the cursor on demand. Room's generated
     * DAO code resolves column indices via `getColumnName`/`getColumnCount`
     * BEFORE the first `step()`, so the cursor must exist by then — deferred
     * execution until `step()` leaves `columnCount == 0` and Room crashes with
     * "Column 'x' does not exist".
     */
    private fun ensureCursor(): Cursor {
        cursor.takeUnless { dirty }?.let { return it }
        cursor?.close()
        dirty = false
        executed = true
        // `rawQuery` takes `Object...` varargs — without `*` the array is
        // passed as ONE vararg element and bindAllArgs crashes on
        // "index 1 out of range" for statements with no placeholders.
        return db.rawQuery(sql, *args.copyOf(minOf(boundCount, placeholderCount))).also { cursor = it }
    }

    override fun step(): Boolean {
        check(!closed) { "Statement is closed" }
        if (isQuery) {
            return ensureCursor().moveToNext()
        }
        if (!executed || dirty) {
            // Mutation/DDL: execute via a compiled statement. `step()`
            // returns false since there is no result row.
            val stmt = compiled ?: db.compileStatement(sql).also { compiled = it }
            stmt.clearBindings()
            for (i in 1..boundCount) {
                when (val v = args[i - 1]) {
                    null -> stmt.bindNull(i)
                    is ByteArray -> stmt.bindBlob(i, v)
                    is Double -> stmt.bindDouble(i, v)
                    is Long -> stmt.bindLong(i, v)
                    is String -> stmt.bindString(i, v)
                    is Int -> stmt.bindLong(i, v.toLong())
                    else -> stmt.bindString(i, v.toString())
                }
            }
            stmt.execute()
            executed = true
            dirty = false
        }
        return false
    }

    private fun requireCursor(): Cursor =
        if (isQuery) ensureCursor()
        else checkNotNull(cursor) { "No row available — call step() before reading columns" }

    override fun getBlob(index: Int): ByteArray = requireCursor().getBlob(index)
    override fun getDouble(index: Int): Double = requireCursor().getDouble(index)
    override fun getLong(index: Int): Long = requireCursor().getLong(index)
    override fun getText(index: Int): String = requireCursor().getString(index)
    override fun isNull(index: Int): Boolean = requireCursor().isNull(index)
    override fun getColumnCount(): Int =
        if (isQuery) ensureCursor().columnCount else 0
    override fun getColumnName(index: Int): String = requireCursor().getColumnName(index)

    override fun getColumnType(index: Int): Int = when (requireCursor().getType(index)) {
        Cursor.FIELD_TYPE_INTEGER -> SQLITE_DATA_INTEGER
        Cursor.FIELD_TYPE_FLOAT -> SQLITE_DATA_FLOAT
        Cursor.FIELD_TYPE_STRING -> SQLITE_DATA_TEXT
        Cursor.FIELD_TYPE_BLOB -> SQLITE_DATA_BLOB
        else -> SQLITE_DATA_NULL
    }

    override fun reset() {
        // Reset to just-prepared state: drop the result cursor so the next
        // step() re-queries, and clear bindings per the SQLiteStatement
        // contract (Room's statement cache relies on this for reuse).
        cursor?.close()
        cursor = null
        args.fill(null)
        boundCount = 0
        executed = false
        dirty = false
    }

    override fun clearBindings() {
        args.fill(null)
        boundCount = 0
        dirty = executed
    }

    override fun close() {
        closed = true
        cursor?.close()
        compiled?.close()
    }

    private companion object {
        const val INITIAL_ARG_CAPACITY = 8
        val KEYWORD_REGEX = Regex("[A-Za-z]+")
    }
}
