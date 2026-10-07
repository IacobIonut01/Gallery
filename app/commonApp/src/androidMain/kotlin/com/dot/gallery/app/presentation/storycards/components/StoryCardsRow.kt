/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.storycards.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dot.gallery.domain.model.StoryCard
import com.dot.gallery.domain.model.StoryCardType
import com.dot.gallery.domain.util.getUri
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.sketch.resize.Precision
import com.dot.gallery.ui.theme.BlackScrim
import com.dot.gallery.core.presentation.components.shape
import com.dot.gallery.feature.editor.adjustments.Crop
import com.dot.gallery.core.presentation.util.storyCardSharedElement
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.story_type_albums
import com.dot.gallery.generated.resources.story_type_categories
import com.dot.gallery.generated.resources.story_type_cloud_memories
import com.dot.gallery.generated.resources.story_type_favorites
import com.dot.gallery.generated.resources.story_type_locations
import com.dot.gallery.generated.resources.story_type_memories
import org.jetbrains.compose.resources.StringResource


const val StoryCardsRowTag = "StoryCards.Row"
fun storyCardTag(id: Long) = "StoryCards.Card.$id"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun StoryCardsRow(
    cards: List<StoryCard>,
    onCardClick: (card: StoryCard) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp)
) {
    if (cards.isEmpty()) return

    LazyRow(
        modifier = modifier.fillMaxWidth().testTag(StoryCardsRowTag),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = cards,
            key = { card -> card.id }
        ) { card ->
            val sharedModifier = if (
                sharedTransitionScope != null && animatedVisibilityScope != null
            ) {
                with(sharedTransitionScope) {
                    Modifier.storyCardSharedElement(
                        cardId = card.id,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                }
            } else {
                Modifier
            }
            StoryCardItem(
                card = card,
                onClick = { onCardClick(card) },
                modifier = sharedModifier.testTag(storyCardTag(card.id)),
            )
        }
    }
}

@Composable
private fun StoryCardItem(
    card: StoryCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeLabel = stringResource(card.type.displayNameRes)
    val cardDescription = listOfNotNull(typeLabel, card.title, card.subtitle).joinToString(". ")

    Box(
        modifier = modifier
            .width(148.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = cardDescription
            }
    ) {
        val thumb = card.thumbnailMedia
        if (thumb != null) {
            AsyncImage(
                request = ComposableImageRequest(thumb.getUri().toString()) {
                    resize(width = 300, height = 440, precision = Precision.LESS_PIXELS)
                    crossfade(false)
                },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = card.type.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BlackScrim
                        )
                    )
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )
            val subtitle = card.subtitle
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Type indicator badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(4.dp)
        ) {
            Icon(
                imageVector = card.type.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
        }
    }
}

private val StoryCardType.icon: ImageVector
    get() = when (this) {
        StoryCardType.MEMORIES -> Icons.Outlined.History
        StoryCardType.ALBUMS -> Icons.Outlined.PhotoAlbum
        StoryCardType.CATEGORIES -> Icons.Outlined.ImageSearch
        StoryCardType.LOCATIONS -> Icons.Outlined.LocationOn
        StoryCardType.FAVORITES -> Icons.Outlined.Favorite
        StoryCardType.CLOUD_MEMORIES -> Icons.Outlined.Cloud
    }

private val StoryCardType.displayNameRes: StringResource
    get() = when (this) {
        StoryCardType.MEMORIES -> Res.string.story_type_memories
        StoryCardType.ALBUMS -> Res.string.story_type_albums
        StoryCardType.CATEGORIES -> Res.string.story_type_categories
        StoryCardType.LOCATIONS -> Res.string.story_type_locations
        StoryCardType.FAVORITES -> Res.string.story_type_favorites
        StoryCardType.CLOUD_MEMORIES -> Res.string.story_type_cloud_memories
    }
