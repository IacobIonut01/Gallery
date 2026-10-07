/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.scrollbar

/** Where dragging is allowed to begin from. */
enum class ScrollbarSelectionMode {
    /** Dragging can start anywhere on the scrollbar track or the thumb. */
    Full,

    /** Dragging can only start on the thumb itself. */
    Thumb,

    /** Dragging is disabled entirely. */
    Disabled
}
