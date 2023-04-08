package com.dot.gallery.feature_node.presentation.util

import androidx.exifinterface.media.ExifInterface

class ExifMetadata(exifInterface: ExifInterface) {
    val modelName: String? =
        exifInterface.getAttribute(ExifInterface.TAG_MODEL)
    val apertureValue: Double =
        exifInterface.getAttributeDouble(ExifInterface.TAG_APERTURE_VALUE, 0.0)

    /** TODO: Display a usable shutter speed value*/
    val shutterSpeedValue: Double =
        exifInterface.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
    val focalLength: Double =
        exifInterface.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
    val isoValue: Int =
        exifInterface.getAttributeInt(ExifInterface.TAG_ISO_SPEED, 0)
    val imageWidth: String? =
        exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
    val imageHeight: String? =
        exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)

    /**
     * 0 - latitude
     * 1 - longitude
     */
    val gpsLatLong: DoubleArray? =
        exifInterface.latLong
}