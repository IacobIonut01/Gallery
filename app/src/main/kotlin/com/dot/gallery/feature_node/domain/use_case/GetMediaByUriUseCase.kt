/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.use_case

import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow

class GetMediaByUriUseCase(
    private val repository: MediaRepository
) {
    operator fun invoke(
        uriAsString: String,
        isSecure: Boolean = false
    ): Flow<Resource<List<Media>>> {
        return repository.getMediaByUri(uriAsString, isSecure)
    }

}

