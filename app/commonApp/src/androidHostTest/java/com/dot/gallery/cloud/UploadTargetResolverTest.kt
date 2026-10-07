/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.UploadTargetResolver
import com.dot.gallery.data.entity.CloudServerConfigEntity
import com.dot.gallery.data.entity.CloudUploadPrefEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.dot.gallery.domain.util.isVideo


class UploadTargetResolverTest {

    @Test
    fun blankEverythingFallsBackToProviderDefault() {
        assertNull(UploadTargetResolver.resolve(config(), pref(albumLabel = ""), isVideo = false))
        assertNull(UploadTargetResolver.resolve(null, pref(albumLabel = ""), isVideo = false))
    }

    @Test
    fun albumLabelAloneBecomesTheTargetFolder() {
        assertEquals(
            "Camera",
            UploadTargetResolver.resolve(config(), pref(albumLabel = "Camera"), isVideo = false)
        )
    }

    @Test
    fun accountBaseIsJoinedWithAlbumLabel() {
        val config = config(uploadBasePath = "Backups/Phone")

        assertEquals(
            "Backups/Phone/Camera",
            UploadTargetResolver.resolve(config, pref(albumLabel = "Camera"), isVideo = false)
        )
    }

    @Test
    fun blankAlbumLabelUsesAccountBaseVerbatim() {
        val config = config(uploadBasePath = "Backups")

        assertEquals(
            "Backups",
            UploadTargetResolver.resolve(config, pref(albumLabel = " "), isVideo = false)
        )
    }

    @Test
    fun videosUseVideosBaseWhenConfigured() {
        val config = config(uploadBasePath = "Photos", uploadVideosPath = "Videos")

        assertEquals(
            "Videos/Camera",
            UploadTargetResolver.resolve(config, pref(albumLabel = "Camera"), isVideo = true)
        )
        assertEquals(
            "Photos/Camera",
            UploadTargetResolver.resolve(config, pref(albumLabel = "Camera"), isVideo = false)
        )
    }

    @Test
    fun videosFallBackToPhotosBaseWhenVideosBaseIsBlank() {
        val config = config(uploadBasePath = "Photos")

        assertEquals(
            "Photos/Camera",
            UploadTargetResolver.resolve(config, pref(albumLabel = "Camera"), isVideo = true)
        )
    }

    @Test
    fun customPathOverridesBaseAndAlbumLabel() {
        val config = config(uploadBasePath = "Photos", uploadVideosPath = "Videos")
        val pref = pref(albumLabel = "Camera", customPath = "Archive/2024")

        assertEquals(
            "Archive/2024",
            UploadTargetResolver.resolve(config, pref, isVideo = false)
        )
        // The override is verbatim — the photo/video split does not apply under it.
        assertEquals(
            "Archive/2024",
            UploadTargetResolver.resolve(config, pref, isVideo = true)
        )
    }

    @Test
    fun customPathStillAppliesWhenConfigIsMissing() {
        assertEquals(
            "Archive",
            UploadTargetResolver.resolve(null, pref(customPath = "Archive"), isVideo = false)
        )
    }

    @Test
    fun unsanitarySegmentsCannotEscapeTheFilesRoot() {
        val pref = pref(customPath = "../outside/../../root")

        assertEquals(
            "outside/root",
            UploadTargetResolver.resolve(config(), pref, isVideo = false)
        )
        assertEquals("a/b/c", UploadTargetResolver.sanitizePath("\\a//b/./c/"))
        assertEquals("", UploadTargetResolver.sanitizePath(" ../../ "))
        assertEquals("", UploadTargetResolver.sanitizePath("///"))
    }

    @Test
    fun userEnteredBasePathsAreNormalized() {
        val config = config(uploadBasePath = " \\Backups\\Phone// ")

        assertEquals(
            "Backups/Phone/Camera",
            UploadTargetResolver.resolve(config, pref(albumLabel = "Camera"), isVideo = false)
        )
    }

    private fun config(
        uploadBasePath: String = "",
        uploadVideosPath: String = ""
    ) = CloudServerConfigEntity(
        id = 7L,
        providerType = ProviderType.WEBDAV,
        serverUrl = "https://dav.example",
        uploadBasePath = uploadBasePath,
        uploadVideosPath = uploadVideosPath
    )

    private fun pref(
        albumLabel: String = "Camera",
        customPath: String = ""
    ) = CloudUploadPrefEntity(
        serverConfigId = 7L,
        albumId = 10L,
        providerType = ProviderType.WEBDAV,
        albumLabel = albumLabel,
        uploadEnabled = true,
        customPath = customPath
    )
}
