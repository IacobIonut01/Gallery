/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core.capabilities

import com.dot.gallery.cloud.core.MediaCapabilityProvider


/** A server-side tag/label as reported by a [TagsCapableProvider]. */
data class CloudTagInfo(
    val tagId: String,
    val name: String,
    val value: String,
    val color: String? = null
)