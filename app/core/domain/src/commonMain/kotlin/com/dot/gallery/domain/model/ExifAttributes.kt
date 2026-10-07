/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.domain.model

import com.dot.gallery.domain.parcel.Parcelable
import com.dot.gallery.domain.parcel.Parcelize

@Parcelize
data class ExifAttributes(
    var manufacturerName: String? = null,
    var modelName: String? = null,
    var apertureValue: Double? = null,
    var focalLength: Double? = null,
    var isoValue: Int? = null,
    var imageDescription: String? = null,
    var gpsLatLong: DoubleArray? = null
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExifAttributes) return false

        if (manufacturerName != other.manufacturerName) return false
        if (modelName != other.modelName) return false
        if (apertureValue != other.apertureValue) return false
        if (focalLength != other.focalLength) return false
        if (isoValue != other.isoValue) return false
        if (imageDescription != other.imageDescription) return false
        if (gpsLatLong != null) {
            if (other.gpsLatLong == null) return false
            if (!gpsLatLong.contentEquals(other.gpsLatLong)) return false
        } else if (other.gpsLatLong != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = manufacturerName?.hashCode() ?: 0
        result = 31 * result + (modelName?.hashCode() ?: 0)
        result = 31 * result + (apertureValue?.hashCode() ?: 0)
        result = 31 * result + (focalLength?.hashCode() ?: 0)
        result = 31 * result + (isoValue ?: 0)
        result = 31 * result + (imageDescription?.hashCode() ?: 0)
        result = 31 * result + (gpsLatLong?.contentHashCode() ?: 0)
        return result
    }

    companion object
}
