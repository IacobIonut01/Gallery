/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.domain.model

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.exifinterface.media.ExifInterface
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExifAttributes(
    var manufacturerName: String? = null,
    var modelName: String? = null,
    var apertureValue: Double? = null,
    var focalLength: Double? = null,
    var isoValue: Int? = null,
    var imageDescription: String? = null,
    var gpsLatLong: DoubleArray? = null
): Parcelable {

    fun writeExif(exif: ExifInterface) {
        manufacturerName?.let { exif.setAttribute(ExifInterface.TAG_MAKE, it) }
        modelName?.let { exif.setAttribute(ExifInterface.TAG_MODEL, it) }
        apertureValue?.let { exif.setAttribute(ExifInterface.TAG_APERTURE_VALUE, it.toString()) }
        focalLength?.let { exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, it.toString()) }
        isoValue?.let { exif.setAttribute(ExifInterface.TAG_ISO_SPEED, it.toString()) }
        imageDescription?.let { exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, it) }
        gpsLatLong?.let { exif.setLatLong(it[0], it[1]) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExifAttributes

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

    companion object {

        fun fromExifInterface(exifInterface: ExifInterface): ExifAttributes {
            val manufacturerName: String? =
                exifInterface.getAttribute(ExifInterface.TAG_MAKE)
            val modelName: String? =
                exifInterface.getAttribute(ExifInterface.TAG_MODEL)
            val apertureValue: Double =
                exifInterface.getAttributeDouble(ExifInterface.TAG_APERTURE_VALUE, 0.0)

            val focalLength: Double =
                exifInterface.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
            val isoValue: Int =
                exifInterface.getAttributeInt(ExifInterface.TAG_ISO_SPEED, 0)
            val imageDescription: String? =
                exifInterface.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)
            val gpsLatLong: DoubleArray? =
                exifInterface.latLong

            return ExifAttributes(
                manufacturerName, modelName, apertureValue, focalLength, isoValue, imageDescription, gpsLatLong
            )
        }
    }
}

@Composable
fun rememberExifAttributes(exifInterface: ExifInterface? = null) = remember {
    if (exifInterface != null) mutableStateOf(ExifAttributes.fromExifInterface(exifInterface))
    else mutableStateOf(ExifAttributes())
}