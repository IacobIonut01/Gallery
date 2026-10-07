/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.request.ComposableImageRequest

/**
 * KMP [MediaImageRenderer] backed by Sketch 4 — the shared implementation for
 * wasmJs/docs and the eventual Android Glide replacement (K5).
 *
 * [signature] rides as a request extra: Sketch factors extras into the request
 * key, so it invalidates the decoded bitmap the same way Glide's `ObjectKey`
 * signature did — change the signature, get a fresh decode.
 */
val SketchMediaImageRenderer = object : MediaImageRenderer {
    @Composable
    override fun RenderImage(
        modifier: Modifier,
        model: Any?,
        contentScale: ContentScale,
        contentDescription: String?,
        signature: Any?
    ) {
        AsyncImage(
            request = ComposableImageRequest(model?.toString()) {
                if (signature != null) {
                    setExtra(key = "thumbnailSignature", value = signature.toString())
                }
            },
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}
