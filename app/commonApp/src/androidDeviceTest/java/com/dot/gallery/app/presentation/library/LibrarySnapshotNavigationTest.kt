/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.library

import com.dot.gallery.feature.library.ui.mergeLibraryViewport
import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.LocalMediaSelector
import com.dot.gallery.core.MediaSelectorImpl
import com.dot.gallery.core.ml.ModelStatus
import com.dot.gallery.core.startup.LocalStartupWorkGate
import com.dot.gallery.domain.model.LibraryIndicatorState
import com.dot.gallery.domain.model.LocationMedia
import com.dot.gallery.domain.model.UIEvent
import com.dot.gallery.domain.util.EventHandler
import com.dot.gallery.ui.theme.GalleryTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.core.ui.components.settings
import com.dot.gallery.feature.editor.adjustments.filters.None
import com.dot.gallery.core.presentation.components.LocalMediaImageRenderer
import com.dot.gallery.core.presentation.components.MockedMediaImageRenderer
import com.dot.gallery.core.startup.StartupContentEffect
import com.dot.gallery.core.startup.StartupWorkGate
import com.dot.gallery.core.ui.settings.LocalSettingsStore
import com.dot.gallery.domain.settings.MapSettingsStore
import com.dot.gallery.feature.library.ui.CloudLibraryState
import com.dot.gallery.feature.library.ui.LibraryActions
import com.dot.gallery.feature.library.ui.LibraryCategoryPreview
import com.dot.gallery.feature.library.ui.LibraryContent
import com.dot.gallery.feature.library.ui.LibraryGeoPreview
import com.dot.gallery.feature.library.ui.LibraryRoutes
import com.dot.gallery.feature.library.ui.LibraryScrollPosition
import com.dot.gallery.feature.library.ui.LibrarySnapshot
import com.dot.gallery.feature.library.ui.LibraryState
import com.dot.gallery.feature.library.ui.LibraryViewport
import com.dot.gallery.app.presentation.search.MainSearchBar
import kotlinx.coroutines.flow.map
import com.dot.gallery.cloud.core.LOCAL_PEOPLE_CONFIG_ID
import com.dot.gallery.domain.uri.path
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.categorise_your_media
import com.dot.gallery.generated.resources.cloud_people
import com.dot.gallery.core.util.getString
import org.jetbrains.compose.resources.StringResource


@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalSharedTransitionApi::class)
class LibrarySnapshotNavigationTest {

    @get:Rule
    val rule = createComposeRule()

    private fun string(res: StringResource): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(res)

    private val handler = object : EventHandler {
        override val updaterFlow: Flow<UIEvent> = emptyFlow()
        override var navigateAction: (String) -> Unit = {}
        override var toggleNavigationBarAction: (Boolean) -> Unit = {}
        override var navigateUpAction: () -> Unit = {}
        override var setFollowThemeAction: (Boolean) -> Unit = {}
        override fun pushEvent(event: UIEvent) {}
    }



    private fun person(id: String) = PersonInfo(
        id = id,
        name = "person-$id",
        providerType = ProviderType.LOCAL_PEOPLE,
        serverConfigId = LOCAL_PEOPLE_CONFIG_ID,
        assetCount = 2
    )

    private fun media(id: Long) = UriMedia(
        id = id,
        label = "photo-$id.jpg",
        uri = Uri.parse("content://media/external/images/media/$id"),
        path = "/storage/emulated/0/DCIM/photo-$id.jpg",
        relativePath = "DCIM/",
        albumID = 10L,
        albumLabel = "Camera",
        timestamp = 1_700_000_000L + id,
        fullDate = "2023-11-14",
        mimeType = "image/jpeg",
        favorite = 0,
        trashed = 0,
        size = 1_024L
    )

    private fun locationRow(id: Long) = LocationMedia(
        media = media(id),
        location = "loc-$id",
        city = "loc-$id",
        country = "DE",
        latitude = 52.5,
        longitude = 13.4
    )

    private fun category(id: Long) = LibraryCategoryPreview(
        id = id,
        name = "category-$id",
        mediaCount = 3,
        thumbnailMedia = null
    )

