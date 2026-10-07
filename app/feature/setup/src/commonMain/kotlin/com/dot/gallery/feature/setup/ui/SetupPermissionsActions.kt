/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

/** Platform hooks the permissions page needs from the app shell. */
data class SetupPermissionsActions(
    /** Launches the runtime media-permission prompt (images/videos/selected). */
    val onRequestCorePermissions: () -> Unit = {},
    /**
     * Re-reads media access synchronously when Continue is tapped — the grant may have
     * changed while the page was open. Returns true when access allows advancing.
     */
    val recheckMediaAccess: () -> Boolean = { true },
    /**
     * Grant action for an optional row. The app decides per permission: fire the runtime
     * prompt again, deep-link into app settings, or launch a system manage-intent.
     */
    val onGrant: (SetupPermission) -> Unit = {},
)
