/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dot.gallery.core.ml.CutoutHelper.CutoutResult
import com.dot.gallery.core.ml.CutoutHelper.CutoutSession
import com.dot.gallery.core.ml.CutoutHelper.PromptPoint

@Stable
class CutoutState {
    // --- Observable state ---
    var session by mutableStateOf<CutoutSession?>(null)
    var promptPoints by mutableStateOf<List<PromptPoint>>(emptyList())
    var activeTool by mutableStateOf(ZoomablePagerImagePointTool.NONE)
    var result by mutableStateOf<CutoutResult?>(null)
    var isProcessing by mutableStateOf(false)
    var isRefining by mutableStateOf(false)

    // --- Internal state (not directly observed by UI, but affects transitions) ---
    private var history by mutableStateOf<List<List<PromptPoint>>>(emptyList())
    private var historyIndex by mutableStateOf(-1)
    private var resultCache by mutableStateOf<Pair<List<PromptPoint>, CutoutResult>?>(null)

    // --- State transitions ---
    val isActive: Boolean get() = session != null
    val canUndo: Boolean get() = historyIndex > 0
    val canRedo: Boolean get() = historyIndex < history.size - 1
    val hasResult: Boolean get() = result != null

    fun dismiss() {
        session?.close()
        session = null
        promptPoints = emptyList()
        history = emptyList()
        historyIndex = -1
        updateResult(null, null)
        activeTool = ZoomablePagerImagePointTool.NONE
    }

    fun initSession(newSession: CutoutSession, initialPoints: List<PromptPoint>) {
        session = newSession
        history = listOf(initialPoints)
        historyIndex = 0
        promptPoints = initialPoints
        activeTool = ZoomablePagerImagePointTool.ADD
    }

    fun pushPoints(newPoints: List<PromptPoint>) {
        val newHistory = history.take(historyIndex + 1) + listOf(newPoints)
        history = newHistory
        historyIndex = newHistory.size - 1
        promptPoints = newPoints
    }

    fun clearPoints() {
        val previousPoints = promptPoints
        pushPoints(emptyList())
        val newCache = result?.let { Pair(previousPoints, it) }
        updateResult(null, newCache)
    }

    // Returns a pair of (previousPoints, newPoints) if a decoder run is required, or null if it was a cache hit.
    fun navigateHistory(delta: Int): Pair<List<PromptPoint>, List<PromptPoint>>? {
        val newIndex = historyIndex + delta
        if (newIndex !in history.indices) return null

        val previousPoints = promptPoints
        historyIndex = newIndex
        val newPoints = history[newIndex]
        promptPoints = newPoints

        val cache = resultCache
        if (cache != null && cache.first == newPoints) {
            val currentRes = result
            updateResult(cache.second, currentRes?.let { Pair(previousPoints, it) })
            return null
        }
        return Pair(previousPoints, newPoints)
    }

    fun updateResult(newResult: CutoutResult?, newCache: Pair<List<PromptPoint>, CutoutResult>?) {
        val bitmapsToKeep = listOfNotNull(newResult?.bitmap, newCache?.second?.bitmap)
        result?.bitmap?.let { bmp ->
            if (bmp !in bitmapsToKeep) bmp.recycle()
        }
        resultCache?.second?.bitmap?.let { bmp ->
            if (bmp !in bitmapsToKeep) bmp.recycle()
        }
        result = newResult
        resultCache = newCache
    }
}

@Composable
fun rememberCutoutState(): CutoutState = remember { CutoutState() }
