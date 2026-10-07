/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.core.Settings
import com.dot.gallery.core.decoder.format.ImageReencoder
import com.dot.gallery.core.ui.mediaview.VisualSearchTarget
import com.dot.gallery.data.entity.MediaMetadata
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.Vault
import kotlinx.coroutines.flow.Flow

/** Lifecycle events of a rotate-image work item, mapped off WorkManager by the app impl. */
sealed interface RotateUpdate {
    data class Stage(val stage: MediaViewViewModel.RotationStage) : RotateUpdate
    data class Succeeded(val resultUri: String?) : RotateUpdate
    data class Failed(val message: String?) : RotateUpdate
}