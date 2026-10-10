/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.sharing

import com.dot.gallery.cloud.core.SharedLinkInfo

/** Every operation [SharedLinksContent] delegates to its host ViewModel. */
data class SharedLinksContentActions(
    val setFilter: (SharedLinksFilter) -> Unit = {},
    val copyLink: (SharedLinkInfo) -> Unit = {},
    val deleteLink: (SharedLinkInfo) -> Unit = {},
    val updateLink: (
        link: SharedLinkInfo,
        description: String?,
        password: String?,
        expiresAt: Long?,
        allowDownload: Boolean,
        allowUpload: Boolean,
        showMetadata: Boolean,
        changeExpiration: Boolean,
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
)
