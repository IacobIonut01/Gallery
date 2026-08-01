/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.image.thumbnail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * Phase 3 (#1076): scroll-aware two-stage thumbnail scheduling.
 *
 * A grid publishes a debounced "is the list currently in motion" flag through
 * [LocalThumbnailMotion]. The shared image renderer reads it to decide the request tier:
 * during motion it loads only the cheap MOTION tier (small, static — no full-size sibling and
 * no animation); once the list has been idle for [DEFAULT_IDLE_DEBOUNCE_MS] it loads the REFINED
 * tier at the cell size, reusing the already-cached MOTION bitmap as an instant placeholder.
 *
 * Null default means "treat as idle/refined" so any surface that does not publish motion state
 * keeps its previous full-quality behavior.
 */
val LocalThumbnailMotion = compositionLocalOf<State<Boolean>?> { null }

/** Idle debounce before a settled list is allowed to refine (tuned from device measurements). */
const val DEFAULT_IDLE_DEBOUNCE_MS = 200L

/**
 * Derive a debounced motion flag from a scroll container's `isScrollInProgress`.
 *
 * Motion turns on immediately when scrolling starts (so visible cells drop to the cheap tier
 * without delay) and turns off only after [idleDebounceMs] of no scrolling (so a brief pause
 * mid-fling does not thrash between tiers). [collectLatest] cancels the pending idle timer the
 * moment scrolling resumes.
 */
@Composable
fun rememberThumbnailMotionState(
    isScrollInProgress: () -> Boolean,
    idleDebounceMs: Long = DEFAULT_IDLE_DEBOUNCE_MS,
): State<Boolean> {
    val moving = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        snapshotFlow { isScrollInProgress() }.collectLatest { scrolling ->
            if (scrolling) {
                moving.value = true
            } else {
                delay(idleDebounceMs)
                moving.value = false
            }
        }
    }
    return moving
}
