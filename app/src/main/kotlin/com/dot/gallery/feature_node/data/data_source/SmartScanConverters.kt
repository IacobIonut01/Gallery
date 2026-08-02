/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data.data_source

import androidx.room.TypeConverter

object SmartScanConverters {
    @TypeConverter
    fun fromTrigger(value: SmartScanTrigger): String = value.storedValue

    @TypeConverter
    fun toTrigger(value: String): SmartScanTrigger =
        SmartScanTrigger.entries.first { it.storedValue == value }

    @TypeConverter
    fun fromStatus(value: SmartScanStatus): String = value.storedValue

    @TypeConverter
    fun toStatus(value: String): SmartScanStatus =
        SmartScanStatus.entries.first { it.storedValue == value }

    @TypeConverter
    fun fromPhase(value: SmartScanPhase): String = value.storedValue

    @TypeConverter
    fun toPhase(value: String): SmartScanPhase =
        SmartScanPhase.entries.first { it.storedValue == value }

    @TypeConverter
    fun fromFeature(value: MediaFeature): String = value.storedValue

    @TypeConverter
    fun toFeature(value: String): MediaFeature =
        MediaFeature.entries.first { it.storedValue == value }

    @TypeConverter
    fun fromFeatureStatus(value: MediaFeatureStatus): String = value.storedValue

    @TypeConverter
    fun toFeatureStatus(value: String): MediaFeatureStatus =
        MediaFeatureStatus.entries.first { it.storedValue == value }
}
