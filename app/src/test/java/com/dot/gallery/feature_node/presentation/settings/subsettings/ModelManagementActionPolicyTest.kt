package com.dot.gallery.feature_node.presentation.settings.subsettings

import com.dot.gallery.core.ml.ModelStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelManagementActionPolicyTest {

    @Test
    fun readyModelsCanOnlyBeDeletedWhenDownloadsAreAvailable() {
        val online = resolveModelManagementAction(ModelStatus.READY, hasInternetPermission = true)
        val offline = resolveModelManagementAction(ModelStatus.READY, hasInternetPermission = false)

        assertEquals(ModelManagementAction.DELETE, online)
        assertTrue(online.enabled)
        assertEquals(ModelManagementAction.INSTALLED_OFFLINE, offline)
        assertFalse(offline.enabled)
    }

    @Test
    fun bundledModelCopyIsNeverInteractive() {
        listOf(true, false).forEach { hasInternetPermission ->
            val action = resolveModelManagementAction(ModelStatus.COPYING, hasInternetPermission)

            assertEquals(ModelManagementAction.COPYING, action)
            assertFalse(action.enabled)
        }
    }

    @Test
    fun activeDownloadCanOnlyBeCancelledWhenDownloadsAreAvailable() {
        val online = resolveModelManagementAction(ModelStatus.DOWNLOADING, hasInternetPermission = true)
        val offline = resolveModelManagementAction(ModelStatus.DOWNLOADING, hasInternetPermission = false)

        assertEquals(ModelManagementAction.CANCEL_DOWNLOAD, online)
        assertTrue(online.enabled)
        assertEquals(ModelManagementAction.UNAVAILABLE_OFFLINE, offline)
        assertFalse(offline.enabled)
    }

    @Test
    fun missingModelsCanOnlyBeDownloadedWhenDownloadsAreAvailable() {
        listOf(ModelStatus.NOT_INSTALLED, ModelStatus.ERROR).forEach { status ->
            val online = resolveModelManagementAction(status, hasInternetPermission = true)
            val offline = resolveModelManagementAction(status, hasInternetPermission = false)

            assertEquals(ModelManagementAction.DOWNLOAD, online)
            assertTrue(online.enabled)
            assertEquals(ModelManagementAction.UNAVAILABLE_OFFLINE, offline)
            assertFalse(offline.enabled)
        }
    }
}
