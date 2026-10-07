/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

/** Callback invoked as a backup/restore progresses through a [BackupSection]. */
typealias BackupProgressListener = (section: BackupSection, current: Int, total: Int) -> Unit
