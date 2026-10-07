/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.util

import android.net.Uri
import java.io.InputStream


/**
 * Host-provided bridge to the app-side [CloudMediaDownloader] (which lives behind
 * flavor source sets and the provider registry). Installed once at startup so
 * module share/viewer code can resolve `cloud://` URIs to a byte stream.
 */
object CloudMediaDownloadHook {
    var downloadCloudMedia: (Uri) -> InputStream? = { null }
    var downloadCloudMediaExact: (Uri) -> InputStream? = { null }
}
