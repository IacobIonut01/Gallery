/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey


/**
 * GLOBAL, per-album "delete from device after backup" preference.
 *
 * Unlike backup destination selection (which is per-account in `cloud_upload_pref`), removing the
 * local copy is a single decision per local album: an asset must only ever be deleted once it is
 * confirmed backed up to **every** cloud that album targets. Keeping this per-account would risk
 * deleting a file after it reached one cloud but not the others (data loss on fan-out).
 *
 * Absence of a row means "keep local copies" (the safe default).
 */
@Entity(tableName = "cloud_delete_local_pref")
data class CloudDeleteLocalPrefEntity(
    @PrimaryKey val albumId: Long,
    val enabled: Boolean = false,
    val albumLabel: String = ""
)
