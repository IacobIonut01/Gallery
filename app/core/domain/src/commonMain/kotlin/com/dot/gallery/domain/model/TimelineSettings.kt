package com.dot.gallery.domain.model

import androidx.compose.runtime.Stable
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.dot.gallery.domain.util.MediaOrder
import com.dot.gallery.domain.util.OrderType


/**
 * Timeline settings
 *
 * This entity contains all settings of the app that
 * affects the media display.
 */
@Stable
@Entity(tableName = "timeline_settings")
data class TimelineSettings(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val groupTimelineByMonth: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val groupTimelineByYear: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val groupTimelineInAlbums: Boolean = false,
    @ColumnInfo(defaultValue = "{\"orderType\":{\"type\":\"com.dot.gallery.feature_node.domain.util.OrderType.Descending\"},\"orderType_date\":{\"type\":\"com.dot.gallery.feature_node.domain.util.OrderType.Descending\"}}")
    val timelineMediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending),
    @ColumnInfo(defaultValue = "{\"orderType\":{\"type\":\"com.dot.gallery.feature_node.domain.util.OrderType.Descending\"},\"orderType_date\":{\"type\":\"com.dot.gallery.feature_node.domain.util.OrderType.Descending\"}}")
    val albumMediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending),
)
