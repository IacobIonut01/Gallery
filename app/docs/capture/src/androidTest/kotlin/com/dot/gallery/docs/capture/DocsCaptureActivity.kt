/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs.capture

import androidx.activity.ComponentActivity

/**
 * Opaque, action-bar-free host for docs-capture frames. The default
 * androidx.test `EmptyActivity` runs in a translucent invisible task, so its
 * window only ever presents a half-drawn first frame — nested Compose layers
 * never flush and captures come back blank. This activity is declared with a
 * regular opaque theme, which lets the window present normally.
 */
class DocsCaptureActivity : ComponentActivity()
