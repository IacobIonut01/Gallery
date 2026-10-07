/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity



enum class MetadataParsingPolicy {
    ON_DEMAND_COMPATIBLE,
    BULK_ISOLATED_ONLY
}

fun metadataParsingPolicy(bulk: Boolean): MetadataParsingPolicy =
    if (bulk) MetadataParsingPolicy.BULK_ISOLATED_ONLY else MetadataParsingPolicy.ON_DEMAND_COMPATIBLE
