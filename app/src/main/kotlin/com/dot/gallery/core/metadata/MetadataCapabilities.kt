package com.dot.gallery.core.metadata

object MetadataCapabilities {
    private val commonRasterModes = MetadataRemovalMode.entries.toSet()

    fun forFormat(format: MediaContainerFormat): SanitizationCapability = when (format) {
        MediaContainerFormat.JPEG,
        MediaContainerFormat.PNG,
        MediaContainerFormat.WEBP -> SanitizationCapability(format, commonRasterModes)
        MediaContainerFormat.GIF -> SanitizationCapability(
            format,
            setOf(MetadataRemovalMode.PRIVACY, MetadataRemovalMode.EVERYTHING)
        )
        MediaContainerFormat.BMP -> SanitizationCapability(
            format,
            emptySet(),
            limitation = "No safely removable embedded metadata was found for BMP."
        )
        MediaContainerFormat.TIFF -> SanitizationCapability(
            format,
            emptySet(),
            limitation = "TIFF and camera RAW metadata cannot yet be rebuilt safely."
        )
        MediaContainerFormat.JP2,
        MediaContainerFormat.JXL -> SanitizationCapability(
            format,
            setOf(MetadataRemovalMode.EVERYTHING),
            limitation = "Selective metadata removal is not available for this box container."
        )
        MediaContainerFormat.PSD,
        MediaContainerFormat.J2K,
        MediaContainerFormat.HEIF,
        MediaContainerFormat.AVIF,
        MediaContainerFormat.SVG -> SanitizationCapability(
            format,
            emptySet(),
            limitation = "Lossless metadata removal is not available for this image container yet."
        )
        MediaContainerFormat.MP4,
        MediaContainerFormat.QUICKTIME,
        MediaContainerFormat.THREE_GPP,
        MediaContainerFormat.MATROSKA,
        MediaContainerFormat.WEBM,
        MediaContainerFormat.AVI,
        MediaContainerFormat.MPEG_TS,
        MediaContainerFormat.OGG -> SanitizationCapability(
            format,
            emptySet(),
            limitation = "The lossless video remux component is not available in this build."
        )
        MediaContainerFormat.UNKNOWN -> SanitizationCapability(
            format,
            emptySet(),
            limitation = "The media container could not be identified."
        )
    }
}