    private fun populatedSnapshot(
        viewport: LibraryViewport = LibraryViewport()
    ) = LibrarySnapshot(
        categories = (1L..8L).map(::category),
        categoryCount = 12,
        locations = (0 until 20).map { locationRow(1_000L + it) },
        locationCount = 34,
        latestGeo = LibraryGeoPreview(media(3L), 52.5, 13.4),
        peopleCount = 20,
        peopleCountsByAccount = mapOf(LOCAL_PEOPLE_CONFIG_ID to 20),
        cloud = CloudLibraryState(
            hasPeople = true,
            people = (0 until 12).map { person("p$it") }
        ),
        indicators = LibraryIndicatorState(trashCount = 1, favoriteCount = 2),
        viewport = viewport
    )

    private class Rig(
        val snapshot: MutableStateFlow<LibrarySnapshot>,
        var viewport: LibraryViewport,
        var nav: NavHostController?,
        val showLibrary: MutableState<Boolean>,
        var firstDrawn: LibrarySnapshot?
    )

    private fun setLibraryContent(
        initial: LibrarySnapshot,
        contentHeight: Dp? = null,
        withEnterTransition: Boolean = false
    ): Rig {
        val snapshotFlow = MutableStateFlow(initial)
        val rig = Rig(snapshotFlow, LibraryViewport(), null, mutableStateOf(true), null)
        rule.setContent {
            GalleryTheme(ignoreUserPreference = true) {
                CompositionLocalProvider(
                    LocalEventHandler provides handler,
                    LocalMediaSelector provides MediaSelectorImpl(),
                    LocalStartupWorkGate provides StartupWorkGate(),
                    // In-memory store — LibraryContent resolves every
                    // rememberPreference through it; defaults apply and
                    // section toggles stay hermetic per test run.
                    LocalSettingsStore provides MapSettingsStore(),
                    LocalMediaImageRenderer provides MockedMediaImageRenderer
                ) {
                    SharedTransitionLayout {
                        val navController = rememberNavController()
                            .also { rig.nav = it }
                        val snapshot by snapshotFlow.collectAsState()
                        NavHost(
                            navController = navController,
                            startDestination = "library",
                            enterTransition = {
                                if (withEnterTransition) fadeIn(tween(400))
                                else androidx.compose.animation.EnterTransition.None
                            }
                        ) {
                            composable("library") {
                                val content: @Composable () -> Unit = {
                                    if (rig.showLibrary.value) {
                                        val isScrolling = remember { mutableStateOf(false) }
                                        // Drawn-frame reporting moved out of the
                                        // content composable into the app shell's
                                        // StartupContentEffect — mirrored here.
                                        StartupContentEffect(
                                            route = "library",
                                            ready = true,
                                            onContentDrawn = {
                                                if (rig.firstDrawn == null) {
                                                    rig.firstDrawn = snapshot
                                                }
                                            }
                                        )
                                        LibraryContent(
                                            state = LibraryState(
                                                snapshot = snapshot,
                                                modelStatus = ModelStatus.READY,
                                                aiAvailable = true,
                                                mapsEnabled = false
                                            ),
                                            actions = LibraryActions(
                                                onNavigate = {},
                                                onOpenLocation = { _, _, _, _ -> },
                                                onOpenMedia = {},
                                                onOpenPerson = { _, _ -> },
                                                onOpenCategory = {},
                                                onEditCategory = {},
                                                onViewportChanged = {
                                                    rig.viewport =
                                                        mergeLibraryViewport(rig.viewport, it)
                                                },
                                                onCellsIndexChange = {}
                                            ),
                                            routes = LibraryRoutes(
                                                library = "library",
                                                settings = "settings",
                                                locations = "locations",
                                                people = "people",
                                                categories = "categories",
                                                trash = "trash",
                                                favorites = "favorites",
                                                vault = "vault",
                                                ignored = "ignored",
                                                privateFolder = "privateFolder",
                                                cloudArchive = "cloudArchive",
                                                cloudSharedLinks = "cloudSharedLinks",
                                                cloudBackup = "cloudBackup",
                                                cloudAccounts = "cloudAccounts"
                                            ),
                                            paddingValues = PaddingValues(0.dp),
                                            isScrolling = isScrolling,
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedContentScope = this,
                                            searchBar = { statusBarInsets, menuItems ->
                                                MainSearchBar(
                                                    isScrolling = isScrolling,
                                                    sharedTransitionScope =
                                                        this@SharedTransitionLayout,
                                                    animatedContentScope = this,
                                                    menuItems = menuItems,
                                                    statusBarInsets = statusBarInsets
                                                )
                                            },
                                            mapContent = null
                                        )
                                    }
                                }
                                if (contentHeight != null) {
                                    Box(Modifier.height(contentHeight)) { content() }
                                } else {
                                    content()
                                }
                            }
                            composable("other") { Text("other-destination") }
                        }
                    }
                }
            }
        }
        return rig
    }

