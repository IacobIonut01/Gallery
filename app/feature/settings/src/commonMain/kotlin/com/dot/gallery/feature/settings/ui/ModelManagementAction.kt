/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import com.dot.gallery.core.ml.ModelStatus

enum class ModelManagementAction(val enabled: Boolean) {
    DELETE(true),
    CANCEL_DOWNLOAD(true),
    DOWNLOAD(true),
    COPYING(false),
    INSTALLED_OFFLINE(false),
    UNAVAILABLE_OFFLINE(false),
}

/**
 * [canInstall] answers "could the user (re)install models at all" — network downloads with
 * INTERNET permission, or a local asset copy on bundled (withML) builds. Delete is only
 * offered when re-install is possible.
 */
fun resolveModelManagementAction(
    status: ModelStatus,
    canInstall: Boolean,
): ModelManagementAction = when (status) {
    ModelStatus.COPYING -> ModelManagementAction.COPYING
    ModelStatus.READY -> if (canInstall) {
        ModelManagementAction.DELETE
    } else {
        ModelManagementAction.INSTALLED_OFFLINE
    }
    ModelStatus.DOWNLOADING -> if (canInstall) {
        ModelManagementAction.CANCEL_DOWNLOAD
    } else {
        ModelManagementAction.UNAVAILABLE_OFFLINE
    }
    ModelStatus.ERROR, ModelStatus.NOT_INSTALLED -> if (canInstall) {
        ModelManagementAction.DOWNLOAD
    } else {
        ModelManagementAction.UNAVAILABLE_OFFLINE
    }
}
