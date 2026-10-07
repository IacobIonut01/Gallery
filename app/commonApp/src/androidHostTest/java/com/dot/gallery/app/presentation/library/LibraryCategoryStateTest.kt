/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.library

import com.dot.gallery.feature.library.ui.noCategories
import com.dot.gallery.feature.library.ui.categoriesLoaded
import com.dot.gallery.data.dao.CategoryWithMediaCount
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dot.gallery.feature.library.CategoryMedia


class LibraryCategoryStateTest {

    private fun item(id: Long) = CategoryMedia(
        category = CategoryWithMediaCount(
            id = id,
            name = "category-$id",
            searchTerms = "term",
            embedding = null,
            referenceImageIds = emptyList(),
            threshold = 0.2f,
            isUserCreated = false,
            isPinned = false,
            createdAt = 1L,
            updatedAt = 1L,
            mediaCount = 1,
            thumbnailMediaId = null
        ),
        thumbnailMedia = null
    )

    @Test
    fun `loading state is neither loaded nor empty`() {
        val items: List<CategoryMedia>? = null
        assertFalse(categoriesLoaded(items))
        assertFalse(noCategories(items))
    }

    @Test
    fun `loaded empty list reports no categories`() {
        val items = emptyList<CategoryMedia>()
        assertTrue(categoriesLoaded(items))
        assertTrue(noCategories(items))
    }

    @Test
    fun `loaded non-empty list renders categories`() {
        val items = listOf(item(1L), item(2L))
        assertTrue(categoriesLoaded(items))
        assertFalse(noCategories(items))
    }
}
