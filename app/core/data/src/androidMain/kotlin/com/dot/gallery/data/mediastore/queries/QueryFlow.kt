/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.data.mediastore.queries

import android.database.Cursor
import kotlinx.coroutines.flow.Flow
import com.dot.gallery.core.util.Query


/**
 * Query flow
 *
 * This class is responsible for fetching data with a cursor
 *
 * @param T
 * @constructor Create empty Query flow
 */
abstract class QueryFlow<T> {

    /** A flow of the data specified by the query */
    abstract fun flowData(): Flow<List<T>>

    /** A flow of the cursor specified by the query */
    abstract fun flowCursor(): Flow<Cursor?>
}