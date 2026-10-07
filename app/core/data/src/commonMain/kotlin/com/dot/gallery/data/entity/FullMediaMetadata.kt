/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.Embedded
import androidx.room3.Relation

data class FullMediaMetadata(
    @Embedded val core: MediaMetadataCore,
    @Relation(
        parentColumns = ["mediaId"],
        entityColumns = ["mediaId"]
    ) val video: MediaMetadataVideo?,
    @Relation(
        parentColumns = ["mediaId"],
        entityColumns = ["mediaId"]
    ) val flags: MediaMetadataFlags?
)
