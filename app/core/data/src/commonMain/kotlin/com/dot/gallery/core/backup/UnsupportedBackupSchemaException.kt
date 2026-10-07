/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

class UnsupportedBackupSchemaException(val schemaVersion: Int) : Exception(
    "Unsupported backup schema $schemaVersion; supported versions are " +
        "${BackupManifest.MIN_SUPPORTED_SCHEMA_VERSION}-${BackupManifest.SCHEMA_VERSION}"
)
