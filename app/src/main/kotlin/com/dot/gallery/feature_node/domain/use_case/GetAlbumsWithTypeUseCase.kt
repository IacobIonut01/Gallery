/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.use_case

import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import kotlinx.coroutines.flow.Flow

class GetAlbumsWithTypeUseCase(
    private val repository: MediaRepository
) {

    operator fun invoke(
        allowedMedia: AllowedMedia
    ): Flow<Resource<List<Album>>> = repository.getAlbumsWithType(allowedMedia)
}