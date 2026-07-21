package com.dot.gallery.core.decoder.glide

import android.content.Context
import android.net.Uri
import java.io.InputStream

/**
 * Wrapper pairing an InputStream with a resolved MIME type from ContentResolver. The optional
 * [uri] + [context] let decoders that support it (e.g. TIFF) open a seekable descriptor and
 * memory-map the file instead of reading the whole stream onto the heap.
 */
data class MimeInputStream(
    val inputStream: InputStream,
    val mimeType: String?,
    val uri: Uri? = null,
    val context: Context? = null,
)
