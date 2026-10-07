/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.app.presentation.widget.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.serialization.json.Json
import com.dot.gallery.core.util.or
import com.dot.gallery.core.presentation.util.remove


object WidgetPreferences {

    private const val PREFS_NAME = "gallery_widget_prefs"
    private const val KEY_PREFIX = "widget_"

    private val json = Json { ignoreUnknownKeys = true }

    fun saveWidgetData(
        context: Context,
        widgetId: Int,
        type: WidgetType,
        uris: List<Uri>,
        mediaIds: List<Long?> = emptyList(),
        displayStyle: WidgetDisplayStyle = WidgetDisplayStyle.IMAGE,
        icon: String? = null
    ) {
        val data = WidgetData(
            widgetId = widgetId,
            type = type,
            mediaUris = uris.map { it.toString() },
            mediaIds = mediaIds,
            displayStyle = displayStyle,
            icon = icon
        )
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit(commit = true) {
                putString("$KEY_PREFIX$widgetId", json.encodeToString(data))
            }
    }

    fun getWidgetData(context: Context, widgetId: Int): WidgetData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("$KEY_PREFIX$widgetId", null) ?: return null
        return try {
            json.decodeFromString<WidgetData>(jsonStr)
        } catch (_: Exception) {
            null
        }
    }

    fun deleteWidgetData(context: Context, widgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                remove("$KEY_PREFIX$widgetId")
            }
    }

    fun getMediaUris(context: Context, widgetId: Int): List<Uri> {
        val data = getWidgetData(context, widgetId) ?: return emptyList()
        return data.mediaUris.map { it.toUri() }
    }
}
