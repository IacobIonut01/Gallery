/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import androidx.room3.Entity
import com.dot.gallery.domain.parcel.Parcelable
import com.dot.gallery.domain.parcel.Parcelize
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Parcelize
@Serializable
@Entity(tableName = "vaults", primaryKeys = ["uuid"])
data class Vault(
    // kotlin.uuid.Uuid serializes as the same canonical string java.util.UUID
    // produced, so existing vault rows and JSON keep working.
    val uuid: Uuid = Uuid.random(),
    val name: String
) : Parcelable
