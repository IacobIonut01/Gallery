/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.local

import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.PeopleCapableProvider
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local on-device people/face detection provider.
 * Uses ONNX Runtime for face detection and recognition.
 *
 * TODO: Implement in Phase 9+ with actual ONNX model integration.
 * This stub provides the interface contract for future implementation.
 */
@Singleton
class LocalPeopleProvider @Inject constructor() : LocalCapabilityProvider(), PeopleCapableProvider {

    override val providerType: ProviderType = ProviderType.LOCAL_PEOPLE
    override val displayName: String = ProviderType.LOCAL_PEOPLE.displayName
    override val capabilities: Set<ProviderCapability> = setOf(ProviderCapability.PEOPLE)

    private var initialized = false

    override suspend fun initialize() {
        // TODO: Load ONNX face detection + recognition models
        initialized = true
    }

    override fun release() {
        initialized = false
    }

    override val isAvailable: Boolean
        get() = initialized

    override fun getPeople(): Flow<Resource<List<PersonInfo>>> {
        // TODO: Query local face database
        return flowOf(Resource.Success(emptyList()))
    }

    override fun getPersonMedia(personId: String): Flow<Resource<List<Media>>> {
        // TODO: Query local face embeddings to find media
        return flowOf(Resource.Success(emptyList()))
    }

    override fun getPersonThumbnailUrl(personId: String): String? {
        // TODO: Return local face thumbnail path
        return null
    }

    override suspend fun updatePersonName(personId: String, name: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Local people provider does not support name editing"))

    override suspend fun updatePersonBirthDate(personId: String, birthDate: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Local people provider does not support birth date editing"))
}
