/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.util

import com.dot.gallery.domain.model.MediaItem
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.domain.model.UriMedia

fun MediaState<UriMedia>.applyOptimisticMutations(
    pendingRemovalIds: Set<Long>,
    favoriteOverrides: Map<Long, Boolean>,
    dropUnfavorited: Boolean = false,
): MediaState<UriMedia> {
    if (pendingRemovalIds.isEmpty() && favoriteOverrides.isEmpty()) return this

    fun UriMedia.mutated(): UriMedia? {
        if (id in pendingRemovalIds) return null
        val effective = favoriteOverrides[id]?.let { fav ->
            if ((favorite == 1) != fav) copy(favorite = if (fav) 1 else 0) else this
        } ?: this
        return if (dropUnfavorited && effective.favorite != 1) null else effective
    }

    // Mutate groups first — representative promotion depends on surviving members.
    // promoted maps a removed representative's id to its replacement media.
    val promoted = HashMap<Long, UriMedia>()
    val newGroups = HashMap<Long, List<UriMedia>>(mediaGroups.size)
    mediaGroups.forEach { (repId, members) ->
        val surviving = members.mapNotNull { it.mutated() }
        if (surviving.isNotEmpty()) {
            val rep = if (surviving.any { it.id == repId }) {
                surviving.first { it.id == repId }
            } else {
                surviving.first().also { promoted[repId] = it }
            }
            if (surviving.size > 1) newGroups[rep.id] = surviving
        }
    }

    fun List<MediaItem<UriMedia>>.mutatedItems(): List<MediaItem<UriMedia>> {
        val out = ArrayList<MediaItem<UriMedia>>(size)
        var heldHeaders: MutableList<MediaItem.Header<UriMedia>>? = null
        for (item in this) {
            when (item) {
                is MediaItem.Header -> {
                    if (heldHeaders == null) heldHeaders = ArrayList(2)
                    heldHeaders.add(
                        if (pendingRemovalIds.isEmpty()) item
                        else item.copy(data = item.data - pendingRemovalIds)
                    )
                }
                is MediaItem.MediaViewItem -> {
                    val replacement = item.media.mutated() ?: promoted[item.media.id]
                    if (replacement != null) {
                        heldHeaders?.let { out.addAll(it); heldHeaders = null }
                        val group = newGroups[replacement.id]
                        out.add(
                            item.copy(
                                media = replacement,
                                stackCount = group?.size ?: 1,
                                isCloudGroup = item.isCloudGroup && group != null
                            )
                        )
                    }
                }
            }
        }
        return out
    }

    val newMedia = media.mapNotNull { it.mutated() }
    val newPagerMedia = pagerMedia.mapNotNull { it.mutated() ?: promoted[it.id] }
    val newHeaders = headers.mapNotNull { header ->
        header.copy(data = header.data - pendingRemovalIds)
            .takeIf { it.data.isNotEmpty() }
    }
    val newCloudBackups = cloudBackups.mapNotNull { (localId, copies) ->
        if (localId in pendingRemovalIds) return@mapNotNull null
        val mutatedCopies = copies.mapNotNull { it.mutated() }
        if (mutatedCopies.isEmpty()) null else localId to mutatedCopies
    }.toMap()

    return copy(
        media = newMedia,
        pagerMedia = newPagerMedia,
        mediaGroups = newGroups,
        mappedMedia = mappedMedia.mutatedItems(),
        mappedMediaWithMonthly = mappedMediaWithMonthly.mutatedItems(),
        mappedMediaWithYearly = mappedMediaWithYearly.mutatedItems(),
        headers = newHeaders,
        cloudBackups = newCloudBackups
    )
}
