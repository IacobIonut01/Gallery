/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.app.presentation.widget.data

import kotlinx.serialization.Serializable


@Serializable
data class WidgetData(
    val widgetId: Int,
    val type: WidgetType,
    val mediaUris: List<String>,
    /**
     * Parallel to [mediaUris]: the timeline-resolvable media id for the item
     * at that index, or null when the pick cannot be opened in the media
     * viewer (vault, private folder). Absent entirely in data stored by
     * versions before the deep-link fix — [WidgetDeepLink.resolveDeepLinkId]
     * falls back to parsing the media URI in that case.
     */
    val mediaIds: List<Long?> = emptyList(),
    /**
     * How the picked photos are drawn on the home screen. Absent in data
     * stored before display styles existed — defaults to [WidgetDisplayStyle.IMAGE].
     */
    val displayStyle: WidgetDisplayStyle = WidgetDisplayStyle.IMAGE,
    /**
     * Emoji or short label shown instead of the photo when
     * [displayStyle] is [WidgetDisplayStyle.ICON]. Null renders the photo.
     */
    val icon: String? = null
)

@Serializable
enum class WidgetType {
    SINGLE, GRID
}
