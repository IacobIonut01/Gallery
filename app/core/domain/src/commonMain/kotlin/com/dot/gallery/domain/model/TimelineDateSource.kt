/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TimelineDateSource {
    CAPTURE_TIME,
    MODIFIED_TIME
}
