/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.use_case

import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import kotlinx.coroutines.flow.Flow

class GetMediaByTypeUseCase(
    private val repository: MediaRepository
) {
    operator fun invoke(type: AllowedMedia): Flow<Resource<List<Media>>> = repository.getMediaByType(type)

}