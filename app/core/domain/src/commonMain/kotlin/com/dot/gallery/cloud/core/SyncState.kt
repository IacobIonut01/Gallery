/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core


enum class SyncState {
    REMOTE_ONLY,
    DOWNLOADING,
    SYNCED,
    UPLOAD_PENDING,
    CONFLICT
}
