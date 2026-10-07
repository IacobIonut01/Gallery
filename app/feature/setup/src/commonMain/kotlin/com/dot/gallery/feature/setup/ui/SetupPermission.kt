/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

/**
 * A permission/device-capability row shown on the permissions page of the setup wizard.
 * The app shell decides which rows to show (SDK + build-variant gating) and owns the
 * runtime grant flow; the enum only identifies the row so content can resolve its
 * icon, title, reason and labels.
 */
enum class SetupPermission {
    IMAGES,
    VIDEO,
    MEDIA_LOCATION,
    INTERNET,
    LOCAL_NETWORK,
    NOTIFICATIONS,
    MANAGE_MEDIA,
    MANAGE_FILES,
}