    private fun assertPopulated(drawn: LibrarySnapshot) {
        assertEquals(20, drawn.locations!!.size)
        assertEquals(34, drawn.locationCount)
        assertEquals(12, drawn.cloud.people.size)
        assertEquals(20, drawn.peopleCount)
        assertEquals(8, drawn.categories!!.size)
        assertEquals(12, drawn.categoryCount)
    }

    @Test
    fun firstDrawDeliversCachedContentBeforeAnyLiveUpdate() {
        val rig = setLibraryContent(populatedSnapshot())
        rule.waitUntil("first content draw", 5_000) { rig.firstDrawn != null }
        assertPopulated(rig.firstDrawn!!)

        val updated = populatedSnapshot().copy(
            cloud = populatedSnapshot().cloud.copy(
                people = populatedSnapshot().cloud.people + person("late")
            )
        )
        rig.snapshot.value = updated
        rule.waitForIdle()
        rule.onNodeWithTag("library-grid")
            .performScrollToNode(hasTestTag("library-people"))
        rule.onNodeWithTag("library-people").performScrollToNode(
            hasTestTag("library-person-${person("late").accountKey}")
        ).assertIsDisplayed()
    }

    @Test
    fun restoredContentRendersAllSectionsImmediately() {
        setLibraryContent(populatedSnapshot())
        rule.onNodeWithTag("library-grid").assertIsDisplayed()
        rule.onNodeWithTag("library-grid")
            .performScrollToNode(hasTestTag("library-people"))
        rule.onNodeWithTag("library-people").performScrollToNode(
            hasTestTag("library-person-${person("p0").accountKey}")
        ).assertIsDisplayed()
        rule.onNodeWithTag("library-grid")
            .performScrollToNode(hasTestTag("library-locations"))
        rule.onNodeWithTag("library-locations").performScrollToNode(
            hasTestTag("library-location-1000")
        ).assertIsDisplayed()
        rule.onNodeWithTag("library-grid")
            .performScrollToNode(hasTestTag("library-categories"))
        rule.onNodeWithTag("library-categories").performScrollToNode(
            hasTestTag("library-category-1")
        ).assertIsDisplayed()
        rule.onNodeWithText(string(Res.string.cloud_people)).assertIsDisplayed()
    }

    @Test
    fun rowPositionsAreCapturedOnLeaveAndRestoredOnReentry() {
        val rig = setLibraryContent(populatedSnapshot())
        rule.waitForIdle()
        rule.onNodeWithTag("library-grid")
            .performScrollToNode(hasTestTag("library-locations"))
        rule.onNodeWithTag("library-locations").performTouchInput { swipeLeft() }
        rule.onNodeWithTag("library-grid")
            .performScrollToNode(hasTestTag("library-people"))
        rule.onNodeWithTag("library-people").performTouchInput { swipeLeft() }
        rule.onNodeWithTag("library-grid")
            .performScrollToNode(hasTestTag("library-categories"))
        rule.onNodeWithTag("library-categories").performTouchInput { swipeLeft() }
        rule.onNodeWithTag("library-categories")
            .performScrollToNode(hasTestTag("library-category-5"))
        rule.waitForIdle()
        val boundsBefore = rule.onNodeWithTag("library-category-5")
            .getUnclippedBoundsInRoot()

        rule.runOnUiThread { rig.nav!!.navigate("other") }
        rule.onNodeWithText("other-destination").assertIsDisplayed()
        val saved = rig.viewport
        assertNotNull(saved.locations.key)
        assertTrue(saved.locations.index > 0 || saved.locations.offset > 0)
        assertNotNull(saved.people.key)
        assertTrue(saved.people.index > 0 || saved.people.offset > 0)
        assertNotNull(saved.categories.key)
        assertTrue(saved.categories.index > 0 || saved.categories.offset > 0)

        rig.viewport = LibraryViewport()
        rig.snapshot.value = populatedSnapshot(viewport = saved)
        rule.runOnUiThread { rig.nav!!.popBackStack() }
        rule.waitForIdle()
        assertEquals(saved.locations.key, rig.viewport.locations.key)
        assertEquals(saved.locations.index, rig.viewport.locations.index)
        assertEquals(saved.locations.offset, rig.viewport.locations.offset)
        assertEquals(saved.people.key, rig.viewport.people.key)
        assertEquals(saved.people.index, rig.viewport.people.index)
        assertEquals(saved.people.offset, rig.viewport.people.offset)
        assertEquals(saved.categories.key, rig.viewport.categories.key)
        assertEquals(saved.categories.index, rig.viewport.categories.index)
        assertEquals(saved.categories.offset, rig.viewport.categories.offset)

        rule.onNodeWithTag("library-category-5").assertIsDisplayed()
        val boundsAfter = rule.onNodeWithTag("library-category-5")
            .getUnclippedBoundsInRoot()
        assertEquals(boundsBefore.left.value.toDouble(), boundsAfter.left.value.toDouble(), 2.0)
        assertEquals(boundsBefore.top.value.toDouble(), boundsAfter.top.value.toDouble(), 2.0)
    }

