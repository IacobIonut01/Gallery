/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.use_case

import android.net.Uri
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow

class GetMediaListByUrisUseCase(
    private val repository: MediaRepository
) {
    operator fun invoke(
        listOfUris: List<Uri>
    ): Flow<Resource<List<Media>>> {
        return repository.getMediaListByUris(listOfUris)
    }

}

