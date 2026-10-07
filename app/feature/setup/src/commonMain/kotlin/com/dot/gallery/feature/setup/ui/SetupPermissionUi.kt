/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

/**
 * One permission row's display state on the permissions page.
 *
 * [requestAttempted] marks optional permissions whose runtime request was already
 * tried once and denied — the grant affordance then becomes "Open app settings"
 * instead of re-firing the system dialog (used by [SetupPermission.MEDIA_LOCATION]).
 */
data class SetupPermissionUi(
    val permission: SetupPermission,
    val granted: Boolean = false,
    val requestAttempted: Boolean = false,
)
