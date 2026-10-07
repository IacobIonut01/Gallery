package com.dot.gallery.app.presentation.albumtimeline

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.dot.gallery.core.AlbumMediaLoadMode
import com.dot.gallery.core.MediaDistributor
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.feature.albums.ui.AlbumTimelineViewModel
import com.dot.gallery.app.presentation.util.MockedMediaDistributor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.core.presentation.util.clear



@OptIn(ExperimentalCoroutinesApi::class)
class AlbumTimelineViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val stores = mutableListOf<ViewModelStore>()

    @Before
    fun setMain() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() {
        stores.forEach { it.clear() }
        stores.clear()
        Dispatchers.resetMain()
    }

    private fun newStore() = ViewModelStore().also { stores += it }

    private class CountingDistributor : MockedMediaDistributor() {
        val calls = mutableListOf<Pair<Long, AlbumMediaLoadMode>>()
        var starts = 0
        var completions = 0
        val source = MutableStateFlow(MediaState<UriMedia>(isLoading = false, dateHeader = "v0"))

        override fun albumTimelineMediaFlow(
            albumId: Long,
            loadMode: AlbumMediaLoadMode
        ): Flow<MediaState<UriMedia>> {
            calls += albumId to loadMode
            return source
                .onStart { starts++ }
                .onCompletion { completions++ }
        }
    }

    private fun viewModel(
        store: ViewModelStore,
        distributor: MediaDistributor,
        albumId: Long
    ): AlbumTimelineViewModel {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AlbumTimelineViewModel(
                    SavedStateHandle(mapOf("albumId" to albumId)),
                    distributor,
                    trace = {},
                ) as T
        }
        return ViewModelProvider(store, factory)[AlbumTimelineViewModel::class.java]
    }

    @Test
    fun completeModeSourceStartsOnceAndSurvivesUiCollectorChurn() = runTest(dispatcher) {
        val distributor = CountingDistributor()
        val store = newStore()
        val vm = viewModel(store, distributor, 42L)
        advanceUntilIdle()

        assertEquals(listOf(42L to AlbumMediaLoadMode.Complete), distributor.calls)
        assertEquals(1, distributor.starts)
        assertEquals("v0", vm.mediaState.value.dateHeader)
        assertFalse(vm.mediaState.value.isLoading)

        val job1 = launch { vm.mediaState.collect() }
        val job2 = launch { vm.mediaState.collect() }
        advanceUntilIdle()
        job1.cancel()
        job2.cancel()
        dispatcher.scheduler.advanceTimeBy(10_000)
        advanceUntilIdle()

        assertEquals(1, distributor.starts)
        assertEquals(0, distributor.completions)
        assertEquals("v0", vm.mediaState.value.dateHeader)
        assertFalse(vm.mediaState.value.isLoading)
    }

    @Test
    fun upstreamUpdateReachesStateWhileNoUiCollectorExists() = runTest(dispatcher) {
        val distributor = CountingDistributor()
        val store = newStore()
        val vm = viewModel(store, distributor, 7L)
        advanceUntilIdle()

        val updated = MediaState<UriMedia>(isLoading = false, dateHeader = "v1")
        distributor.source.value = updated
        advanceUntilIdle()

        assertSame(updated, vm.mediaState.value)
    }

    @Test
    fun clearingOwnerStoreStopsTheSource() = runTest(dispatcher) {
        val distributor = CountingDistributor()
        val store = newStore()
        viewModel(store, distributor, 9L)
        advanceUntilIdle()
        assertEquals(1, distributor.starts)

        store.clear()
        advanceUntilIdle()

        assertEquals(1, distributor.completions)
    }

    @Test
    fun separateOwnerEntriesDoNotShareAlbumState() = runTest(dispatcher) {
        val distributor = CountingDistributor()
        val storeA = newStore()
        val storeB = newStore()
        val vmA = viewModel(storeA, distributor, 1L)
        val vmB = viewModel(storeB, distributor, 2L)
        advanceUntilIdle()

        assertNotSame(vmA, vmB)
        assertEquals(1L, vmA.albumId)
        assertEquals(2L, vmB.albumId)
        assertEquals(
            listOf(1L to AlbumMediaLoadMode.Complete, 2L to AlbumMediaLoadMode.Complete),
            distributor.calls
        )
        assertEquals(2, distributor.starts)
    }

    @Test
    fun sameOwnerEntryReturnsSameViewModel() = runTest(dispatcher) {
        val distributor = CountingDistributor()
        val store = newStore()
        val first = viewModel(store, distributor, 5L)
        val second = viewModel(store, distributor, 5L)
        advanceUntilIdle()

        assertSame(first, second)
        assertEquals(1, distributor.calls.size)
        assertEquals(1, distributor.starts)
    }
}
