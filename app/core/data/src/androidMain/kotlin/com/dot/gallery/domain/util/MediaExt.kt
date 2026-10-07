package com.dot.gallery.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.dot.gallery.domain.model.Media
import com.github.panpf.zoomimage.subsampling.ContentImageSource
import com.github.panpf.zoomimage.subsampling.SubsamplingImage
import io.ktor.util.reflect.instanceOf
import kotlinx.serialization.json.Json
import java.util.UUID
import com.dot.gallery.domain.util.getUri



fun <T : Media> T.asSubsamplingImage(context: Context): SubsamplingImage {
    return SubsamplingImage(imageSource = ContentImageSource(context, getUri()))
}

fun <T : Media> T.compatibleBitmapFormat(): Bitmap.CompressFormat {
    return when (mimeType) {
        "image/jpeg" -> Bitmap.CompressFormat.JPEG
        "image/png" -> Bitmap.CompressFormat.PNG
        else -> Bitmap.CompressFormat.PNG
    }
}
