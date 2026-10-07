/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.albums.ui

/**
 * Seam for the app-side startup work gate: when non-null, secondary flows
 * (e.g. the media-type card data) suspend until the first visible content of
 * the screen has been drawn, keeping cold-start work off the critical path.
 */
fun interface AlbumsStartupGate {
    suspend fun awaitFirstContent()
}
