/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs.capture

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dot.gallery.docs.DocsScene

/**
 * Delegates to the shared docs scene host so captures render through the exact
 * same theme/store/navigation scaffolding as the wasmJs live embed.
 */
@Composable
fun DocsSceneHost(theme: String?, scene: DocsScene, state: String?, modifier: Modifier = Modifier) {
    com.dot.gallery.docs.DocsSceneHost(
        scene = scene,
        state = state,
        theme = theme,
        modifier = modifier,
    )
}
