/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

enum class MediaFeatureStatus(val storedValue: String) {
    PENDING("pending"),
    PROCESSING("processing"),
    SUCCEEDED("succeeded"),
    SKIPPED("skipped"),
    BLOCKED("blocked"),
    FAILED("failed")
}
