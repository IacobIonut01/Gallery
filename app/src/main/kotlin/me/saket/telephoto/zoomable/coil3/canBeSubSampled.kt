@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package me.saket.telephoto.zoomable.coil3

import android.annotation.SuppressLint
import android.util.TypedValue
import coil3.decode.DecodeUtils
import coil3.gif.isGif
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
    return withContext(Dispatchers.IO) {
        when (this@canBeSubSampled) {
            is ResourceImageSource -> !isVectorDrawable()
            is AssetImageSource -> canBeSubSampled()
            is UriImageSource -> canBeSubSampled()
            is FileImageSource -> canBeSubSampled(FileSystem.SYSTEM.source(path))
            is RawImageSource -> canBeSubSampled(source.invoke())
        }
    }
}

context(Resolver)
private fun ResourceImageSource.isVectorDrawable(): Boolean =
    TypedValue().apply {
        request.context.resources.getValue(id, this, /* resolveRefs = */ true)
    }.string.endsWith(".xml")

context(Resolver)
private fun AssetImageSource.canBeSubSampled(): Boolean =
    canBeSubSampled(peek(request.context).source())

context(Resolver)
@SuppressLint("Recycle")
private fun UriImageSource.canBeSubSampled(): Boolean =
    canBeSubSampled(peek(request.context).source())

private fun canBeSubSampled(source: Source): Boolean {
    return source.buffer().use {
        // Check for GIFs as well because Android's ImageDecoder can return a Bitmap for single-frame GIFs.
        !DecodeUtils.isSvg(it) && !DecodeUtils.isGif(it)
    }
}