    @Test
    fun storedViewportAnchorsAFreshComposition() {
        val rig = setLibraryContent(populatedSnapshot())
        rule.waitForIdle()
        rule.onNodeWithTag("library-people").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        val saved = rig.viewport
        assertNotNull(saved.people.key)
        assertTrue(saved.people.index > 0 || saved.people.offset > 0)

        rig.showLibrary.value = false
        rule.waitForIdle()
        rig.viewport = LibraryViewport()
        rig.snapshot.value = populatedSnapshot(viewport = saved)
        rig.showLibrary.value = true
        rule.waitForIdle()
        assertEquals(saved.people.key, rig.viewport.people.key)
        assertEquals(saved.people.index, rig.viewport.people.index)
        assertEquals(saved.people.offset, rig.viewport.people.offset)
    }

    @Test
    fun insertionBeforeTheAnchorKeepsTheSameRowKey() {
        val rig = setLibraryContent(populatedSnapshot())
        rule.waitForIdle()
        rule.onNodeWithTag("library-people").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        val before = rig.viewport.people
        assertNotNull(before.key)
        assertTrue(before.index > 0 || before.offset > 0)

        rig.snapshot.value = populatedSnapshot().copy(
            cloud = populatedSnapshot().cloud.copy(
                people = listOf(person("new")) + populatedSnapshot().cloud.people
            )
        )
        rule.waitForIdle()
        assertEquals(before.key, rig.viewport.people.key)
        assertEquals(before.index + 1, rig.viewport.people.index)
    }

    @Test
    fun savedGridAnchorResolvesToTheSectionKeyNotTheRawIndex() {
        val saved = LibraryViewport(
            grid = LibraryScrollPosition(key = "CategoriesList", index = 0, offset = 0)
        )
        setLibraryContent(populatedSnapshot(viewport = saved))
        rule.waitForIdle()
        rule.onNodeWithTag("library-categories").assertIsDisplayed()
    }

    @Test
    fun unknownCategoriesRenderNoSectionWhileKnownEmptyShowsPlaceholder() {
        val rig = setLibraryContent(
            populatedSnapshot().copy(categories = null, categoryCount = 0)
        )
        rule.waitForIdle()
        rule.onNodeWithTag("library-categories").assertDoesNotExist()
        rule.onNodeWithText(
            string(Res.string.categorise_your_media)
        ).assertDoesNotExist()

        rig.snapshot.value = populatedSnapshot().copy(
            categories = emptyList(),
            categoryCount = 0
        )
        rule.waitForIdle()
        rule.onNodeWithTag("library-grid").performScrollToNode(
            hasText(string(Res.string.categorise_your_media))
        )
        rule.onNodeWithText(
            string(Res.string.categorise_your_media)
        ).assertIsDisplayed()
    }

    @Test
    fun gridScrollPositionSurvivesNavigation() {
        val rig = setLibraryContent(populatedSnapshot(), contentHeight = 240.dp)
        rule.onNodeWithTag("library-grid").performTouchInput { swipeUp() }
        rule.waitForIdle()

        rule.runOnUiThread { rig.nav!!.navigate("other") }
        rule.onNodeWithText("other-destination").assertIsDisplayed()
        val saved = rig.viewport
        assertTrue(saved.grid.index > 0 || saved.grid.offset > 0)

        rig.viewport = LibraryViewport()
        rig.snapshot.value = populatedSnapshot(viewport = saved)
        rule.runOnUiThread { rig.nav!!.popBackStack() }
        rule.waitForIdle()
        assertTrue(rig.viewport.grid.index > 0 || rig.viewport.grid.offset > 0)
        assertEquals(saved.grid.key, rig.viewport.grid.key)
    }

