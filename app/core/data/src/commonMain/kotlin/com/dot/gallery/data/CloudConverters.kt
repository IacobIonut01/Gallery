/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data

import androidx.room3.ColumnTypeConverter
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SyncState
import com.dot.gallery.data.entity.FaceLinkKind


object CloudConverters {
    @ColumnTypeConverter
    fun fromFaceLinkKind(value: FaceLinkKind): String = value.storedValue

    @ColumnTypeConverter
    fun toFaceLinkKind(value: String): FaceLinkKind = FaceLinkKind.fromStored(value)

    @ColumnTypeConverter
    fun fromProviderType(value: ProviderType): String = value.name

    @ColumnTypeConverter
    fun toProviderType(value: String): ProviderType = ProviderType.valueOf(value)

    @ColumnTypeConverter
    fun fromSyncState(value: SyncState): String = value.name

    @ColumnTypeConverter
    fun toSyncState(value: String): SyncState = SyncState.valueOf(value)
}
