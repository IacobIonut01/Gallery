/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.vault

import android.net.Uri
import com.dot.gallery.domain.model.Vault
import kotlinx.coroutines.flow.Flow

/** Vault worker operations — mapped to `VaultOperationWorker.OP_*` constants by the app impl. */
enum class VaultOp { ENCRYPT, DECRYPT, HIDE, MIGRATE }