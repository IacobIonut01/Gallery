@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package me.saket.telephoto.zoomable.coil3

import android.annotation.SuppressLint
import android.util.TypedValue
import coil.decode.DecodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.telephoto.subsamplingimage.AssetImageSource
import me.saket.telephoto.subsamplingimage.FileImageSource
import me.saket.telephoto.subsamplingimage.RawImageSource
import me.saket.telephoto.subsamplingimage.ResourceImageSource
import me.saket.telephoto.subsamplingimage.SubSamplingImageSource
import me.saket.telephoto.subsamplingimage.UriImageSource
import me.saket.telephoto.zoomable.coil.isSvg
import okio.FileSystem
import okio.Source
import okio.buffer
import okio.source

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
  // Searching for coil's SvgDecoder by name isn't the best idea,
  // but it'll prevent opening of bitmap sources and inspecting
  // them for SVGs for projects that don't need SVGs.
  return imageLoader.components.decoderFactories.any {
    it::class.qualifiedName?.contains("svg", ignoreCase = true) == true
  }
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
    source?.buffer()?.use(DecodeUtils::isSvg) == true
  }
}