    private fun Rig.reenterWithFreshEntry(snapshot: LibrarySnapshot) {
        this.snapshot.value = snapshot
        rule.runOnUiThread {
            nav!!.popBackStack("library", inclusive = true)
            nav!!.navigate("library")
        }
        rule.waitForIdle()
    }

    @Test
    fun bareLazyGridAppliesInitialScrollOffset() {
        var observed = -1
        rule.setContent {
            val state = androidx.compose.foundation.lazy.grid.rememberLazyGridState(
                initialFirstVisibleItemIndex = 0,
                initialFirstVisibleItemScrollOffset = 400
            )
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                state = state,
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2)
            ) {
                items(50) { i -> Text("cell-$i", modifier = Modifier.height(200.dp)) }
            }
            androidx.compose.runtime.LaunchedEffect(state) {
                kotlinx.coroutines.delay(500)
                observed = state.firstVisibleItemScrollOffset
            }
        }
        rule.waitUntil("offset read", 5_000) { observed != -1 }
        assertEquals(400, observed)
    }

    @Test
    fun freshCompositionAppliesRestoredGridOffset() {
        val rig = setLibraryContent(populatedSnapshot())
        rule.waitForIdle()
        val signature = rig.viewport.configuration
        assertTrue(signature.isNotEmpty())
        rig.viewport = LibraryViewport()
        rig.reenterWithFreshEntry(
            populatedSnapshot(
                viewport = LibraryViewport(
                    grid = LibraryScrollPosition(
                        key = "libraryShortcuts", index = 0, offset = 400
                    ),
                    configuration = signature
                )
            )
        )
        assertEquals("libraryShortcuts", rig.viewport.grid.key)
        assertEquals(400, rig.viewport.grid.offset)
    }

    @Test
    fun freshCompositionAppliesRestoredGridOffsetUnderEnterTransition() {
        val rig = setLibraryContent(
            populatedSnapshot(),
            withEnterTransition = true
        )
        rule.waitForIdle()
        val signature = rig.viewport.configuration
        rig.viewport = LibraryViewport()
        rig.reenterWithFreshEntry(
            populatedSnapshot(
                viewport = LibraryViewport(
                    grid = LibraryScrollPosition(
                        key = "libraryShortcuts", index = 0, offset = 400
                    ),
                    configuration = signature
                )
            )
        )
        assertEquals("libraryShortcuts", rig.viewport.grid.key)
        assertEquals(400, rig.viewport.grid.offset)
    }

    @Test
    fun freshCompositionAppliesRestoredGridOffsetWithSparseContent() {
        val rig = setLibraryContent(
            LibrarySnapshot(
                locations = (0 until 2).map { locationRow(1_000L + it) },
                locationCount = 2,
                latestGeo = LibraryGeoPreview(media(3L), 52.5, 13.4),
                peopleCount = 0,
                cloud = CloudLibraryState(hasPeople = false, people = emptyList()),
                indicators = LibraryIndicatorState(trashCount = 1, favoriteCount = 2)
            ),
            contentHeight = 240.dp
        )
        rule.waitForIdle()
        val signature = rig.viewport.configuration
        rig.viewport = LibraryViewport()
        rig.reenterWithFreshEntry(
            LibrarySnapshot(
                locations = (0 until 2).map { locationRow(1_000L + it) },
                locationCount = 2,
                latestGeo = LibraryGeoPreview(media(3L), 52.5, 13.4),
                peopleCount = 0,
                cloud = CloudLibraryState(hasPeople = false, people = emptyList()),
                indicators = LibraryIndicatorState(trashCount = 1, favoriteCount = 2),
                viewport = LibraryViewport(
                    grid = LibraryScrollPosition(
                        key = "libraryShortcuts", index = 0, offset = 400
                    ),
                    configuration = signature
                )
            )
        )
        assertEquals("libraryShortcuts", rig.viewport.grid.key)
        assertTrue(rig.viewport.grid.offset > 0)
    }
}
