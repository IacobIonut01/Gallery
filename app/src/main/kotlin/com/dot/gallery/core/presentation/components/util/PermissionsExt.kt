package com.dot.gallery.core.presentation.components.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

enum class MediaAccessState {
    NONE,
    LIMITED,
    FULL,
}

fun resolveMediaAccessState(
    sdkInt: Int,
    imagesGranted: Boolean = false,
    videosGranted: Boolean = false,
    selectedMediaGranted: Boolean = false,
    legacyReadGranted: Boolean = false,
): MediaAccessState = when {
    sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && (imagesGranted || videosGranted) ->
        MediaAccessState.FULL
    sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && selectedMediaGranted ->
        MediaAccessState.LIMITED
    sdkInt >= Build.VERSION_CODES.TIRAMISU && (imagesGranted || videosGranted) ->
        MediaAccessState.FULL
    sdkInt < Build.VERSION_CODES.TIRAMISU && legacyReadGranted ->
        MediaAccessState.FULL
    else -> MediaAccessState.NONE
}

fun Context.mediaAccessState(): MediaAccessState = resolveMediaAccessState(
    sdkInt = Build.VERSION.SDK_INT,
    imagesGranted = isPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES),
    videosGranted = isPermissionGranted(Manifest.permission.READ_MEDIA_VIDEO),
    selectedMediaGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        isPermissionGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
    legacyReadGranted = isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE),
)

fun Context.hasMediaAccess(): Boolean = mediaAccessState() != MediaAccessState.NONE

fun Context.permissionGranted(list: List<String>): Boolean {
    val mediaPermissions = setOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        Manifest.permission.READ_EXTERNAL_STORAGE,
    )
    val containsMediaPermission = list.any { it in mediaPermissions }
    return (!containsMediaPermission || hasMediaAccess()) && list
        .filterNot { it in mediaPermissions || it == Manifest.permission.WRITE_EXTERNAL_STORAGE }
        .all(::isPermissionGranted)
}

private fun Context.isPermissionGranted(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
