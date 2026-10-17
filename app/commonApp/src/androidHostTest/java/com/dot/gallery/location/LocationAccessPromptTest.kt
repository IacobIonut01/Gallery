package com.dot.gallery.location

import com.dot.gallery.feature.viewer.components.shouldOpenPermissionSettings
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalPermissionsApi::class)
class LocationAccessPromptTest {

    @Test
    fun neverAskedPromptsRequestNotSettings() {
        // A fresh Denied state reports shouldShowRationale=false too, so the
        // first tap must still go through the runtime request.
        assertFalse(shouldOpenPermissionSettings(false, PermissionStatus.Denied(false)))
    }

    @Test
    fun deniedOnceWithRationaleReRequests() {
        assertFalse(shouldOpenPermissionSettings(true, PermissionStatus.Denied(true)))
    }

    @Test
    fun permanentlyDeniedOpensSettings() {
        assertTrue(shouldOpenPermissionSettings(true, PermissionStatus.Denied(false)))
    }

    @Test
    fun grantedNeverOpensSettings() {
        assertFalse(shouldOpenPermissionSettings(false, PermissionStatus.Granted))
        assertFalse(shouldOpenPermissionSettings(true, PermissionStatus.Granted))
    }
}
