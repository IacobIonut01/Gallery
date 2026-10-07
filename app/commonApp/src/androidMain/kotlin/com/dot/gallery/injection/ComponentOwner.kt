/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.injection

/**
 * Implemented by the platform `Application` subclass (androidApp's
 * `GalleryApp`) so library code can reach the kotlin-inject graph without a
 * dependency on the entry module.
 */
interface ComponentOwner {
    val component: AppComponent
}
