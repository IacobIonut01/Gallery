package com.dot.gallery.feature_node.presentation.frameextract

import kotlin.math.roundToInt
import kotlin.math.roundToLong

class FrameTimeline private constructor(
    val durationUs: Long,
    val frameCount: Int?,
    val frameRate: Float?,
    private val timestampsUs: LongArray?,
) {
    val usesFrameIndexes: Boolean
        get() = timestampsUs == null && frameCount != null && frameCount > 0 && frameRate != null && frameRate > 0f

    val first: FrameIdentity
        get() = identityForIndex(0)

    val last: FrameIdentity
        get() = when {
            timestampsUs != null -> FrameIdentity(timestampsUs.lastIndex, timestampsUs.last())
            frameCount != null && frameCount > 0 -> identityForIndex(frameCount - 1)
            else -> FrameIdentity(-1, durationUs.coerceAtLeast(0L))
        }

    fun closest(timeUs: Long): FrameIdentity {
        val clamped = timeUs.coerceIn(0L, durationUs.coerceAtLeast(0L))
        timestampsUs?.let { timestamps ->
            if (timestamps.isEmpty()) return FrameIdentity(-1, clamped)
            val index = timestamps.binarySearch(clamped)
            if (index >= 0) return FrameIdentity(index, timestamps[index])
            val insertion = -index - 1
            val before = (insertion - 1).coerceAtLeast(0)
            val after = insertion.coerceAtMost(timestamps.lastIndex)
            val resolved = if (clamped - timestamps[before] <= timestamps[after] - clamped) before else after
            return FrameIdentity(resolved, timestamps[resolved])
        }
        if (usesFrameIndexes) {
            val index = ((clamped / 1_000_000.0) * frameRate!!).roundToInt()
                .coerceIn(0, frameCount!! - 1)
            return identityForIndex(index)
        }
        return FrameIdentity(-1, clamped)
    }

    fun step(current: FrameIdentity, delta: Int): FrameIdentity {
        timestampsUs?.let { timestamps ->
            if (timestamps.isEmpty()) return current
            val index = if (current.frameIndex in timestamps.indices) current.frameIndex
                else closest(current.presentationTimeUs).frameIndex
            val target = (index + delta).coerceIn(0, timestamps.lastIndex)
            return FrameIdentity(target, timestamps[target])
        }
        if (usesFrameIndexes) {
            val index = if (current.frameIndex >= 0) current.frameIndex
                else closest(current.presentationTimeUs).frameIndex
            return identityForIndex((index + delta).coerceIn(0, frameCount!! - 1))
        }
        val nominalStepUs = frameRate?.takeIf { it > 0f }
            ?.let { (1_000_000.0 / it).roundToLong() }
            ?: DEFAULT_UNKNOWN_FRAME_STEP_US
        return closest(current.presentationTimeUs + nominalStepUs * delta)
    }

    fun filmstrip(center: FrameIdentity, count: Int): List<FrameIdentity> {
        if (count <= 0) return emptyList()
        if (timestampsUs != null || usesFrameIndexes) {
            val centerIndex = if (center.frameIndex >= 0) center.frameIndex else closest(center.presentationTimeUs).frameIndex
            val total = timestampsUs?.size ?: frameCount ?: 0
            if (total <= count) return (0 until total).map(::identityForIndex)
            val start = (centerIndex - count / 2).coerceIn(0, total - count)
            return (start until start + count).map(::identityForIndex)
        }
        if (durationUs <= 0L) return listOf(center)
        val windowUs = minOf(durationUs, (durationUs / 8L).coerceAtLeast(1_000_000L))
        val start = (center.presentationTimeUs - windowUs / 2L).coerceIn(0L, (durationUs - windowUs).coerceAtLeast(0L))
        val step = if (count == 1) 0L else windowUs / (count - 1)
        return List(count) { closest(start + step * it) }.distinct()
    }

    private fun identityForIndex(index: Int): FrameIdentity {
        timestampsUs?.let { return FrameIdentity(index, it[index]) }
        val timeUs = frameRate?.takeIf { it > 0f }
            ?.let { ((index / it) * 1_000_000.0).roundToLong() }
            ?: 0L
        return FrameIdentity(index, timeUs.coerceIn(0L, durationUs.coerceAtLeast(0L)))
    }

    companion object {
        private const val DEFAULT_UNKNOWN_FRAME_STEP_US = 33_333L

        fun constant(durationUs: Long, frameCount: Int, frameRate: Float): FrameTimeline =
            FrameTimeline(durationUs, frameCount.coerceAtLeast(1), frameRate, null)

        fun variable(durationUs: Long, timestampsUs: Collection<Long>): FrameTimeline {
            val normalized = timestampsUs.asSequence()
                .filter { it >= 0L }
                .distinct()
                .sorted()
                .toList()
                .toLongArray()
            return FrameTimeline(durationUs, normalized.size.takeIf { it > 0 }, null, normalized)
        }

        fun unknown(durationUs: Long, nominalFrameRate: Float? = null): FrameTimeline =
            FrameTimeline(durationUs, null, nominalFrameRate, null)
    }
}
