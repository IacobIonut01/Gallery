package com.dot.gallery.app.presentation.search

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.Position
import com.dot.gallery.core.Settings.Misc.rememberGridSize
import com.dot.gallery.core.Settings.Misc.rememberMosaicGridSize
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.navigate
import com.dot.gallery.core.navigateUp
import com.dot.gallery.core.presentation.components.SelectionSheet
import com.dot.gallery.core.ui.components.SettingsItem
import com.dot.gallery.feature.classifier.ui.components.CategoryCarouselItem
import com.dot.gallery.feature.classifier.ui.search.SearchActions
import com.dot.gallery.feature.classifier.ui.search.SearchContent
import com.dot.gallery.feature.classifier.ui.search.SearchState
import com.dot.gallery.feature.help.domain.HelpSearchItem
import com.dot.gallery.core.presentation.util.Screen
import com.dot.gallery.feature.help.toScreenRoute
import com.dot.gallery.feature.search.SearchViewModel
import com.dot.gallery.domain.uri.query
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.help_search_see_all
import com.dot.gallery.generated.resources.help_tips_section_title


/**
 * App shell for `:feature:classifier`'s [SearchContent] — collects the
 * [SearchViewModel] state, wires routes through `Screen`, and supplies the
 * "Help & Tips" block (kept app-side: it depends on `:feature:help` types).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    isScrolling: MutableState<Boolean>,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    viewModel: SearchViewModel,
) {
    val eventHandler = LocalEventHandler.current
    val searchResults by viewModel.searchResultsState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val tipResults by viewModel.tipResults.collectAsStateWithLifecycle()
    val selectedImageMedia by viewModel.selectedImageMedia.collectAsStateWithLifecycle()
    val isModelAvailable by viewModel.isModelAvailable.collectAsStateWithLifecycle()
    val searchableMediaIds by viewModel.searchableMediaIds.collectAsStateWithLifecycle()
    val searchIndexerState by viewModel.searchIndexerState.collectAsStateWithLifecycle()
    val topCategories by viewModel.topCategories.collectAsStateWithLifecycle()
    val topLocations by viewModel.topLocations.collectAsStateWithLifecycle()
    val topMimeTypes by viewModel.topMimeTypes.collectAsStateWithLifecycle()
    val topLensModels by viewModel.topLensModels.collectAsStateWithLifecycle()
    val topMediaModes by viewModel.topMediaModes.collectAsStateWithLifecycle()
    val topGroupTypes by viewModel.topGroupTypes.collectAsStateWithLifecycle()

    SearchContent(
        state = SearchState(
            query = query,
            searchResults = searchResults,
            selectedImageMedia = selectedImageMedia,
            isModelAvailable = isModelAvailable,
            searchableMediaIds = searchableMediaIds,
            searchIndexerState = searchIndexerState,
            topCategories = topCategories.map { categoryMedia ->
                CategoryCarouselItem(
                    id = categoryMedia.category.id,
                    name = categoryMedia.category.name,
                    mediaCount = categoryMedia.category.mediaCount,
                    thumbnailMedia = categoryMedia.thumbnailMedia,
                )
            },
            topLocations = topLocations,
            topMimeTypes = topMimeTypes,
            topLensModels = topLensModels,
            topMediaModes = topMediaModes,
            topGroupTypes = topGroupTypes,
        ),
        actions = SearchActions(
            onBack = { eventHandler.navigateUp() },
            onClearQuery = viewModel::clearQuery,
            onQueryChange = viewModel::setQuery,
            onSubmitSearch = { newQuery ->
                viewModel.setQuery(newQuery, apply = true)
                viewModel.addHistory(newQuery)
            },
            onRestoreImageSearch = viewModel::restoreImageSearch,
            onRemoveHistory = viewModel::removeHistory,
            onRemoveImageHistory = viewModel::removeImageHistory,
            onImageSelected = viewModel::setSelectedMedia,
            onClearSelectedMedia = viewModel::clearSelectedMedia,
            onMimeTypeFilter = viewModel::setMimeTypeQuery,
            onLensModelFilter = viewModel::setLensModelQuery,
            onMediaModeFilter = viewModel::setMediaModeQuery,
            onGroupTypeFilter = viewModel::setGroupTypeQuery,
            onRetrySearch = viewModel::retrySearch,
            onOpenCategory = { categoryId ->
                eventHandler.navigate(Screen.CategoryViewScreen.categoryId(categoryId))
            },
            onOpenLocation = { city, country, latitude, longitude ->
                eventHandler.navigate(
                    Screen.LocationTimelineScreen.location(
                        gpsLocationNameCity = city,
                        gpsLocationNameCountry = country,
                        latitude = latitude,
                        longitude = longitude,
                    )
                )
            },
            onOpenMedia = { mediaId ->
                eventHandler.navigate(Screen.MediaViewScreen.idAndAlbum(mediaId, -1L))
            },
            onOpenSearchResult = { mediaId ->
                eventHandler.navigate(Screen.MediaViewScreen.idAndQuery(mediaId))
            },
        ),
        isScrolling = isScrolling,
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope,
        tipsContent = if (tipResults.isEmpty()) null else {
            {
                HelpTipsSearchSection(
                    tips = tipResults,
                    onTipClick = { tip -> tip.route?.let { eventHandler.navigate(it.toScreenRoute()) } },
                    onSeeAll = { eventHandler.navigate(Screen.HelpScreen()) }
                )
            }
        },
        selectionSheetContent = { allMedia, selectedMediaList ->
            SelectionSheet(
                modifier = Modifier.align(Alignment.BottomEnd),
                allMedia = allMedia,
                selectedMedia = selectedMediaList
            )
        },
        lastCellIndex = rememberGridSize(),
        lastMosaicCellIndex = rememberMosaicGridSize(),
    )
}

/**
 * Compact "Help & Tips" section shown above the media results in the timeline
 * search. Renders the top matching tips; a "See all" row opens the full Help
 * screen. Tapping a tip navigates to its detail (never mutates search state).
 */
