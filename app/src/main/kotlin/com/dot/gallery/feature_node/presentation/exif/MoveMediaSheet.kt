package com.dot.gallery.feature_node.presentation.exif

import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Constants.albumCellsList
import com.dot.gallery.core.LocalMediaHandler
import com.dot.gallery.core.Settings.Album.rememberAlbumGridSize
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.core.presentation.components.SecurityInfoSheet
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.AlbumGroupWithAlbums
import com.dot.gallery.feature_node.domain.model.AlbumState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaMutationResult
import com.dot.gallery.feature_node.domain.util.isCloud
import com.dot.gallery.feature_node.domain.util.mediaStoreVolumeName
import com.dot.gallery.feature_node.domain.util.resolveMediaStoreVolume
import com.dot.gallery.feature_node.presentation.albums.components.AlbumComponent
import com.dot.gallery.feature_node.presentation.albums.components.AlbumGroupComponent
import com.dot.gallery.feature_node.presentation.util.AppBottomSheetState
import com.dot.gallery.feature_node.presentation.util.launchWriteRequest
import com.dot.gallery.feature_node.presentation.util.rememberActivityResult
import com.dot.gallery.feature_node.presentation.util.rememberAppBottomSheetState
import com.dot.gallery.feature_node.presentation.util.toastError
import com.dot.gallery.feature_node.presentation.util.writeRequest
import com.dot.gallery.feature_node.presentation.vault.utils.rememberBiometricState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T: Media> MoveMediaSheet(
    sheetState: AppBottomSheetState,
    mediaList: List<T>,
    albumState: State<AlbumState>,
    onFinish: () -> Unit,
) {
    val handler = LocalMediaHandler.current
    val context = LocalContext.current
    val hasAllFilesAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else true
    val hasFullMediaAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hasAllFilesAccess || MediaStore.canManageMedia(context)
    } else hasAllFilesAccess
    val toastError = toastError()

    val scope = rememberCoroutineScope()
    var progress by remember(mediaList) { mutableFloatStateOf(0f) }
    var newPath by remember(mediaList) { mutableStateOf("") }

    val newAlbumSheetState = rememberAppBottomSheetState()
    val securitySheetState = rememberAppBottomSheetState()
    var pendingLockedAlbumPath by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val sources = mediaList.filter { !it.isCloud }.map {
        AlbumDestinationSource(it.mediaStoreVolumeName, it.relativePath)
    }

    fun Album.isMoveDestinationEnabled(): Boolean = absolutePath.isNotBlank() &&
        isAlbumMoveDestinationEnabled(
            hasFullMediaAccess = hasFullMediaAccess,
            albumVolume = volume,
            albumRelativePath = relativePath,
            sources = sources,
            isCloudAlbum = uri.scheme == "cloud" || relativePath.startsWith("cloud/"),
        )

    fun localMediaForDestination(path: String): List<T> {
        val (destinationVolume, destinationRelativePath) = resolveMediaStoreVolume(path)
        return mediaList.filter {
            !it.isCloud && (it.mediaStoreVolumeName != destinationVolume ||
                normalizeMediaStoreRelativePath(it.relativePath) !=
                normalizeMediaStoreRelativePath(destinationRelativePath))
        }
    }

    val doMove: () -> Unit = {
        scope.launch {
            val localMedia = localMediaForDestination(newPath)
            val done = async {
                localMedia.forEachIndexed { index, it ->
                    if (handler.moveMedia(media = it, newPath = newPath)) {
                        val movedFilePath = newPath.trimEnd('/') + "/" + it.label
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(movedFilePath),
                            arrayOf(it.mimeType),
                            null
                        )
                        progress = index.toFloat() / mediaList.size
                    } else {
                        return@async false
                    }
                }
                return@async true
            }
            if (done.await()) {
                context.contentResolver.notifyChange(
                    MediaStore.Files.getContentUri("external"), null
                )
                sheetState.hide()
                onFinish()
            } else {
                toastError.show()
                delay(1000)
                sheetState.hide()
            }
        }
    }

    val request = rememberActivityResult { doMove() }

    var pendingCopyUris by rememberSaveable(mediaList) { mutableStateOf<List<String>>(emptyList()) }
    var restrictedMoveJob by remember(mediaList) { mutableStateOf<Job?>(null) }

    val finishMove: () -> Unit = {
        scope.launch {
            context.contentResolver.notifyChange(
                MediaStore.Files.getContentUri("external"), null
            )
            pendingCopyUris = emptyList()
            sheetState.hide()
            onFinish()
        }
    }

    val deleteRequest = rememberActivityResult(
        onResultCanceled = {
            // The originals stay where they are, so drop the copies: a cancelled move must
            // not leave a duplicate behind.
            scope.launch {
                handler.discardMediaCopies(pendingCopyUris.map(Uri::parse))
                pendingCopyUris = emptyList()
                progress = 0f
                sheetState.hide()
            }
        },
        onResultOk = finishMove
    )

    /**
     * Media stored in another app's `Android/media/<package>` folder cannot be renamed into an
     * album. Copy the selection to the destination first, then let the system ask the user to
     * confirm the removal of the originals - a single dialog for the whole batch.
     */
    fun startRestrictedMove(albumPath: String, localMedia: List<T>) {
        if (restrictedMoveJob?.isActive == true) return
        progress = 0.001f
        restrictedMoveJob = scope.launch {
            var copies = emptyList<Uri>()
            var copyOwnershipTransferred = false
            try {
                copies = handler.copyMediaForMove(localMedia, albumPath) { copyProgress ->
                    withContext(Dispatchers.Main) { progress = copyProgress }
                }
                if (copies.isEmpty()) {
                    progress = 0f
                    toastError.show()
                    delay(1000)
                    sheetState.hide()
                    return@launch
                }
                pendingCopyUris = copies.map(Uri::toString)
                when (handler.deleteMedia(deleteRequest, localMedia)) {
                    // The dialog drives the rest through deleteRequest.
                    MediaMutationResult.REQUEST_LAUNCHED -> copyOwnershipTransferred = true
                    MediaMutationResult.COMPLETED -> {
                        copyOwnershipTransferred = true
                        finishMove()
                    }
                    MediaMutationResult.FAILED -> {
                        withContext(NonCancellable) { handler.discardMediaCopies(copies) }
                        copies = emptyList()
                        pendingCopyUris = emptyList()
                        progress = 0f
                        toastError.show()
                        delay(1000)
                        sheetState.hide()
                    }
                }
            } catch (e: CancellationException) {
                if (!copyOwnershipTransferred) {
                    if (copies.isNotEmpty()) {
                        withContext(NonCancellable) { handler.discardMediaCopies(copies) }
                    }
                    pendingCopyUris = emptyList()
                }
                throw e
            } catch (e: Exception) {
                withContext(NonCancellable) { handler.discardMediaCopies(copies) }
                pendingCopyUris = emptyList()
                progress = 0f
                toastError.show()
                delay(1000)
                sheetState.hide()
            } finally {
                restrictedMoveJob = null
            }
        }
    }

    fun startMove(albumPath: String) {
        val cloudMedia = mediaList.filter { it.isCloud }
        val localMedia = localMediaForDestination(albumPath)
        val isRestrictedMove = restrictedMoveSources(hasFullMediaAccess, localMedia).isNotEmpty()
        if (isRestrictedMove && restrictedMoveJob?.isActive == true) return
        // For cloud media: copy to local destination (download + insert into MediaStore)
        if (cloudMedia.isNotEmpty()) {
            scope.launch { handler.copyMedia(*cloudMedia.map { it to albumPath }.toTypedArray()) }
        }
        // For local media: use the standard write-request move flow
        if (localMedia.isNotEmpty()) {
            if (isRestrictedMove) {
                startRestrictedMove(albumPath, localMedia)
            } else {
                scope.launch(Dispatchers.Main) {
                    newPath = albumPath
                    request.launchWriteRequest(
                        localMedia.writeRequest(context.contentResolver),
                        doMove
                    )
                }
            }
        } else {
            // All cloud — just finish after enqueue
            scope.launch {
                sheetState.hide()
                onFinish()
            }
        }
    }

    val biometricState = rememberBiometricState(
        title = stringResource(R.string.biometric_authentication),
        subtitle = stringResource(R.string.unlock_album_biometric_subtitle),
        onSuccess = {
            pendingLockedAlbumPath?.let { path ->
                startMove(path)
            }
            pendingLockedAlbumPath = null
        },
        onFailed = {
            pendingLockedAlbumPath = null
        }
    )

    if (sheetState.isVisible) {
        ModalBottomSheet(
            sheetState = sheetState.sheetState,
            onDismissRequest = {
                restrictedMoveJob?.cancel()
                scope.launch {
                    sheetState.hide()
                }
            },
            dragHandle = { DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .imePadding()
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.move_to_another_album),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                )

                AnimatedVisibility(
                    visible = progress == 0f,
                    enter = Constants.Animation.enterAnimation,
                    exit = Constants.Animation.exitAnimation
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        placeholder = { Text(stringResource(R.string.search_albums)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = stringResource(R.string.search)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = null
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                AnimatedVisibility(
                    visible = progress > 0f,
                    modifier = Modifier
                        .padding(32.dp)
                        .padding(bottom = 64.dp)
                        .navigationBarsPadding()
                        .size(128.dp)
                        .align(Alignment.CenterHorizontally),
                    enter = Constants.Animation.enterAnimation,
                    exit = Constants.Animation.exitAnimation
                ) {
                    CircularProgressIndicator(
                        progress = {
                            progress
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                val albumSize by rememberAlbumGridSize()
                AnimatedVisibility(
                    visible = progress == 0f,
                    enter = Constants.Animation.enterAnimation,
                    exit = Constants.Animation.exitAnimation
                ) {
                    val allGroups = albumState.value.albumGroups
                    val groupedAlbumIds = remember(allGroups) {
                        allGroups.flatMap { g -> g.albums.map { it.id } }.toSet()
                    }
                    val allUngroupedAlbums = remember(albumState.value.albums, groupedAlbumIds) {
                        albumState.value.albums.filter { it.id !in groupedAlbumIds }
                    }
                    var selectedGroup by remember { mutableStateOf<AlbumGroupWithAlbums?>(null) }
                    // Keep selectedGroup in sync with latest data
                    val liveSelectedGroup = selectedGroup?.let { sel ->
                        allGroups.find { it.group.id == sel.group.id }
                    }

                    val query = searchQuery.trim()
                    val filteredGroups = remember(allGroups, query) {
                        if (query.isEmpty()) allGroups
                        else allGroups.mapNotNull { g ->
                            val matched = g.albums.filter { it.label.contains(query, ignoreCase = true) }
                            if (matched.isNotEmpty()) g.copy(albums = matched)
                            else if (g.group.label.contains(query, ignoreCase = true)) g
                            else null
                        }
                    }
                    val filteredUngroupedAlbums = remember(allUngroupedAlbums, query) {
                        if (query.isEmpty()) allUngroupedAlbums
                        else allUngroupedAlbums.filter { it.label.contains(query, ignoreCase = true) }
                    }
                    val filteredGroupAlbums = remember(liveSelectedGroup, query) {
                        val albums = liveSelectedGroup?.albums ?: emptyList()
                        if (query.isEmpty()) albums
                        else albums.filter { it.label.contains(query, ignoreCase = true) }
                    }

                    LazyVerticalGrid(
                        state = rememberLazyGridState(),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        columns = albumCellsList[albumSize],
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            bottom = WindowInsets.navigationBars.getBottom(
                                LocalDensity.current
                            ).dp
                        )
                    ) {
                        if (liveSelectedGroup != null) {
                            // Group detail view
                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "group_back_header"
                            ) {
                                PickerGroupBackHeader(
                                    group = liveSelectedGroup,
                                    onBack = {
                                        selectedGroup = null
                                        searchQuery = ""
                                    }
                                )
                            }
                            items(
                                items = filteredGroupAlbums,
                                key = { item -> "group_album_${item.id}" }
                            ) { item ->
                                AlbumComponent(
                                    modifier = Modifier.animateItem(),
                                    album = item,
                                    isEnabled = item.isMoveDestinationEnabled(),
                                    onItemClick = { album ->
                                        if (album.isLocked) {
                                            if (!biometricState.isSupported) {
                                                scope.launch { securitySheetState.show() }
                                            } else {
                                                pendingLockedAlbumPath = album.absolutePath
                                                biometricState.authenticate()
                                            }
                                        } else {
                                            startMove(album.absolutePath)
                                        }
                                    }
                                )
                            }
                        } else {
                            // Main view: New Album + groups + ungrouped albums
                            if (query.isEmpty()) {
                                item {
                                    AlbumComponent(
                                        album = Album.NewAlbum,
                                        isEnabled = true,
                                        onItemClick = {
                                            scope.launch(Dispatchers.Main) {
                                                newAlbumSheetState.show()
                                            }
                                        }
                                    )
                                }
                            }

                            items(
                                items = filteredGroups,
                                key = { group -> "group_${group.group.id}" }
                            ) { group ->
                                AlbumGroupComponent(
                                    modifier = Modifier.animateItem(),
                                    groupWithAlbums = group,
                                    onGroupClick = { selectedGroup = it }
                                )
                            }

                            items(
                                items = filteredUngroupedAlbums,
                                key = { item -> item.toString() }
                            ) { item ->
                                AlbumComponent(
                                    album = item,
                                    isEnabled = item.isMoveDestinationEnabled(),
                                    onItemClick = { album ->
                                        if (album.isLocked) {
                                            if (!biometricState.isSupported) {
                                                scope.launch { securitySheetState.show() }
                                            } else {
                                                pendingLockedAlbumPath = album.absolutePath
                                                biometricState.authenticate()
                                            }
                                        } else {
                                            startMove(album.absolutePath)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    SecurityInfoSheet(sheetState = securitySheetState)

    AddAlbumSheet(
        sheetState = newAlbumSheetState,
        onFinish = { newAlbum ->
            // Same routing as an existing album so restricted sources reach the copy +
            // delete-request path here too.
            startMove(resolveNewAlbumMovePath(newAlbum, hasAllFilesAccess))
        },
        onCancel = {
            if (newAlbumSheetState.isVisible) {
                scope.launch(Dispatchers.Main) {
                    newAlbumSheetState.hide()
                }
            }
        }
    )
}

internal data class AlbumDestinationSource(
    val volume: String,
    val relativePath: String,
)

/**
 * A copy only reads the source through MediaStore and inserts a brand new file at the
 * destination, so restricted sources (another app's `Android/media/<package>` folder, or a
 * different storage volume) do not need write access. Only the destination has to be writable.
 */
internal fun isAlbumCopyDestinationEnabled(
    hasFullMediaAccess: Boolean,
    albumRelativePath: String,
    isCloudAlbum: Boolean = false,
): Boolean {
    if (isCloudAlbum) return false
    if (hasFullMediaAccess) return true
    return !albumRelativePath.isAndroidMediaPath()
}

/**
 * A move needs the original gone once the destination holds it. Sources inside another app's
 * `Android/media/<package>` folder cannot be renamed, so they take the copy + delete-request
 * path instead: the destination rules are the same as for a copy, plus a source that actually
 * differs from the destination folder.
 */
internal fun isAlbumMoveDestinationEnabled(
    hasFullMediaAccess: Boolean,
    albumVolume: String,
    albumRelativePath: String,
    sources: List<AlbumDestinationSource>,
    isCloudAlbum: Boolean = false,
): Boolean {
    if (!isAlbumCopyDestinationEnabled(hasFullMediaAccess, albumRelativePath, isCloudAlbum)) {
        return false
    }
    if (!hasFullMediaAccess && sources.any { it.volume != albumVolume }) return false
    return sources.isEmpty() || sources.any {
        it.volume != albumVolume ||
            normalizeMediaStoreRelativePath(it.relativePath) !=
            normalizeMediaStoreRelativePath(albumRelativePath)
    }
}

/** Sources that only a copy + delete request can take out of their folder. */
internal fun <T : Media> restrictedMoveSources(
    hasFullMediaAccess: Boolean,
    mediaList: List<T>,
): List<T> = if (hasFullMediaAccess) emptyList()
    else mediaList.filter { it.relativePath.isAndroidMediaPath() }

internal fun String.isAndroidMediaPath(): Boolean = normalizeMediaStoreRelativePath(this).let {
    it == "android/media" || it.startsWith("android/media/")
}

internal fun normalizeMediaStoreRelativePath(path: String): String = path
    .split('/')
    .filter(String::isNotBlank)
    .joinToString("/")
    .lowercase()

internal fun resolveNewAlbumMovePath(albumName: String, hasAllFilesAccess: Boolean): String =
    if (hasAllFilesAccess) albumName else "Pictures/$albumName"

