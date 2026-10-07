/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

/** Display state for the permissions page: current media access + the rows to render. */
data class SetupPermissionsState(
    val mediaAccess: SetupMediaAccess = SetupMediaAccess.NONE,
    val permissions: List<SetupPermissionUi> = emptyList(),
) {
    /** Media access above [SetupMediaAccess.NONE] unlocks the Continue button. */
    val coreGranted: Boolean get() = mediaAccess != SetupMediaAccess.NONE
}
