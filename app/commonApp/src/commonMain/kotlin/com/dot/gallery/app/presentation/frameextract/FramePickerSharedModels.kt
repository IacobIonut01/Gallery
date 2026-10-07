package com.dot.gallery.app.presentation.frameextract

import com.dot.gallery.domain.parcel.Parcelable
import com.dot.gallery.domain.parcel.Parcelize

@Parcelize
enum class FrameSourceKind : Parcelable {
    LOCAL,
    DOCUMENT,
    CLOUD,
    VAULT
}

@Parcelize
data class FrameIdentity(
    val frameIndex: Int,
    val presentationTimeUs: Long,
) : Parcelable, Comparable<FrameIdentity> {
    override fun compareTo(other: FrameIdentity): Int =
        compareValuesBy(this, other, FrameIdentity::presentationTimeUs, FrameIdentity::frameIndex)

    fun encode(): String = "$frameIndex:$presentationTimeUs"

    companion object {
        fun decode(value: String): FrameIdentity? {
            val separator = value.indexOf(':')
            if (separator <= 0) return null
            val frameIndex = value.substring(0, separator).toIntOrNull() ?: return null
            val presentationTimeUs = value.substring(separator + 1).toLongOrNull() ?: return null
            return FrameIdentity(frameIndex, presentationTimeUs)
        }
    }
}

data class FrameVideoMetadata(
    val durationUs: Long,
    val frameCount: Int?,
    val frameRate: Float?,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val isConstantFrameRate: Boolean,
    val isHdr: Boolean,
    val isDolbyVision: Boolean = false,
)

sealed interface SelectionChange {
    data class Updated(val frames: List<FrameIdentity>) : SelectionChange
    data object LimitReached : SelectionChange
}

object VideoLocationParser {
    private val pattern = Regex("^([+-]\\d{2,3}(?:\\.\\d+)?)([+-]\\d{3}(?:\\.\\d+)?)(?:[+-]\\d+(?:\\.\\d+)?)?/?$")

    fun parse(value: String?): Pair<Double, Double>? {
        val match = value?.trim()?.let(pattern::matchEntire) ?: return null
        val latitude = match.groupValues[1].toDoubleOrNull() ?: return null
        val longitude = match.groupValues[2].toDoubleOrNull() ?: return null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return latitude to longitude
    }
}

object FrameSelectionReducer {
    const val MAX_SELECTION = 50

    fun toggle(
        selection: Collection<FrameIdentity>,
        frame: FrameIdentity,
    ): SelectionChange {
        if (frame in selection) {
            return SelectionChange.Updated(selection.filterNot { it == frame }.sorted())
        }
        if (selection.size >= MAX_SELECTION) return SelectionChange.LimitReached
        return SelectionChange.Updated((selection + frame).distinct().sorted())
    }

    fun restore(values: Collection<String>): List<FrameIdentity> =
        values.mapNotNull(FrameIdentity::decode)
            .distinct()
            .sorted()
            .take(MAX_SELECTION)
}
