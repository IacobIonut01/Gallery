/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.settings

/** Which of the account settings dialogs opens on first composition. */
enum class CloudProviderSettingsDialog {
    NONE,
    SYNC_INTERVAL,
    DOWNLOAD,
    REMOVE_DOWNLOADS,
    DELETE_ACCOUNT,
}
