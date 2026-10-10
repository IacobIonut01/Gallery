/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui

import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.cloud_step_credentials
import com.dot.gallery.generated.resources.cloud_step_credentials_desc
import com.dot.gallery.generated.resources.cloud_step_networking
import com.dot.gallery.generated.resources.cloud_step_networking_desc
import com.dot.gallery.generated.resources.cloud_step_review
import com.dot.gallery.generated.resources.cloud_step_review_desc
import com.dot.gallery.generated.resources.cloud_step_server
import com.dot.gallery.generated.resources.cloud_step_server_desc
import com.dot.gallery.generated.resources.cloud_step_sync
import com.dot.gallery.generated.resources.cloud_step_sync_desc
import org.jetbrains.compose.resources.StringResource

/**
 * The add-server wizard pages, in display order. Public so the docs capture tier
 * can pose [CloudAddServerContent] directly on a given step.
 */
enum class CloudSetupStep(val titleRes: StringResource, val subtitleRes: StringResource? = null) {
    SERVER(Res.string.cloud_step_server, Res.string.cloud_step_server_desc),
    CREDENTIALS(Res.string.cloud_step_credentials, Res.string.cloud_step_credentials_desc),
    NETWORKING(Res.string.cloud_step_networking, Res.string.cloud_step_networking_desc),
    SYNC(Res.string.cloud_step_sync, Res.string.cloud_step_sync_desc),
    REVIEW(Res.string.cloud_step_review, Res.string.cloud_step_review_desc)
}
