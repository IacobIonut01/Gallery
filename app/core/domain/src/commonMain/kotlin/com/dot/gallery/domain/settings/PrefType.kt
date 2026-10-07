/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.settings

import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Storage-kind tag so backends can pick their native key type. */
enum class PrefType { BOOLEAN, INT, LONG, FLOAT, DOUBLE, STRING, STRING_SET }
