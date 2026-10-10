/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs

/**
 * Platform-only scenes appended to [sceneRegistry]. Android registers
 * capture-only scenes that need `commonApp` components (editor, frame picker,
 * widgets); the wasm bundle contributes none — the live catalog must never
 * list a scene it cannot render.
 */
expect fun platformScenes(): List<DocsScene>
