package io.sanghun.compose.video

import androidx.media3.common.Player

/**
 * Convert [RepeatMode] to exoplayer repeat mode.
 *
 * @return [Player.REPEAT_MODE_ALL] or [Player.REPEAT_MODE_OFF] or [Player.REPEAT_MODE_ONE] or
 */
internal fun RepeatMode.toExoPlayerRepeatMode(): Int =
    when (this) {
        RepeatMode.NONE -> Player.REPEAT_MODE_OFF
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    }