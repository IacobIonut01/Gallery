package com.dot.gallery.feature.editor

import com.dot.gallery.core.decoder.format.ImageReencoder
import com.dot.gallery.feature.editor.components.develop.RawSaveFormat
import kotlinx.coroutines.sync.Mutex

/** Rejects overlapping saves instead of queueing a second write behind the first one. */
class EditorSaveGuard {
    private val mutex = Mutex()

    fun tryAcquire(): Boolean = mutex.tryLock()

    fun release() {
        mutex.unlock()
    }

    suspend fun runIfIdle(block: suspend () -> Unit): Boolean {
        if (!tryAcquire()) return false
        return try {
            block()
            true
        } finally {
            release()
        }
    }
}
