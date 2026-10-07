/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.dot.gallery.BuildConfig

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "ReFra") {
        MaterialTheme {
            Text("ReFra desktop — stub (BuildConfig.OFFLINE_MODE=${BuildConfig.OFFLINE_MODE})")
        }
    }
}
