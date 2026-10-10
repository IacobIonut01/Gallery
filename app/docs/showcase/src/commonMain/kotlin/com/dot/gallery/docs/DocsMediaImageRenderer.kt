/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.dot.gallery.core.presentation.components.MediaImageRenderer
import com.dot.gallery.docs.showcase.generated.resources.Res
import com.dot.gallery.docs.showcase.generated.resources.docs_earth
import com.dot.gallery.docs.showcase.generated.resources.docs_flower
import com.dot.gallery.docs.showcase.generated.resources.docs_flower_detail
import com.dot.gallery.docs.showcase.generated.resources.docs_lake
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Docs-only [MediaImageRenderer]: resolves fixture media/photo models to the
 * bundled real photographs (see ASSETS.md for licensing). `fixture://docs/…`
 * URIs and `/demo/…` paths are mapped by their filename — unknown models fall
 * back to the lake photo rather than a colored placeholder so a missed mapping
 * still reads as a real gallery cell.
 */
object DocsMediaImageRenderer : MediaImageRenderer {

    @Composable
    override fun RenderImage(
        modifier: Modifier,
        model: Any?,
        contentScale: ContentScale,
        contentDescription: String?,
        signature: Any?
    ) {
        Image(
            painter = painterResource(docsDrawableFor(model)),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

/** Model (uri string / path / Uri) → bundled drawable. Documented fallback: docs_lake. */
internal fun docsDrawableFor(model: Any?): DrawableResource {
    val label = model?.toString()?.substringAfterLast('/') ?: return Res.drawable.docs_lake
    return when (DocsGalleryData.drawableKeyByLabel[label]) {
        DocsGalleryData.DRAWABLE_EARTH -> Res.drawable.docs_earth
        DocsGalleryData.DRAWABLE_FLOWER -> Res.drawable.docs_flower
        DocsGalleryData.DRAWABLE_FLOWER_DETAIL -> Res.drawable.docs_flower_detail
        else -> Res.drawable.docs_lake
    }
}
