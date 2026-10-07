/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core


sealed class Position {
    data object Top : Position()
    data object Middle : Position()
    data object Bottom : Position()
    data object Alone : Position()
}
