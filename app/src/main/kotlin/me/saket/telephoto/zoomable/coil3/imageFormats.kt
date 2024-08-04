package me.saket.telephoto.zoomable.coil3

import coil.decode.DecodeUtils
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

private val SVG_TAG: ByteString = "<svg".encodeUtf8()
private val LEFT_ANGLE_BRACKET: ByteString = "<".encodeUtf8()

// https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
private val GIF_HEADER_87A = "GIF87a".encodeUtf8()
private val GIF_HEADER_89A = "GIF89a".encodeUtf8()

/**
 * Copied from coil-svg.
 *
 * Return 'true' if the [source] contains an SVG image. The [source] is not consumed.
 *
 * NOTE: There's no guaranteed method to determine if a byte stream is an SVG without attempting
 * to decode it. This method uses heuristics.
*/
@Suppress("UnusedReceiverParameter")
internal fun DecodeUtils.isSvg(source: BufferedSource): Boolean {
  return source.rangeEquals(0, LEFT_ANGLE_BRACKET) &&
    source.indexOf(SVG_TAG, 0, 1024) != -1L
}

/**
 * Copied from coil-gif.
 *
 * Return 'true' if the [source] contains a GIF image. The [source] is not consumed.
 */
@Suppress("UnusedReceiverParameter")
internal fun DecodeUtils.isGif(source: BufferedSource): Boolean {
  return source.rangeEquals(0, GIF_HEADER_89A) ||
    source.rangeEquals(0, GIF_HEADER_87A)
}

/** Copied from coil-svg. */
internal fun BufferedSource.indexOf(bytes: ByteString, fromIndex: Long, toIndex: Long): Long {
  require(bytes.size > 0) { "bytes is empty" }

  val firstByte = bytes[0]
  val lastIndex = toIndex - bytes.size
  var currentIndex = fromIndex
  while (currentIndex < lastIndex) {
    currentIndex = indexOf(firstByte, currentIndex, lastIndex)
    if (currentIndex == -1L || rangeEquals(currentIndex, bytes)) {
      return currentIndex
    }
    currentIndex++
  }
  return -1
}