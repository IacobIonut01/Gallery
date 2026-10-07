/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.cloudMediaId
import com.dot.gallery.cloud.core.stableIdHash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dot.gallery.core.presentation.util.add


/**
 * Pure-JVM tests for cloud media id derivation.
 *
 * These are the guarantees that keep the unified timeline's LazyGrid key
 * ("media_<id>_<label>") globally unique when local + multi-account cloud media are merged.
 * A collision here previously crashed the grid with
 * `IllegalArgumentException: Key ... was already used` during a fling.
 */
class CloudMediaIdTest {

    @Test
    fun stableIdHashIsPositiveAndDeterministic() {
        val a = stableIdHash("IMMICH/1/asset-x")
        val b = stableIdHash("IMMICH/1/asset-x")
        assertEquals(a, b)
        assertTrue("hash must be positive", a >= 0L)
    }

    @Test
    fun cloudMediaIdIsAlwaysNegative() {
        // Local MediaStore ids are always >= 0, so cloud ids MUST be strictly negative
        // to guarantee they can never collide with a local item's id.
        val id = cloudMediaId(ProviderType.IMMICH, 1L, "asset-x")
        assertTrue("cloud id must be strictly negative, was $id", id < 0L)
    }

    @Test
    fun cloudMediaIdIsDeterministic() {
        assertEquals(
            cloudMediaId(ProviderType.WEBDAV, 3L, "Photos/IMG.jpg"),
            cloudMediaId(ProviderType.WEBDAV, 3L, "Photos/IMG.jpg")
        )
    }

    @Test
    fun differentRemoteIdsProduceDifferentIds() {
        assertNotEquals(
            cloudMediaId(ProviderType.IMMICH, 1L, "asset-a"),
            cloudMediaId(ProviderType.IMMICH, 1L, "asset-b")
        )
    }

    @Test
    fun sameRemoteIdOnDifferentProvidersDoesNotCollide() {
        assertNotEquals(
            cloudMediaId(ProviderType.IMMICH, 1L, "shared-id"),
            cloudMediaId(ProviderType.OWNCLOUD, 1L, "shared-id")
        )
    }

    @Test
    fun sameRemoteIdOnDifferentAccountsOfSameTypeDoesNotCollide() {
        // Two Immich servers reusing the same remote id (e.g. both start assets at "1")
        // must still resolve to distinct timeline ids.
        assertNotEquals(
            cloudMediaId(ProviderType.IMMICH, 1L, "1"),
            cloudMediaId(ProviderType.IMMICH, 2L, "1")
        )
    }

    @Test
    fun idsAreUniqueAcrossAManyItemMultiAccountSet() {
        val ids = mutableSetOf<Long>()
        val providers = listOf(
            ProviderType.IMMICH, ProviderType.OWNCLOUD, ProviderType.NEXTCLOUD,
            ProviderType.WEBDAV, ProviderType.SMB, ProviderType.NFS
        )
        var total = 0
        for (provider in providers) {
            for (configId in 1L..5L) {
                for (n in 0 until 500) {
                    ids.add(cloudMediaId(provider, configId, "asset-$n"))
                    total++
                }
            }
        }
        assertEquals("expected no id collisions across the full set", total, ids.size)
    }
}
