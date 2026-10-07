/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.media_location
import com.dot.gallery.generated.resources.no_map_app
import com.dot.gallery.core.util.getString


fun Context.launchMap(lat: Double, lang: Double) {
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data = "geo:0,0?q=$lat,$lang(${getString(Res.string.media_location)})".toUri()
            }
        )
    } catch (_: ActivityNotFoundException) {
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    data = "https://www.google.com/maps/search/?api=1&query=$lat,$lang".toUri()
                }
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, getString(Res.string.no_map_app), Toast.LENGTH_SHORT).show()
        }
    }
}