/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs.capture

/**
 * Device presets the capture tier renders each scene under (DOCS_SITE_PLAN.md
 * §6-§7). The `id` vocabulary matches `DevicePreset` in :docs-showcase so live
 * and capture tiers speak one device language; the dimensions are the plan's
 * capture spec (phone 412x915dp, tablet 840x1180dp, foldable canvas matching
 * the showcase's unfolded stage).
 */
enum class CapturePreset(val id: String, val widthDp: Int, val heightDp: Int) {
    PHONE("phone", 412, 915),
    TABLET("tablet", 840, 1180),
    FOLDABLE("foldable", 768, 932);

    companion object {
        /**
         * Null/blank selects every preset; a comma-separated id list selects a
         * subset. Unknown ids fail fast.
         */
        fun parse(filter: String?): List<CapturePreset> {
            if (filter.isNullOrBlank()) return entries.toList()
            val wanted = filter.split(',').map { it.trim().lowercase() }
            val parsed = wanted.map { id ->
                requireNotNull(entries.firstOrNull { it.id == id }) {
                    "Unknown device preset '$id'. Known: ${entries.joinToString { it.id }}"
                }
            }
            return parsed.distinct()
        }
    }
}
