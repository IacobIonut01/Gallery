/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

/** Primitive type expected for a portable user preference. */
enum class PortableSettingType(val code: String) {
    BOOLEAN("b"),
    INT("i"),
    LONG("l"),
    FLOAT("f"),
    DOUBLE("d"),
    STRING("s"),
    STRING_SET("ss")
}
