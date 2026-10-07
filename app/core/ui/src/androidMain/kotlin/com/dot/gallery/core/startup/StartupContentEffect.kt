/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.startup

import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import com.dot.gallery.core.metrics.StartupTracer


@Composable
fun StartupContentEffect(
    route: String,
    ready: Boolean,
    releaseGate: Boolean = true,
    committedLabel: String? = null,
    drawnLabel: String? = null,
    onContentDrawn: (() -> Unit)? = null,
) {
    val view = LocalView.current
    val gate = LocalStartupWorkGate.current ?: return
    val currentOnContentDrawn by rememberUpdatedState(onContentDrawn)
    DisposableEffect(view, gate, route, ready, releaseGate, committedLabel, drawnLabel) {
        if (!ready) return@DisposableEffect onDispose { }
        var active = true
        var requested = false
        val listener = object : ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                if (requested) return
                requested = true
                view.post {
                    if (view.viewTreeObserver.isAlive) {
                        view.viewTreeObserver.removeOnDrawListener(this)
                    }
                }
                if (view.isHardwareAccelerated) {
                    val committed = Runnable {
                        if (active) {
                            StartupTracer.trace(
                                committedLabel ?: "Startup.contentFrameCommitted($route)"
                            ) { }
                            if (releaseGate) gate.onContentDrawn()
                            currentOnContentDrawn?.invoke()
                        }
                    }
                    view.viewTreeObserver.registerFrameCommitCallback(committed)
                } else {
                    view.post {
                        if (active) {
                            StartupTracer.trace(
                                drawnLabel ?: "Startup.contentDrawn($route)"
                            ) { }
                            if (releaseGate) gate.onContentDrawn()
                            currentOnContentDrawn?.invoke()
                        }
                    }
                }
            }
        }
        view.viewTreeObserver.addOnDrawListener(listener)
        onDispose {
            active = false
            if (view.viewTreeObserver.isAlive) {
                view.viewTreeObserver.removeOnDrawListener(listener)
            }
        }
    }
}
