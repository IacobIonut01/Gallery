/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

/**
 * Platform hooks for the whole setup wizard, wired by the app shell.
 * Page-scoped hooks live in the nested action types.
 */
data class SetupActions(
    /** Back was pressed on the first page — the app finishes its host activity. */
    val onExit: () -> Unit = {},
    /**
     * Synchronous media-access re-check that gates wizard completion: the permission may
     * have been revoked from system settings while later setup pages were open, in which
     * case the wizard jumps back to the permissions page instead of finishing.
     */
    val hasMediaAccess: () -> Boolean = { true },
    /**
     * Runs the app-side completion steps after the wizard persists
     * `SetupKeys.setupCompletedVersion`: swap the launcher alias when the chosen
     * name/logo differs from what is applied, restart the app to reflect it, then run
     * the navigation completion callback.
     */
    val onFinish: () -> Unit = {},
    val permissions: SetupPermissionsActions = SetupPermissionsActions(),
    val looksFeel: SetupLooksFeelActions = SetupLooksFeelActions(),
    val cloud: SetupCloudActions = SetupCloudActions(),
    val aiModels: SetupAiModelsActions = SetupAiModelsActions(),
)
