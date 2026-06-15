/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.local

import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.cloud.core.ProviderType

/**
 * Base class for local on-device capability providers.
 * These providers run ML models locally (e.g. ONNX people detection, OCR)
 * and do not require network access or server configuration.
 *
 * Subclasses should implement specific capability interfaces like
 * PeopleCapableProvider or OcrCapableProvider.
 */
abstract class LocalCapabilityProvider : MediaCapabilityProvider {

    override val isAvailable: Boolean
        get() = true // Local providers are always available once initialized

    /**
     * Initialize the local provider (e.g., load ML models).
     * Called once during app startup.
     */
    abstract suspend fun initialize()

    /**
     * Release resources held by this provider.
     */
    abstract fun release()
}