@Composable
private fun HelpTipsSearchSection(
    tips: List<HelpSearchItem>,
    onTipClick: (HelpSearchItem) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Build the same settings-DSL structure the search history uses: a section
    // Header followed by slim Preference rows, rendered through SettingsItem so the
    // grouping, corners and typography match history exactly.
    val entities = buildList {
        add(SettingsEntity.Header(stringResource(Res.string.help_tips_section_title)))
        tips.take(3).forEach { tip ->
            add(
                SettingsEntity.Preference(
                    icon = tip.icon,
                    title = tip.title,
                    summary = tip.subtitle.ifBlank { null },
                    onClick = { onTipClick(tip) }
                )
            )
        }
        if (tips.size > 3) {
            add(
                SettingsEntity.Preference(
                    title = stringResource(Res.string.help_search_see_all, tips.size),
                    onClick = onSeeAll
                )
            )
        }
    }
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        entities.forEachIndexed { index, item ->
            val position = when (index) {
                0 -> if (entities.size == 1) Position.Alone else Position.Top
                entities.lastIndex ->
                    if (entities[index - 1] is SettingsEntity.Header) Position.Alone
                    else Position.Bottom

                else -> {
                    val previous = entities[index - 1]
                    val next = entities[index + 1]
                    when {
                        previous is SettingsEntity.Header && next is SettingsEntity.Header -> Position.Alone
                        previous is SettingsEntity.Header -> Position.Top
                        next is SettingsEntity.Header -> Position.Bottom
                        else -> Position.Middle
                    }
                }
            }
            val newItem = when (item) {
                is SettingsEntity.Preference -> item.copy(screenPosition = position)
                else -> item
            }
            SettingsItem(
                item = newItem,
                slimLayout = true,
                tintIcon = newItem.iconUri == null
            )
        }
    }
}
