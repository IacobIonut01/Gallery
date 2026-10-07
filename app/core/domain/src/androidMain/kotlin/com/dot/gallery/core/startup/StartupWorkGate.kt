/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.startup

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import com.dot.gallery.core.injection.Singleton


@Singleton
class StartupWorkGate @Inject constructor() {

    private val firstContent = CompletableDeferred<Unit>()

    fun onContentDrawn() {
        firstContent.complete(Unit)
    }

    val isReleased: Boolean
        get() = firstContent.isCompleted

    suspend fun awaitFirstContent() {
        withTimeoutOrNull(1_500L) { firstContent.await() }
    }
}

val LocalStartupWorkGate = staticCompositionLocalOf<StartupWorkGate?> { null }
