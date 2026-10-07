package com.dot.gallery.app.presentation.albumtimeline

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.dot.gallery.core.AlbumMediaLoadMode
import com.dot.gallery.core.MediaDistributor
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.feature.albums.ui.AlbumTimelineViewModel
import com.dot.gallery.app.presentation.util.MockedMediaDistributor
import com.dot.gallery.core.presentation.util.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.core.navigate
import com.dot.gallery.domain.uri.path




@MediumTest
@RunWith(AndroidJUnit4::class)
class AlbumTimelineNavigationTest {

    @get:Rule
    val rule = createComposeRule()

    private class CountingDistributor : MockedMediaDistributor() {
        var starts = 0
        var completions = 0
        val source = MutableStateFlow(readyState(ITEM_COUNT))

        override fun albumTimelineMediaFlow(
            albumId: Long,
            loadMode: AlbumMediaLoadMode
        ): Flow<MediaState<UriMedia>> {
            return source
                .onStart { starts++ }
                .onCompletion { completions++ }
        }
    }

    private class Harness {
        var nav: NavHostController? = null
        var scope: CoroutineScope? = null
        var albumVm: AlbumTimelineViewModel? = null
        var viewerVm: AlbumTimelineViewModel? = null
        var gridState: LazyGridState? = null
        val albumFirstSeen = mutableListOf<MediaState<UriMedia>>()
        val viewerFirstSeen = mutableListOf<MediaState<UriMedia>>()
    }

