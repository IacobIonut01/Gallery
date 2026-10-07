package com.dot.gallery.core.decryption

import android.content.Context
import androidx.collection.LruCache
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import com.dot.gallery.core.injection.Singleton
import com.dot.gallery.core.metrics.MetricsCollector
import com.dot.gallery.data.data_source.KeychainHolder
import com.dot.gallery.domain.uri.path

@Singleton
class DecryptManager @Inject constructor(
    private val context: Context,
    private val metrics: MetricsCollector
) {
    // Small LRU for decrypted byte arrays (only small items inserted)
    private val lru = object : LruCache<String, DecryptResult>(32) {
        override fun sizeOf(key: String, value: DecryptResult): Int = value.bytes.size
    }
    private val inFlight = ConcurrentHashMap<String, MutableList<(DecryptResult?, Throwable?) -> Unit>>()
    private val keychainHolder by lazy { KeychainHolder(context) }

    fun decrypt(file: File): DecryptResult {
        val key = hash(file)
        lru[key]?.let {
            metrics.incLruHit()
            return it
        }
        metrics.incLruMiss()
        // Single-flight: if already decrypting, wait via callback list
        val callbacks = inFlight.computeIfAbsent(key) { mutableListOf() }
        if (callbacks.isNotEmpty()) {
            var result: DecryptResult? = null
            var failure: Throwable? = null
            val latch = java.util.concurrent.CountDownLatch(1)
            synchronized(callbacks) {
                callbacks += { r, e ->
                    result = r
                    failure = e
                    latch.countDown()
                }
            }
            metrics.incDecryptWaiters(1)
            latch.await()
            failure?.let { throw it }
            return result!!
        }
        // We are first owner
        var result: DecryptResult? = null
        var failure: Throwable? = null
        try {
            metrics.incDecryptInvocation()
            val decrypted = keychainHolder.decryptVaultMedia(file)
            try {
                result = DecryptResult(decrypted.readBytes(), decrypted.mimeType)
            } finally {
                decrypted.cleanup()
            }
            // Only cache small results (< 2MB) to keep memory bounded
            if (result.bytes.size <= 2 * 1024 * 1024) {
                lru.put(key, result)
            }
            return result
        } catch (t: Throwable) {
            failure = t
            throw t
        } finally {
            val list = inFlight.remove(key)
            if (list != null) {
                val r = result
                val f = failure
                list.forEach { cb -> cb(r, f) }
            }
        }
    }

    private fun hash(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(file.path.encodeToByteArray())
        val digest = md.digest()
        return digest.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    }
}