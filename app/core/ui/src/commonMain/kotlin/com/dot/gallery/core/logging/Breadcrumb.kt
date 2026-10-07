/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.logging

import kotlinx.serialization.Serializable

/** A user action or navigation step recorded before a failure. */
@Serializable
data class Breadcrumb(
    val ts: Long,
    val label: String,
    val ctx: Map<String, String>? = null,
)