    private fun vmFactory(distributor: MediaDistributor, albumId: Long) =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AlbumTimelineViewModel(
                    SavedStateHandle(mapOf("albumId" to albumId)),
                    distributor
                ) as T
        }

    private fun ComposeContentTestRule.installGraph(
        distributor: CountingDistributor,
        harness: Harness,
        startAtViewer: Boolean = false,
    ) {
        setContent {
            val nav = rememberNavController().also { harness.nav = it }
            harness.scope = rememberCoroutineScope()
            NavHost(
                navController = nav,
                startDestination = if (startAtViewer) {
                    Screen.MediaViewScreen.idAndAlbum(deepMediaId(GRID_INDEX), ALBUM_ID)
                } else "home"
            ) {
                composable("home") {
                    Box(Modifier.fillMaxSize().clickable {
                        nav.navigate(Screen.AlbumViewScreen.album(ALBUM_ID, "Deep"))
                    }) { Text("home") }
                }
                composable(
                    route = Screen.AlbumViewScreen.albumAndName(),
                    arguments = listOf(
                        navArgument("albumId") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("albumName") { type = NavType.StringType; defaultValue = "" },
                    )
                ) { entry ->
                    val aId = entry.arguments?.getLong("albumId") ?: -1L
                    val vm: AlbumTimelineViewModel = viewModel(entry, factory = vmFactory(distributor, aId))
                    harness.albumVm = vm
                    val albumSnap = remember(entry) { vm.mediaState.value }
                    LaunchedEffect(entry) { harness.albumFirstSeen += albumSnap }
                    val state by vm.mediaState.collectAsStateWithLifecycle()
                    val grid = rememberLazyGridState().also { harness.gridState = it }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = grid,
                        modifier = Modifier.fillMaxSize().testTag("albumGrid")
                    ) {
                        items(count = state.pagerMedia.size) { i ->
                            val media = state.pagerMedia[i]
                            Box(
                                Modifier
                                    .size(64.dp)
                                    .clickable {
                                        nav.navigate(Screen.MediaViewScreen.idAndAlbum(media.id, aId))
                                    }
                            ) { Text(media.label) }
                        }
                    }
                }
                composable(
                    route = Screen.MediaViewScreen.idAndAlbum(),
                    arguments = listOf(
                        navArgument("mediaId") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("albumId") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("slideshow") { type = NavType.BoolType; defaultValue = false },
                    )
                ) { entry ->
                    val mId = entry.arguments?.getLong("mediaId") ?: -1L
                    val aId = entry.arguments?.getLong("albumId") ?: -1L
                    val owner = remember(entry, aId) { albumViewerStateOwner(nav, entry, aId) }
                    val vm: AlbumTimelineViewModel = viewModel(owner, factory = vmFactory(distributor, aId))
                    harness.viewerVm = vm
                    val viewerSnap = remember(entry) { vm.mediaState.value }
                    LaunchedEffect(entry) { harness.viewerFirstSeen += viewerSnap }
                    Text(
                        "viewer:m=$mId:loading=${vm.mediaState.value.isLoading}:items=${vm.mediaState.value.media.size}:index=${vm.mediaState.value.pagerMedia.indexOfFirst { it.id == mId }}",
                        Modifier.testTag("viewerInfo")
                    )
                }
            }
        }
    }

    @Test
    fun viewerReusesParentAlbumStateAndBackRestoresGrid() {
        val distributor = CountingDistributor()
        val harness = Harness()
        rule.installGraph(distributor, harness)
        rule.waitForIdle()

        rule.onNodeWithText("home").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("albumGrid").assertIsDisplayed()
        rule.runOnIdle {
            assertEquals(1, distributor.starts)
            assertFalse(harness.albumFirstSeen.last().isLoading)
            assertEquals(ITEM_COUNT, harness.albumFirstSeen.last().media.size)
        }

        rule.runOnUiThread {
            harness.scope!!.launch { harness.gridState!!.scrollToItem(GRID_INDEX, SCROLL_OFFSET) }
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            harness.gridState!!.firstVisibleItemIndex == GRID_INDEX &&
                harness.gridState!!.firstVisibleItemScrollOffset == SCROLL_OFFSET
        }

        rule.onNodeWithText(deepLabel(GRID_INDEX)).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("viewerInfo").assertIsDisplayed()

        var stateBeforeBack: MediaState<UriMedia>? = null
        rule.runOnIdle {
            assertSame(harness.albumVm, harness.viewerVm)
            val viewerSnap = harness.viewerFirstSeen.last()
            assertFalse(viewerSnap.isLoading)
            assertEquals(ITEM_COUNT, viewerSnap.media.size)
            assertEquals(GRID_INDEX, viewerSnap.pagerMedia.indexOfFirst { it.id == deepMediaId(GRID_INDEX) })
            assertEquals(
                Lifecycle.State.CREATED,
                harness.nav!!.getBackStackEntry(Screen.AlbumViewScreen.albumAndName()).lifecycle.currentState
            )
            stateBeforeBack = harness.albumVm!!.mediaState.value
        }

        rule.runOnUiThread { harness.nav!!.popBackStack() }
        rule.waitForIdle()
        rule.onNodeWithTag("albumGrid").assertIsDisplayed()

        rule.runOnIdle {
            assertEquals(2, harness.albumFirstSeen.size)
            assertSame(stateBeforeBack, harness.albumFirstSeen.last())
            assertFalse(harness.albumFirstSeen.last().isLoading)
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            harness.gridState!!.firstVisibleItemIndex == GRID_INDEX &&
                harness.gridState!!.firstVisibleItemScrollOffset == SCROLL_OFFSET
        }
        rule.runOnIdle {
            assertEquals(1, distributor.starts)
            assertEquals(0, distributor.completions)
        }

        rule.onNodeWithText(deepLabel(GRID_INDEX + 1)).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("viewerInfo").assertIsDisplayed()
        rule.runOnIdle {
            assertSame(harness.albumVm, harness.viewerVm)
            assertEquals(
                GRID_INDEX + 1,
                harness.viewerFirstSeen.last().pagerMedia.indexOfFirst { it.id == deepMediaId(GRID_INDEX + 1) }
            )
        }

        rule.runOnUiThread {
            distributor.source.value = readyState(ITEM_COUNT - 1)
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            harness.viewerVm!!.mediaState.value.media.size == ITEM_COUNT - 1
        }
        rule.runOnUiThread { harness.nav!!.popBackStack() }
        rule.waitForIdle()
        rule.onNodeWithTag("albumGrid").assertIsDisplayed()

        rule.runOnIdle {
            assertEquals(ITEM_COUNT - 1, harness.albumFirstSeen.last().media.size)
            assertEquals(1, distributor.starts)
        }

        rule.runOnUiThread { harness.nav!!.popBackStack() }
        rule.waitUntil(timeoutMillis = 5_000) { distributor.completions == 1 }
    }

    @Test
    fun viewerWithoutAlbumParentOwnsItsState() {
        val distributor = CountingDistributor()
        val harness = Harness()
        rule.installGraph(distributor, harness, startAtViewer = true)
        rule.waitForIdle()

        rule.onNodeWithTag("viewerInfo").assertIsDisplayed()
        var standaloneViewerVm: AlbumTimelineViewModel? = null
        rule.runOnIdle {
            standaloneViewerVm = harness.viewerVm
            assertEquals(1, distributor.starts)
        }

        rule.runOnUiThread {
            harness.nav!!.navigate(Screen.AlbumViewScreen.album(ALBUM_ID, "Deep"))
        }
        rule.waitForIdle()
        rule.onNodeWithTag("albumGrid").assertIsDisplayed()

        rule.runOnIdle {
            assertNotSame(standaloneViewerVm, harness.albumVm)
            assertEquals(2, distributor.starts)
        }
    }

    @Test
    fun poppingAlbumEntryStopsItsSource() {
        val distributor = CountingDistributor()
        val harness = Harness()
        rule.installGraph(distributor, harness)
        rule.waitForIdle()

        rule.onNodeWithText("home").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("albumGrid").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, distributor.starts) }

        rule.runOnUiThread { harness.nav!!.popBackStack() }
        rule.waitForIdle()
        rule.onNodeWithText("home").assertIsDisplayed()
        rule.waitUntil(timeoutMillis = 5_000) { distributor.completions == 1 }
    }

    @Test
    fun differentAlbumEntriesGetSeparateOwnerState() {
        val distributor = CountingDistributor()
        val harness = Harness()
        rule.installGraph(distributor, harness)
        rule.waitForIdle()

        rule.onNodeWithText("home").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("albumGrid").assertIsDisplayed()
        var firstVm: AlbumTimelineViewModel? = null
        rule.runOnIdle { firstVm = harness.albumVm }

        rule.runOnUiThread { harness.nav!!.popBackStack() }
        rule.waitUntil(timeoutMillis = 5_000) { distributor.completions == 1 }

        rule.runOnUiThread {
            harness.nav!!.navigate(Screen.AlbumViewScreen.album(ALBUM_ID + 1, "Other"))
        }
        rule.waitForIdle()

        rule.runOnIdle {
            assertNotSame(firstVm, harness.albumVm)
            assertEquals(ALBUM_ID + 1, harness.albumVm!!.albumId)
            assertEquals(2, distributor.starts)
        }
    }

    @Test
    fun nestedAlbumEntriesShareNearestMatchingOwner() {
        val distributor = CountingDistributor()
        val harness = Harness()
        rule.installGraph(distributor, harness)
        rule.waitForIdle()

        rule.onNodeWithText("home").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("albumGrid").assertIsDisplayed()
        var albumAVm: AlbumTimelineViewModel? = null
        rule.runOnIdle { albumAVm = harness.albumVm }

        rule.runOnUiThread {
            harness.nav!!.navigate(Screen.AlbumViewScreen.album(ALBUM_ID_B, "Neg"))
        }
        rule.waitForIdle()
        rule.onNodeWithTag("albumGrid").assertIsDisplayed()
        var albumBVm: AlbumTimelineViewModel? = null
        rule.runOnIdle {
            albumBVm = harness.albumVm
            assertNotSame(albumAVm, albumBVm)
            assertEquals(ALBUM_ID_B, albumBVm!!.albumId)
            assertEquals(2, distributor.starts)
        }

        rule.runOnUiThread {
            harness.nav!!.navigate(Screen.MediaViewScreen.idAndAlbum(deepMediaId(GRID_INDEX), ALBUM_ID_B))
        }
        rule.waitForIdle()
        rule.onNodeWithTag("viewerInfo").assertIsDisplayed()
        rule.runOnIdle {
            assertSame(albumBVm, harness.viewerVm)
            assertNotSame(albumAVm, harness.viewerVm)
            assertEquals(2, distributor.starts)
        }

        rule.runOnUiThread { harness.nav!!.popBackStack() }
        rule.waitForIdle()
        rule.onNodeWithTag("albumGrid").assertIsDisplayed()

        rule.runOnUiThread {
            harness.nav!!.navigate(Screen.MediaViewScreen.idAndAlbum(deepMediaId(GRID_INDEX), OTHER_ALBUM_ID))
        }
        rule.waitForIdle()
        rule.onNodeWithTag("viewerInfo").assertIsDisplayed()
        rule.runOnIdle {
            assertNotSame(albumAVm, harness.viewerVm)
            assertNotSame(albumBVm, harness.viewerVm)
            assertEquals(OTHER_ALBUM_ID, harness.viewerVm!!.albumId)
            assertEquals(3, distributor.starts)
        }
    }

    companion object {
        private const val ALBUM_ID = 525230640L
        private const val ALBUM_ID_B = -525230641L
        private const val OTHER_ALBUM_ID = 525230642L
        private const val ITEM_COUNT = 600
        private const val GRID_INDEX = 450
        private const val SCROLL_OFFSET = 30

        private fun deepMediaId(index: Int) = index.toLong() + 1
        private fun deepLabel(index: Int) = "RefraDeep_%04d.jpg".format(index)

        private fun readyState(count: Int): MediaState<UriMedia> {
            val media = (0 until count).map { i ->
                UriMedia(
                    id = i.toLong() + 1,
                    label = deepLabel(i),
                    uri = Uri.parse("content://media/external/file/${i + 1}"),
                    path = "/sdcard/Pictures/ReFraDeepAlbum20260915/${deepLabel(i)}",
                    relativePath = "Pictures/ReFraDeepAlbum20260915/",
                    albumID = ALBUM_ID,
                    albumLabel = "ReFraDeepAlbum20260915",
                    timestamp = 1_700_000_000L + i,
                    fullDate = "now",
                    mimeType = "image/jpeg",
                    favorite = 0,
                    trashed = 0,
                    size = 73174L,
                )
            }
            return MediaState(
                media = media,
                pagerMedia = media,
                isLoading = false,
            )
        }
    }
}
