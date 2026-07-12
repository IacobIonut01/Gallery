/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.local

import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.PeopleCapableProvider
import com.dot.gallery.cloud.data.dao.DetectedFaceDao
import com.dot.gallery.cloud.data.dao.PersonDao
import com.dot.gallery.core.Resource
import com.dot.gallery.core.ml.ModelGroup
import com.dot.gallery.core.ml.ModelManager
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local, on-device people provider backed by the [PersonDao]/[DetectedFaceDao] tables that the
 * [com.dot.gallery.core.workers.FaceIndexerWorker] populates. Available whenever the face
 * detector model is installed.
 */
@Singleton
class LocalPeopleProvider @Inject constructor(
    private val personDao: PersonDao,
    private val faceDao: DetectedFaceDao,
    private val mediaRepository: MediaRepository,
    private val modelManager: ModelManager
) : LocalCapabilityProvider(), PeopleCapableProvider {

    override val providerType: ProviderType = ProviderType.LOCAL_PEOPLE
    override val displayName: String = ProviderType.LOCAL_PEOPLE.displayName
    override val capabilities: Set<ProviderCapability> = setOf(ProviderCapability.PEOPLE)

    override suspend fun initialize() { /* No eager model load; sessions are created lazily. */ }

    override fun release() { }

    override val isAvailable: Boolean
        get() = modelManager.isReady(ModelGroup.FACE_DETECT)

    override fun getPeople(): Flow<Resource<List<PersonInfo>>> =
        personDao.getVisibleByProvider(ProviderType.LOCAL_PEOPLE).map { people ->
            Resource.Success(
                people.map { p ->
                    PersonInfo(
                        id = p.id,
                        name = p.name,
                        providerType = ProviderType.LOCAL_PEOPLE,
                        thumbnailUrl = p.thumbnailUrl,
                        assetCount = p.faceCount
                    )
                }
            )
        }

    override fun getPersonMedia(personId: String): Flow<Resource<List<Media>>> = flow {
        val ids = faceDao.getMediaIdsForPerson(personId).toHashSet()
        if (ids.isEmpty()) {
            emit(Resource.Success(emptyList()))
            return@flow
        }
        val media = mediaRepository.getCompleteMedia().first().data.orEmpty()
        emit(Resource.Success(media.filter { it.id in ids }))
    }

    override fun getPersonThumbnailUrl(personId: String): String? = null

    override suspend fun updatePersonName(personId: String, name: String): Result<Unit> =
        runCatching { personDao.updateName(personId, name) }

    override suspend fun updatePersonBirthDate(personId: String, birthDate: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Local people provider does not track birth dates"))

    /** Reassign every face of [sourceId] to [targetId] and delete the now-empty source person. */
    suspend fun mergePeople(sourceId: String, targetId: String) {
        faceDao.reassignPerson(sourceId, targetId)
        personDao.deleteById(sourceId)
        personDao.updateFaceCount(targetId, faceDao.countForPerson(targetId), System.currentTimeMillis())
    }

    suspend fun setHidden(personId: String, hidden: Boolean) = personDao.setHidden(personId, hidden)

    suspend fun setCover(personId: String, mediaId: Long, thumbnailUrl: String?) =
        personDao.updateThumbnail(personId, mediaId, thumbnailUrl)
}
