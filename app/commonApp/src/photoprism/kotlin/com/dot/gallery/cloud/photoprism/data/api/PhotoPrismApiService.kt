/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.photoprism.data.api

import com.dot.gallery.cloud.photoprism.data.dto.PhotoPrismAlbumDto
import com.dot.gallery.cloud.photoprism.data.dto.PhotoPrismConfigDto
import com.dot.gallery.cloud.photoprism.data.dto.PhotoPrismGeoCollectionDto
import com.dot.gallery.cloud.photoprism.data.dto.PhotoPrismOAuthTokenDto
import com.dot.gallery.cloud.photoprism.data.dto.PhotoPrismPhotoDto
import com.dot.gallery.cloud.photoprism.data.dto.PhotoPrismSessionDto
import com.dot.gallery.cloud.photoprism.data.dto.PhotoPrismSubjectDto
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PhotoPrismApiService {

    @POST("api/v1/session")
    suspend fun createSession(@Body body: RequestBody): Response<PhotoPrismSessionDto>

    @FormUrlEncoded
    @POST("api/v1/oauth/token")
    suspend fun oauthToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): Response<PhotoPrismOAuthTokenDto>

    @GET("api/v1/config")
    suspend fun getConfig(): Response<PhotoPrismConfigDto>

    @GET("api/v1/session")
    suspend fun getSession(): Response<PhotoPrismSessionDto>

    @GET("api/v1/photos")
    suspend fun getPhotos(
        @Query("count") count: Int,
        @Query("offset") offset: Int,
        @Query("merged") merged: Boolean = true,
        @Query("primary") primary: Boolean = true,
        @Query("order") order: String = "newest",
        @Query("q") query: String? = null,
        @Query("s") albumUid: String? = null,
        @Query("favorite") favorite: Boolean? = null,
        /** Subject/person UID filter (PhotoPrism `subject` form field). */
        @Query("subject") subjectUid: String? = null
    ): Response<List<PhotoPrismPhotoDto>>

    /** Places / map markers as GeoJSON FeatureCollection (`coordinates` = [lng, lat]). */
    @GET("api/v1/geo")
    suspend fun getGeo(
        @Query("count") count: Int = 100000,
        @Query("offset") offset: Int = 0
    ): Response<PhotoPrismGeoCollectionDto>

    @GET("api/v1/albums")
    suspend fun getAlbums(
        @Query("count") count: Int = 1000,
        @Query("offset") offset: Int = 0
    ): Response<List<PhotoPrismAlbumDto>>

    @POST("api/v1/albums")
    suspend fun createAlbum(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<PhotoPrismAlbumDto>

    @POST("api/v1/albums/{uid}/photos")
    suspend fun addPhotosToAlbum(
        @Path("uid") albumUid: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @GET("api/v1/subjects")
    suspend fun getSubjects(
        @Query("count") count: Int = 1000,
        @Query("offset") offset: Int = 0,
        @Query("type") type: String = "person",
        @Query("files") files: Int = 1,
        @Query("order") order: String = "name"
    ): Response<List<PhotoPrismSubjectDto>>

    @GET("api/v1/subjects/{uid}")
    suspend fun getSubject(@Path("uid") uid: String): Response<PhotoPrismSubjectDto>

    @PUT("api/v1/subjects/{uid}")
    suspend fun updateSubject(
        @Path("uid") uid: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<PhotoPrismSubjectDto>

    @POST("api/v1/photos/{uid}/like")
    suspend fun likePhoto(@Path("uid") uid: String): Response<Unit>

    @DELETE("api/v1/photos/{uid}/like")
    suspend fun unlikePhoto(@Path("uid") uid: String): Response<Unit>
}
