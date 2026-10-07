/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import com.dot.gallery.core.logging.LogEntry

sealed class SettingsEntity(
    open val icon: ImageVector? = null,
    open val iconUri: String? = null,
    open val iconRes: Int? = null,
    open val title: String,
    open val titleAnnotated: AnnotatedString? = null,
    open val summary: String? = null,
    open val summaryAnnotated: AnnotatedString? = null,
    open val rightText: String? = null,
    open val rightTextAnnotated: AnnotatedString? = null,
    val type: PreferenceType = PreferenceType.Default,
    open val enabled: Boolean = true,
    open val isChecked: Boolean? = null,
    open val onCheck: ((Boolean) -> Unit)? = null,
    open val onClick: (() -> Unit)? = null,
    open val onLongClick: (() -> Unit)? = null,
    open val onSwipeToDelete: (() -> Unit)? = null,
    open val minValue: Float? = null,
    open val currentValue: Float? = null,
    open val maxValue: Float? = null,
    open val step: Int = 1,
    open val valueMultiplier: Int = 1,
    open val seekSuffix: String? = null,
    open val onSeek: ((Float) -> Unit)? = null,
    open val screenPosition: Position = Position.Alone,
    open val horizontalLayout: Boolean = false,
    open val tag: Any? = null,
) {
    val isHeader = type == PreferenceType.Header

    @Stable
    data class Header(
        override val title: String,
        override val titleAnnotated: AnnotatedString? = null,
    ) : SettingsEntity(
        title = title,
        titleAnnotated = titleAnnotated,
        type = PreferenceType.Header
    )

    @Stable
    data class Preference(
        override val icon: ImageVector? = null,
        override val iconUri: String? = null,
        override val iconRes: Int? = null,
        override val title: String,
        override val titleAnnotated: AnnotatedString? = null,
        override val summary: String? = null,
        override val summaryAnnotated: AnnotatedString? = null,
        override val rightText: String? = null,
        override val rightTextAnnotated: AnnotatedString? = null,
        override val enabled: Boolean = true,
        override val screenPosition: Position = Position.Alone,
        override val horizontalLayout: Boolean = false,
        override val onClick: (() -> Unit)? = null,
        override val onLongClick: (() -> Unit)? = null,
        override val onSwipeToDelete: (() -> Unit)? = null,
        override val tag: Any? = null,
    ) : SettingsEntity(
        icon = icon,
        iconUri = iconUri,
        iconRes = iconRes,
        title = title,
        titleAnnotated = titleAnnotated,
        summary = summary,
        summaryAnnotated = summaryAnnotated,
        rightText = rightText,
        rightTextAnnotated = rightTextAnnotated,
        enabled = enabled,
        screenPosition = screenPosition,
        horizontalLayout = horizontalLayout,
        onClick = onClick,
        onLongClick = onLongClick,
        onSwipeToDelete = onSwipeToDelete,
        tag = tag,
        type = PreferenceType.Default
    )

    @Stable
    data class PreferenceExtra(
        override val icon: ImageVector? = null,
        override val iconUri: String? = null,
        override val iconRes: Int? = null,
        override val title: String,
        override val titleAnnotated: AnnotatedString? = null,
        override val summary: String? = null,
        override val summaryAnnotated: AnnotatedString? = null,
        override val rightText: String? = null,
        override val rightTextAnnotated: AnnotatedString? = null,
        override val enabled: Boolean = true,
        override val screenPosition: Position = Position.Alone,
        override val horizontalLayout: Boolean = false,
        override val onClick: (() -> Unit)? = null,
    ) : SettingsEntity(
        icon = icon,
        iconUri = iconUri,
        iconRes = iconRes,
        title = title,
        titleAnnotated = titleAnnotated,
        summary = summary,
        summaryAnnotated = summaryAnnotated,
        rightText = rightText,
        rightTextAnnotated = rightTextAnnotated,
        enabled = enabled,
        screenPosition = screenPosition,
        horizontalLayout = horizontalLayout,
        onClick = onClick,
        type = PreferenceType.Default
    )

    @Stable
    data class SwitchPreference(
        override val icon: ImageVector? = null,
        override val iconUri: String? = null,
        override val iconRes: Int? = null,
        override val title: String,
        override val titleAnnotated: AnnotatedString? = null,
        override val summary: String? = null,
        override val summaryAnnotated: AnnotatedString? = null,
        override val enabled: Boolean = true,
        override val screenPosition: Position = Position.Alone,
        override val isChecked: Boolean = false,
        override val onCheck: ((Boolean) -> Unit)? = null,
        override val onClick: (() -> Unit)? = null,
    ) : SettingsEntity(
        icon = icon,
        iconUri = iconUri,
        iconRes = iconRes,
        title = title,
        titleAnnotated = titleAnnotated,
        summary = summary,
        summaryAnnotated = summaryAnnotated,
        enabled = enabled,
        isChecked = isChecked,
        onCheck = onCheck,
        onClick = onClick,
        screenPosition = screenPosition,
        type = PreferenceType.Switch
    )

    @Stable
    data class SeekPreference(
        override val icon: ImageVector? = null,
        override val iconUri: String? = null,
        override val iconRes: Int? = null,
        override val title: String,
        override val titleAnnotated: AnnotatedString? = null,
        override val summary: String? = null,
        override val summaryAnnotated: AnnotatedString? = null,
        override val enabled: Boolean = true,
        override val screenPosition: Position = Position.Alone,
        override val minValue: Float? = null,
        override val currentValue: Float? = null,
        override val maxValue: Float? = null,
        override val step: Int = 1,
        override val valueMultiplier: Int = 1,
        override val seekSuffix: String? = null,
        override val onSeek: ((Float) -> Unit)? = null,
    ) : SettingsEntity(
        icon = icon,
        iconUri = iconUri,
        iconRes = iconRes,
        title = title,
        titleAnnotated = titleAnnotated,
        summary = summary,
        summaryAnnotated = summaryAnnotated,
        enabled = enabled,
        screenPosition = screenPosition,
        minValue = minValue,
        currentValue = currentValue,
        maxValue = maxValue,
        step = step,
        valueMultiplier = valueMultiplier,
        seekSuffix = seekSuffix,
        onSeek = onSeek,
        type = PreferenceType.Seek
    )

    @Stable
    data class AlbumPreference(
        override val title: String,
        override val titleAnnotated: AnnotatedString? = null,
        override val summary: String? = null,
        override val summaryAnnotated: AnnotatedString? = null,
        override val enabled: Boolean = true,
        override val screenPosition: Position = Position.Alone,
        override val onClick: (() -> Unit)? = null,
        val albumUri: Any? = null,
        val secondaryAlbumUri: Any? = null,
        val albumLabel: String? = null,
        val albumCount: Int = 0,
        val matchedAlbumsCount: Int = 0,
        val isMultiple: Boolean = false,
        val isWildcard: Boolean = false,
    ) : SettingsEntity(
        title = title,
        titleAnnotated = titleAnnotated,
        summary = summary,
        summaryAnnotated = summaryAnnotated,
        enabled = enabled,
        screenPosition = screenPosition,
        onClick = onClick,
        type = PreferenceType.Album
    )

    /**
     * A settings row rendering a single [LogEntry] — level-coloured dot, tag and
     * timestamp tinted by the level, ctx folded into the summary line.
     */
    @Stable
    data class LogPreference(
        val entry: LogEntry,
        override val screenPosition: Position = Position.Alone,
        override val enabled: Boolean = true,
        override val onClick: (() -> Unit)? = null,
        override val onLongClick: (() -> Unit)? = null,
    ) : SettingsEntity(
        title = entry.message,
        summary = entry.tag,
        enabled = enabled,
        screenPosition = screenPosition,
        onClick = onClick,
        onLongClick = onLongClick,
        type = PreferenceType.Log
    )
}
