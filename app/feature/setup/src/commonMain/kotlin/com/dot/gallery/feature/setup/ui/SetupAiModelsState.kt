/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

/**
 * Display state for the AI models page.
 *
 * [isBundled] mirrors `BuildConfig.ML_MODELS_BUNDLED` (models ship inside the APK and
 * are copied rather than downloaded); [hasInternet] reports whether the app may reach
 * the network — without it the download affordances collapse to a hint.
 */
data class SetupAiModelsState(
    val isBundled: Boolean = false,
    val hasInternet: Boolean = true,
    val search: SetupModelUi = SetupModelUi(),
    val cutout: SetupModelUi = SetupModelUi(),
    val faceDetect: SetupModelUi = SetupModelUi(),
    val faceRecognition: SetupModelUi = SetupModelUi(),
)
