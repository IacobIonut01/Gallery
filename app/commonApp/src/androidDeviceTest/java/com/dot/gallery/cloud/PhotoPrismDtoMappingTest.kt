/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.photoprism.data.dto.PhotoPrismFileDto
import com.dot.gallery.cloud.photoprism.data.dto.PhotoPrismPhotoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Network-free tests for PhotoPrism photo -> [com.dot.gallery.data.entity.CloudMediaEntity]
 * mapping (UID, Hash→fileId, favorite, mime, thumb/original URL construction).
 */
@RunWith(AndroidJUnit4::class)
class PhotoPrismDtoMappingTest {

    @Test
    fun mapsUidHashFavoriteAndUrls() {
        val dto = PhotoPrismPhotoDto(
            uid = "pse1yzastu36irsj",
            type = "image",
            title = "Winery",
            fileName = "2012/photo.jpg",
            hash = "4bc82c3ea5aaa323aea801fe0125b554af8e49af",
            width = 3368,
            height = 2246,
            takenAt = "2012-08-27T12:40:25Z",
            favorite = true,
            path = "2012",
            originalName = "photo.jpg",
            files = listOf(
                PhotoPrismFileDto(
                    uid = "fse1",
                    hash = "4bc82c3ea5aaa323aea801fe0125b554af8e49af",
                    name = "2012/photo.jpg",
                    size = 2473990,
                    primary = true,
                    mime = "image/jpeg",
                    width = 3368,
                    height = 2246
                )
            )
        )

        val entity = dto.toCloudMediaEntity(
            serverConfigId = 7L,
            baseUrl = "https://pp.test",
            previewToken = "prevtok",
            downloadToken = "dlTok"
        )

        assertEquals("pse1yzastu36irsj", entity.remoteId)
        assertEquals(ProviderType.PHOTOPRISM, entity.providerType)
        assertEquals(7L, entity.serverConfigId)
        assertEquals("photo.jpg", entity.label)
        assertEquals("image/jpeg", entity.mimeType)
        assertTrue(entity.favorite)
        assertFalse(entity.trashed)
        assertEquals("4bc82c3ea5aaa323aea801fe0125b554af8e49af", entity.fileId)
        assertEquals("4bc82c3ea5aaa323aea801fe0125b554af8e49af", entity.contentHash)
        assertEquals(
            "https://pp.test/api/v1/t/4bc82c3ea5aaa323aea801fe0125b554af8e49af/prevtok/fit_720",
            entity.thumbnailUrl
        )
        assertEquals(
            "https://pp.test/api/v1/dl/4bc82c3ea5aaa323aea801fe0125b554af8e49af?t=dlTok",
            entity.originalUrl
        )
        assertEquals(3368, entity.width)
        assertEquals(2246, entity.height)
        assertTrue(entity.timestamp > 0L)
    }

    @Test
    fun fallsBackToPrimaryFileHashWhenTopLevelHashBlank() {
        val dto = PhotoPrismPhotoDto(
            uid = "p1",
            hash = "",
            files = listOf(
                PhotoPrismFileDto(hash = "abc123", primary = true, mime = "image/png")
            )
        )
        val entity = dto.toCloudMediaEntity(1L, "https://pp.test", "p", "d")
        assertEquals("abc123", entity.fileId)
        assertEquals("image/png", entity.mimeType)
    }

    @Test
    fun videoTypeGetsVideoMimeWhenMimeMissing() {
        val dto = PhotoPrismPhotoDto(uid = "v1", type = "video", hash = "h1")
        val entity = dto.toCloudMediaEntity(1L, "https://pp.test", "p", "d")
        assertEquals("video/mp4", entity.mimeType)
    }
}
