/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif

import com.dot.gallery.domain.model.Media
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/** Work-item lifecycle state, decoupled from WorkManager's enum. */
enum class CopyWorkState { ENQUEUED, RUNNING, BLOCKED, SUCCEEDED, FAILED, CANCELLED }