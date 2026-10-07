/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

/** Progress of a single [BackupSection]. [total] of 0 means "empty section" (instantly done). */
data class SectionProgress(val current: Int, val total: Int) {
    val done: Boolean get() = current >= total
}
