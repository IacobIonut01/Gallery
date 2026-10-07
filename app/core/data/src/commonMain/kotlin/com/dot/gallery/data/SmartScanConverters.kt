/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data

import androidx.room3.ColumnTypeConverter
import com.dot.gallery.data.entity.MediaFeature
import com.dot.gallery.data.entity.MediaFeatureStatus
import com.dot.gallery.data.entity.SmartScanPhase
import com.dot.gallery.data.entity.SmartScanStatus
import com.dot.gallery.data.entity.SmartScanTrigger


object SmartScanConverters {
    @ColumnTypeConverter
    fun fromTrigger(value: SmartScanTrigger): String = value.storedValue

    @ColumnTypeConverter
    fun toTrigger(value: String): SmartScanTrigger =
        SmartScanTrigger.entries.first { it.storedValue == value }

    @ColumnTypeConverter
    fun fromStatus(value: SmartScanStatus): String = value.storedValue

    @ColumnTypeConverter
    fun toStatus(value: String): SmartScanStatus =
        SmartScanStatus.entries.first { it.storedValue == value }

    @ColumnTypeConverter
    fun fromPhase(value: SmartScanPhase): String = value.storedValue

    @ColumnTypeConverter
    fun toPhase(value: String): SmartScanPhase =
        SmartScanPhase.entries.first { it.storedValue == value }

    @ColumnTypeConverter
    fun fromFeature(value: MediaFeature): String = value.storedValue

    @ColumnTypeConverter
    fun toFeature(value: String): MediaFeature =
        MediaFeature.entries.first { it.storedValue == value }

    @ColumnTypeConverter
    fun fromFeatureStatus(value: MediaFeatureStatus): String = value.storedValue

    @ColumnTypeConverter
    fun toFeatureStatus(value: String): MediaFeatureStatus =
        MediaFeatureStatus.entries.first { it.storedValue == value }
}
