/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.util.ext

import android.util.Size
import androidx.exifinterface.media.ExifInterface

private const val DEFAULT_VALUE_INT = -1
private const val DEFAULT_VALUE_DOUBLE = -1.0

fun ExifInterface.getAttributeInt(tag: String) =
    getAttributeInt(tag, DEFAULT_VALUE_INT).takeIf {
        it != DEFAULT_VALUE_INT
    }

fun ExifInterface.getAttributeDouble(tag: String): Double? =
    getAttributeDouble(tag, DEFAULT_VALUE_DOUBLE).takeIf {
        it != DEFAULT_VALUE_DOUBLE
    }

val ExifInterface.artist
    get() = getAttribute(ExifInterface.TAG_ARTIST)

val ExifInterface.apertureValue
    get() = getAttributeDouble(ExifInterface.TAG_APERTURE_VALUE)

val ExifInterface.copyright
    get() = getAttribute(ExifInterface.TAG_COPYRIGHT)

val ExifInterface.exposureTime
    get() = getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME)

val ExifInterface.isoSpeed
    get() = getAttributeInt(ExifInterface.TAG_ISO_SPEED)

val ExifInterface.focalLength
    get() = getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH)

val ExifInterface.make
    get() = getAttribute(ExifInterface.TAG_MAKE)

val ExifInterface.model
    get() = getAttribute(ExifInterface.TAG_MODEL)

val ExifInterface.pixelXDimension
    get() = getAttributeInt(ExifInterface.TAG_PIXEL_X_DIMENSION)

val ExifInterface.pixelYDimension
    get() = getAttributeInt(ExifInterface.TAG_PIXEL_Y_DIMENSION)

val ExifInterface.size
    get() = pixelXDimension?.let { x -> pixelYDimension?.let { y -> Size(x, y) } }

val ExifInterface.software
    get() = getAttribute(ExifInterface.TAG_SOFTWARE)

var ExifInterface.userComment
    get() = getAttribute(ExifInterface.TAG_USER_COMMENT)
    set(value) {
        setAttribute(ExifInterface.TAG_USER_COMMENT, value)
    }

val ExifInterface.isSupportedFormatForSavingAttributes: Boolean
    get() {
        val mimeType = ExifInterface::class.java.getDeclaredField("mMimeType").apply {
            isAccessible = true
        }.get(this) as Int

        val isSupportedFormatForSavingAttributes = ExifInterface::class.java.getDeclaredMethod(
            "isSupportedFormatForSavingAttributes", Int::class.java
        ).apply {
            isAccessible = true
        }

        return isSupportedFormatForSavingAttributes.invoke(null, mimeType) as Boolean
    }