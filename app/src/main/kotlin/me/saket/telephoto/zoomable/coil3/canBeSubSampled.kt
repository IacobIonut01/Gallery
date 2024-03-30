@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package me.saket.telephoto.zoomable.coil3

import android.annotation.SuppressLint
import android.util.TypedValue
import coil3.decode.DecodeUtils
import coil3.svg.isSvg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.telephoto.subsamplingimage.AssetImageSource
import me.saket.telephoto.subsamplingimage.FileImageSource
import me.saket.telephoto.subsamplingimage.RawImageSource
import me.saket.telephoto.subsamplingimage.ResourceImageSource
import me.saket.telephoto.subsamplingimage.SubSamplingImageSource
import me.saket.telephoto.subsamplingimage.UriImageSource
import okio.FileSystem
import okio.Source
import okio.buffer
import okio.source
import okio.use

context(Resolver)
internal suspend fun SubSamplingImageSource.canBeSubSampled(): Boolean {
    val preventSubSampling = when (this) {
        is ResourceImageSource -> isVectorDrawable()
        is AssetImageSource -> isSvgDecoderPresent() && isSvg()
        is UriImageSource -> isSvgDecoderPresent() && isSvg()
        is FileImageSource -> isSvgDecoderPresent() && isSvg(FileSystem.SYSTEM.source(path))
        is RawImageSource -> isSvgDecoderPresent() && isSvg(source.invoke())
    }
    return !preventSubSampling
}

context(Resolver)
private fun isSvgDecoderPresent(): Boolean {
    // Only available in this app
    return true
}

context(Resolver)
private fun ResourceImageSource.isVectorDrawable(): Boolean =
    TypedValue().apply {
        request.context.resources.getValue(id, this, /* resolveRefs = */ true)
    }.string.endsWith(".xml")

context(Resolver)
private suspend fun AssetImageSource.isSvg(): Boolean =
    isSvg(peek(request.context).source())

context(Resolver)
@SuppressLint("Recycle")
private suspend fun UriImageSource.isSvg(): Boolean =
    isSvg(peek(request.context).source())

private suspend fun isSvg(source: Source?): Boolean {
    return withContext(Dispatchers.IO) {
        source?.buffer()?.use { DecodeUtils.isSvg(source.buffer()) } == true
    }
}