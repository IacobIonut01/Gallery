/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity



fun MediaMetadata.toCore() = MediaMetadataCore(
    mediaId = mediaId,
    imageDescription = imageDescription,
    dateTimeOriginal = dateTimeOriginal,
    manufacturerName = manufacturerName,
    modelName = modelName,
    lensModel = lensModel,
    aperture = aperture,
    exposureTime = exposureTime,
    iso = iso,
    focalLength = focalLength,
    gpsLatitude = gpsLatitude,
    gpsLongitude = gpsLongitude,
    gpsLocationName = gpsLocationName,
    gpsLocationNameCountry = gpsLocationNameCountry,
    gpsLocationNameCity = gpsLocationNameCity,
    imageWidth = imageWidth,
    imageHeight = imageHeight,
    imageResolutionX = imageResolutionX,
    imageResolutionY = imageResolutionY,
    resolutionUnit = resolutionUnit
)

fun MediaMetadata.toVideo() = MediaMetadataVideo(
    mediaId = mediaId,
    durationMs = durationMs,
    videoWidth = videoWidth,
    videoHeight = videoHeight,
    frameRate = frameRate,
    bitRate = bitRate
)

fun MediaMetadata.toFlags() = MediaMetadataFlags(
    mediaId = mediaId,
    isNightMode = isNightMode,
    isPanorama = isPanorama,
    isPhotosphere = isPhotosphere,
    isLongExposure = isLongExposure,
    isMotionPhoto = isMotionPhoto
)

fun FullMediaMetadata.toMediaMetadata(): MediaMetadata {
    val v = video
    val f = flags
    return MediaMetadata(
        mediaId = core.mediaId,
        imageDescription = core.imageDescription,
        dateTimeOriginal = core.dateTimeOriginal,
        manufacturerName = core.manufacturerName,
        modelName = core.modelName,
        lensModel = core.lensModel,
        aperture = core.aperture,
        exposureTime = core.exposureTime,
        iso = core.iso,
        focalLength = core.focalLength,
        gpsLatitude = core.gpsLatitude,
        gpsLongitude = core.gpsLongitude,
        gpsLocationName = core.gpsLocationName,
        gpsLocationNameCountry = core.gpsLocationNameCountry,
        gpsLocationNameCity = core.gpsLocationNameCity,
        imageWidth = core.imageWidth,
        imageHeight = core.imageHeight,
        imageResolutionX = core.imageResolutionX,
        imageResolutionY = core.imageResolutionY,
        resolutionUnit = core.resolutionUnit,
        durationMs = v?.durationMs,
        videoWidth = v?.videoWidth,
        videoHeight = v?.videoHeight,
        frameRate = v?.frameRate,
        bitRate = v?.bitRate,
        isNightMode = f?.isNightMode ?: false,
        isPanorama = f?.isPanorama ?: false,
        isPhotosphere = f?.isPhotosphere ?: false,
        isLongExposure = f?.isLongExposure ?: false,
        isMotionPhoto = f?.isMotionPhoto ?: false
    )
}
