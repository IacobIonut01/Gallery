package com.dot.gallery.domain.model

import androidx.compose.runtime.Immutable
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.dot.gallery.domain.parcel.Parcelable
import com.dot.gallery.domain.parcel.Parcelize
import com.dot.gallery.domain.util.volume

@Entity(tableName = "blacklist")
@Parcelize
@Immutable
data class IgnoredAlbum(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val label: String? = null,
    val wildcard: String? = null,
    @ColumnInfo(defaultValue = "[]")
    val albumIds: List<Long> = emptyList(),
    @ColumnInfo(defaultValue = ALBUMS_ONLY.toString())
    val location: Int = ALBUMS_ONLY,
    @ColumnInfo(defaultValue = "[]")
    val matchedAlbums: List<String> = emptyList()
) : Parcelable {

    fun matchesMedia(media: Media): Boolean =
        matches(
            id = media.albumID,
            path = media.path,
            relativePath = media.relativePath,
            volume = media.volume,
            shouldRemove = hiddenInTimeline
        )

    fun matchesAlbum(album: Album): Boolean =
        matches(
            id = album.id,
            path = album.pathToThumbnail,
            relativePath = album.relativePath,
            volume = album.volume,
            shouldRemove = hiddenInAlbums
        )

    /**
     * Same matching as [matchesMedia]/[matchesAlbum] but gated on [hiddenInTimeline], used to
     * resolve which cloud albums a blacklist entry hides from the unified timeline. Cloud
     * media carries a constant shared albumID, so the timeline filter cannot go through
     * [matchesMedia]; it resolves the matching cloud albums and removes their members.
     */
    fun matchesForTimeline(id: Long, path: String, relativePath: String, volume: String): Boolean =
        matches(
            id = id,
            path = path,
            relativePath = relativePath,
            volume = volume,
            shouldRemove = hiddenInTimeline
        )

    private fun matches(
        id: Long,
        path: String,
        relativePath: String,
        volume: String,
        shouldRemove: Boolean
    ): Boolean {
        if (!shouldRemove) return false

        // Check if ID matches directly
        val matchesId = this.id == id
        if (matchesId) return true

        // Check if ID is in the albumIds list
        if (albumIds.isNotEmpty() && albumIds.contains(id)) return true

        // Check regex pattern
        val regex = wildcard?.toRegex()
        return regex?.let {
            path.matches(it) || relativePath.matches(it) || volume.matches(it)
        } ?: false
    }

    companion object {
        const val ALBUMS_ONLY = 0
        const val TIMELINE_ONLY = 1
        const val ALBUMS_AND_TIMELINE = 2
    }
}

private val IgnoredAlbum.hiddenInBoth get() = location == IgnoredAlbum.ALBUMS_AND_TIMELINE
val IgnoredAlbum.hiddenInAlbums get() = location == IgnoredAlbum.ALBUMS_ONLY || hiddenInBoth
val IgnoredAlbum.hiddenInTimeline get() = location == IgnoredAlbum.TIMELINE_ONLY || hiddenInBoth

fun Regex.matchesAlbum(album: Album): Boolean {
    return album.pathToThumbnail.matches(this) || album.relativePath.matches(this) || album.volume.matches(
        this
    )
}

fun IgnoredAlbum.shouldIgnore(media: Media, albumId: Long = -1L) =
    matchesMedia(media) && (hiddenInTimeline && albumId == -1L || hiddenInAlbums && albumId != -1L)
