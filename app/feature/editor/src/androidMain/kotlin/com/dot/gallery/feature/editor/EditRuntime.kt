/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.editor

import kotlinx.coroutines.flow.Flow

/**
 * App-side edit operations that need WorkManager. Emits one terminal result.
 */
interface EditRuntime {

    /** Enqueues a revert-to-original bake; emits the worker's success flag once. */
    fun revertEdit(mediaId: Long): Flow<Boolean>
}
