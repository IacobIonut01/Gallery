package com.dot.gallery.app.presentation.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.request.ComposableImageRequest
import com.dot.gallery.domain.model.GeoMedia
import com.dot.gallery.domain.util.getUri
import com.dot.gallery.feature.editor.adjustments.Crop
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.map_cluster_count
import com.dot.gallery.generated.resources.map_cluster_title



@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MapClusterSheet(
    media: List<GeoMedia>,
    onDismiss: () -> Unit,
    onMediaClick: (GeoMedia) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.map_cluster_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                text = stringResource(Res.string.map_cluster_count, media.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(96.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(media, key = { it.mediaId }) { item ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onMediaClick(item) },
                    ) {
                        AsyncImage(
                            request = ComposableImageRequest(item.media.getUri().toString()) {
                                setExtra(key = "mediaSignature", value = item.media.toString())
                            },
                            contentDescription = item.media.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
