package com.dot.gallery.domain.util

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatcher for blocking IO work. `Dispatchers.IO` exists only on JVM/Android;
 * other targets fall back to `Dispatchers.Default`.
 */
expect val IODispatcher: CoroutineDispatcher
