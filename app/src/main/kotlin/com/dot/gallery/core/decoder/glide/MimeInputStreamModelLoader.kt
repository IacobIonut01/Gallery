package com.dot.gallery.core.decoder.glide

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.bumptech.glide.Priority
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.data.DataFetcher.DataCallback
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import java.io.IOException
import java.io.InputStream

/**
 * Supplies a MimeInputStream (stream + MIME) for a given content Uri. This allows decoders
 * to make format decisions from MIME, avoiding fragile header sniffing.
 */
class MimeInputStreamModelLoader(
    private val context: Context,
    private val resolver: ContentResolver
) : ModelLoader<Uri, MimeInputStream> {

    override fun handles(model: Uri): Boolean = "content" == model.scheme

    override fun buildLoadData(
        model: Uri,
        width: Int,
        height: Int,
        options: Options
    ): LoadData<MimeInputStream>? {
        return LoadData(ObjectKey(model), MimeInputStreamFetcher(context, resolver, model))
    }

    private class MimeInputStreamFetcher(
        private val context: Context,
        private val resolver: ContentResolver,
        private val uri: Uri
    ) : DataFetcher<MimeInputStream> {
        private var stream: InputStream? = null

        override fun loadData(priority: Priority, callback: DataCallback<in MimeInputStream>) {
            try {
                val mime = resolver.getType(uri)
                // Diagnostic (debug builds only): log each distinct MIME seen on the content-image
                // path once, so undecodable formats reaching the grid (e.g. RAW files MediaStore
                // tags with a generic/unexpected MIME) can be identified.
                if (mime != null && loggedMimes.add(mime)) {
                    com.dot.gallery.feature_node.presentation.util.printDebug("MimeInputStream: content MIME '$mime' (e.g. $uri)")
                }
                stream = resolver.openInputStream(uri)
                val s = stream
                if (s == null) {
                    callback.onLoadFailed(IOException("Null InputStream for $uri"))
                } else {
                    callback.onDataReady(MimeInputStream(s, mime, uri, context))
                }
            } catch (e: Exception) {
                callback.onLoadFailed(e)
            }
        }

        override fun cleanup() { stream?.close() }
        override fun cancel() { /* no-op */ }
        override fun getDataClass(): Class<MimeInputStream> = MimeInputStream::class.java
        override fun getDataSource(): com.bumptech.glide.load.DataSource = com.bumptech.glide.load.DataSource.LOCAL

        private companion object {
            val loggedMimes = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())
        }
    }

    class Factory(private val context: Context): ModelLoaderFactory<Uri, MimeInputStream> {
        override fun build(multiFactory: com.bumptech.glide.load.model.MultiModelLoaderFactory): ModelLoader<Uri, MimeInputStream> =
            MimeInputStreamModelLoader(context.applicationContext, context.contentResolver)
        override fun teardown() {}
    }
}
