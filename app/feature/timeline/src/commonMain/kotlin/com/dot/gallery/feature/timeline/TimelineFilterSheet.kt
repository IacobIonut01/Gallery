package com.dot.gallery.feature.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import com.dot.gallery.core.presentation.components.SetupButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.FilterKind
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.util.OrderType
import com.dot.gallery.domain.model.MediaTypeFilter
import com.dot.gallery.domain.model.TimelineFilter
import com.dot.gallery.core.ui.util.AppBottomSheetState
import com.dot.gallery.domain.model.LastSort
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.timeline.generated.resources.Res
import com.dot.gallery.feature.timeline.generated.resources.capture_time
import com.dot.gallery.feature.timeline.generated.resources.capture_time_indexing
import com.dot.gallery.feature.timeline.generated.resources.capture_time_summary
import com.dot.gallery.feature.timeline.generated.resources.favorites
import com.dot.gallery.feature.timeline.generated.resources.filter_all
import com.dot.gallery.feature.timeline.generated.resources.filter_apply
import com.dot.gallery.feature.timeline.generated.resources.filter_reset
import com.dot.gallery.feature.timeline.generated.resources.filter_source
import com.dot.gallery.feature.timeline.generated.resources.filter_status
import com.dot.gallery.feature.timeline.generated.resources.filter_time
import com.dot.gallery.feature.timeline.generated.resources.filter_type
import com.dot.gallery.feature.timeline.generated.resources.modified_time_summary
import com.dot.gallery.feature.timeline.generated.resources.order_by
import com.dot.gallery.feature.timeline.generated.resources.photos
import com.dot.gallery.feature.timeline.generated.resources.sort_ascending
import com.dot.gallery.feature.timeline.generated.resources.sort_by_date_modified
import com.dot.gallery.feature.timeline.generated.resources.sort_descending
import com.dot.gallery.feature.timeline.generated.resources.timeline_controls
import com.dot.gallery.feature.timeline.generated.resources.videos


const val TIMELINE_FILTER_CHIP_VISUAL_TAG = "TimelineFilterChipVisual"
const val TIMELINE_CAPTURE_SORT_TAG = "TimelineCaptureSort"
const val TIMELINE_MODIFIED_SORT_TAG = "TimelineModifiedSort"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimelineFilterSheet(
    sheetState: AppBottomSheetState,
    currentFilter: TimelineFilter,
    currentSort: LastSort,
    availableYears: List<Int>,
    availableAlbums: List<Album>,
    indexProgress: Int?,
    onApply: (TimelineFilter, LastSort) -> Unit,
) {
    val scope = rememberCoroutineScope()

    if (sheetState.isVisible) {
        var filter by remember(currentFilter) { mutableStateOf(currentFilter) }
        var sort by remember(currentSort) { mutableStateOf(currentSort) }

        ModalBottomSheet(
            sheetState = sheetState.sheetState,
            onDismissRequest = {
                scope.launch { sheetState.hide() }
            },
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Scrollable filter content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    // Title
                    Text(
                        text = stringResource(Res.string.timeline_controls),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    FilterSectionHeader(stringResource(Res.string.order_by))
                    Column(
                        modifier = Modifier.padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TimelineSortOption(
                            title = stringResource(Res.string.capture_time),
                            summary = stringResource(Res.string.capture_time_summary),
                            testTag = TIMELINE_CAPTURE_SORT_TAG,
                            selected = sort.kind == FilterKind.DATE,
                            onClick = { sort = sort.copy(kind = FilterKind.DATE) }
                        )
                        TimelineSortOption(
                            title = stringResource(Res.string.sort_by_date_modified),
                            summary = stringResource(Res.string.modified_time_summary),
                            testTag = TIMELINE_MODIFIED_SORT_TAG,
                            selected = sort.kind == FilterKind.DATE_MODIFIED,
                            onClick = { sort = sort.copy(kind = FilterKind.DATE_MODIFIED) }
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        TimelineFilterChip(
                            label = stringResource(Res.string.sort_descending),
                            selected = sort.orderType == OrderType.Descending,
                            role = Role.RadioButton,
                            onClick = { sort = sort.copy(orderType = OrderType.Descending) }
                        )
                        TimelineFilterChip(
                            label = stringResource(Res.string.sort_ascending),
                            selected = sort.orderType == OrderType.Ascending,
                            role = Role.RadioButton,
                            onClick = { sort = sort.copy(orderType = OrderType.Ascending) }
                        )
                    }
                    if (indexProgress != null) {
                        Text(
                            text = stringResource(Res.string.capture_time_indexing, indexProgress),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Type section
                    FilterSectionHeader(stringResource(Res.string.filter_type))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        TimelineFilterChip(
                            label = stringResource(Res.string.filter_all),
                            selected = filter.mediaType == MediaTypeFilter.ALL,
                            onClick = { filter = filter.copy(mediaType = MediaTypeFilter.ALL) }
                        )
                        TimelineFilterChip(
                            label = stringResource(Res.string.photos),
                            selected = filter.mediaType == MediaTypeFilter.PHOTOS,
                            onClick = { filter = filter.copy(mediaType = MediaTypeFilter.PHOTOS) }
                        )
                        TimelineFilterChip(
                            label = stringResource(Res.string.videos),
                            selected = filter.mediaType == MediaTypeFilter.VIDEOS,
                            onClick = { filter = filter.copy(mediaType = MediaTypeFilter.VIDEOS) }
                        )
                    }

                    // Status section
                    FilterSectionHeader(stringResource(Res.string.filter_status))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        TimelineFilterChip(
                            label = stringResource(Res.string.favorites),
                            selected = filter.favoritesOnly,
                            onClick = { filter = filter.copy(favoritesOnly = !filter.favoritesOnly) }
                        )
                    }

                    // Time section
                    if (availableYears.isNotEmpty()) {
                        FilterSectionHeader(stringResource(Res.string.filter_time))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            availableYears.forEach { year ->
                                TimelineFilterChip(
                                    label = year.toString(),
                                    selected = year in filter.selectedYears,
                                    onClick = {
                                        val newYears = filter.selectedYears.toMutableSet()
                                        if (year in newYears) newYears.remove(year) else newYears.add(year)
                                        filter = filter.copy(selectedYears = newYears)
                                    }
                                )
                            }
                        }
                    }

                    // Source (Albums) section
                    if (availableAlbums.isNotEmpty()) {
                        FilterSectionHeader(stringResource(Res.string.filter_source))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            availableAlbums.forEach { album ->
                                TimelineFilterChip(
                                    label = album.label,
                                    selected = album.id in filter.selectedAlbumIds,
                                    onClick = {
                                        val newIds = filter.selectedAlbumIds.toMutableSet()
                                        if (album.id in newIds) newIds.remove(album.id) else newIds.add(album.id)
                                        filter = filter.copy(selectedAlbumIds = newIds)
                                    }
                                )
                            }
                        }
                    }
                }

                // Pinned bottom buttons
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SetupButton(
                        modifier = Modifier.weight(1f),
                        applyHorizontalPadding = false,
                        applyBottomPadding = false,
                        applyInsets = false,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        text = stringResource(Res.string.filter_reset),
                        onClick = {
                            filter = TimelineFilter()
                            sort = LastSort(OrderType.Descending, FilterKind.DATE)
                        }
                    )
                    SetupButton(
                        modifier = Modifier.weight(1f),
                        applyHorizontalPadding = false,
                        applyBottomPadding = false,
                        applyInsets = false,
                        text = stringResource(Res.string.filter_apply),
                        onClick = {
                            onApply(filter, sort)
                            scope.launch { sheetState.hide() }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
