package com.dot.gallery.metadata

import com.dot.gallery.core.Settings
import com.dot.gallery.core.metadata.MetadataCategory
import com.dot.gallery.core.metadata.MetadataPolicy
import com.dot.gallery.core.metadata.MetadataRemovalMode
import com.dot.gallery.feature_node.data.repository.shouldUsePerFileMetadataIsolation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataPolicyTest {
    @Test
    fun bulkMetadataReusesSharedIsolationUnlessPerFileWasRequested() {
        assertFalse(
            shouldUsePerFileMetadataIsolation(
                Settings.Security.METADATA_ISOLATION_SHARED,
                bulk = true
            )
        )
        assertFalse(
            shouldUsePerFileMetadataIsolation(
                Settings.Security.METADATA_ISOLATION_HYBRID,
                bulk = true
            )
        )
        assertTrue(
            shouldUsePerFileMetadataIsolation(
                Settings.Security.METADATA_ISOLATION_PER_FILE,
                bulk = true
            )
        )
    }

    @Test
    fun hybridUsesPerFileIsolationForInteractiveMetadata() {
        assertFalse(
            shouldUsePerFileMetadataIsolation(
                Settings.Security.METADATA_ISOLATION_SHARED,
                bulk = false
            )
        )
        assertTrue(
            shouldUsePerFileMetadataIsolation(
                Settings.Security.METADATA_ISOLATION_HYBRID,
                bulk = false
            )
        )
        assertTrue(
            shouldUsePerFileMetadataIsolation(
                Settings.Security.METADATA_ISOLATION_PER_FILE,
                bulk = false
            )
        )
    }

    @Test
    fun locationOnlyRemovesOnlyLocation() {
        val policy = MetadataPolicy.forMode(MetadataRemovalMode.LOCATION)

        assertTrue(MetadataCategory.LOCATION in policy.removedCategories)
        assertFalse(MetadataCategory.TIMESTAMPS in policy.removedCategories)
        assertTrue(MetadataCategory.STRUCTURAL_FUNCTIONAL in policy.preservedCategories)
    }

    @Test
    fun privacyPreservesCaptureAndRenderingData() {
        val policy = MetadataPolicy.forMode(MetadataRemovalMode.PRIVACY)

        assertTrue(MetadataCategory.IDENTITY_DEVICE in policy.removedCategories)
        assertTrue(MetadataCategory.TIMESTAMPS in policy.removedCategories)
        assertTrue(MetadataCategory.PEOPLE in policy.removedCategories)
        assertFalse(MetadataCategory.CAPTURE_SETTINGS in policy.removedCategories)
        assertTrue(MetadataCategory.COLOR_HDR in policy.preservedCategories)
    }

    @Test
    fun everythingNeverSelectsStructuralOrColorMetadata() {
        val policy = MetadataPolicy.forMode(MetadataRemovalMode.EVERYTHING)

        assertFalse(MetadataCategory.STRUCTURAL_FUNCTIONAL in policy.removedCategories)
        assertFalse(MetadataCategory.COLOR_HDR in policy.removedCategories)
        assertTrue(MetadataCategory.CAPTURE_SETTINGS in policy.removedCategories)
        assertTrue(MetadataCategory.PROVENANCE in policy.removedCategories)
    }
}
