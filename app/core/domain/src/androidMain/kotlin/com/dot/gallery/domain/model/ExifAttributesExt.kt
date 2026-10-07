/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.domain.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.exifinterface.media.ExifInterface

fun ExifAttributes.writeExif(exif: ExifInterface) {
    exif.setAttribute(ExifInterface.TAG_MAKE, manufacturerName)
    exif.setAttribute(ExifInterface.TAG_MODEL, modelName)
    exif.setAttribute(ExifInterface.TAG_APERTURE_VALUE, apertureValue?.toString())
    exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, focalLength?.toString())
    exif.setAttribute(ExifInterface.TAG_ISO_SPEED, isoValue?.toString())
    exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, imageDescription)
    if (gpsLatLong != null) {
        exif.setLatLong(gpsLatLong!![0], gpsLatLong!![1])
    } else {
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null)
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null)
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
    }
}

fun ExifAttributes.Companion.fromExifInterface(exifInterface: ExifInterface): ExifAttributes {
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
        manufacturerName,
        modelName,
        apertureValue,
        focalLength,
        isoValue,
        imageDescription,
        gpsLatLong
    )
}

@Composable
fun rememberExifAttributes(exifInterface: ExifInterface? = null) = rememberSaveable {
    if (exifInterface != null) mutableStateOf(ExifAttributes.fromExifInterface(exifInterface))
    else mutableStateOf(ExifAttributes())
}
