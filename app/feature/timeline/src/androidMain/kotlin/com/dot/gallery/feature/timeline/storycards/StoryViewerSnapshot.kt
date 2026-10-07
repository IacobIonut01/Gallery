/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.timeline.storycards

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.MemoryInfo
import com.dot.gallery.cloud.core.capabilities.MemoriesCapableProvider
import com.dot.gallery.cloud.core.stableIdHash
import com.dot.gallery.core.MediaDistributor
import com.dot.gallery.core.Resource
import com.dot.gallery.core.Settings
import com.dot.gallery.domain.model.Media
import com.dot.gallery.data.entity.MediaMetadata
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.domain.model.StoryCard
import com.dot.gallery.domain.model.StoryCardType
import com.dot.gallery.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.absoluteValue
import kotlin.math.min
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.domain.model.Album
import com.dot.gallery.core.presentation.util.add
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.core.startup.StartupWorkGate
import com.dot.gallery.core.logging.printError
import com.dot.gallery.domain.model.definedTimestamp
import com.dot.gallery.feature.timeline.generated.resources.Res
import com.dot.gallery.feature.timeline.generated.resources.favorites
import com.dot.gallery.feature.timeline.generated.resources.story_highlight_count
import com.dot.gallery.feature.timeline.generated.resources.story_item_count
import com.dot.gallery.feature.timeline.generated.resources.story_this_year
import com.dot.gallery.feature.timeline.generated.resources.story_years_ago
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString


data class StoryViewerSnapshot(
    val cards: List<StoryCard>,
    val initialCardId: Long,
)