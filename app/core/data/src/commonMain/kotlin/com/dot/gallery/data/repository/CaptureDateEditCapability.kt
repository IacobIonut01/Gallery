/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.repository

import com.dot.gallery.domain.uri.Uri

enum class CaptureDateEditCapability {
    DIRECT_WRITE,
    SAFE_COPY,
    COPY_ONLY,
    UNSUPPORTED
}