/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs.capture

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Holds instrumentation attached for clip recording, without a compose rule.
 * Compose-resources resolve through `InstrumentationRegistry`, which is only
 * registered while an instrumentation session is live, so a directly-launched
 * DocsCaptureActivity crashes without a hold. The hold lives in a separate
 * class on purpose: the compose rule in [DocsCaptureTest] installs the
 * pausable test frame clock for the whole process, and a scene launched via
 * `am start` then composes on a clock nobody pumps — gestures leave the
 * pager mid-transition and player state never reaches the UI.
 *
 * Run in the background (`am instrument` without -w), `am start` the scene,
 * record, then stop the session. `-e holdSecs N` bounds the hold
 * (explicit opt-in, max 600). Ordinary test runs skip this helper.
 */
@RunWith(AndroidJUnit4::class)
class DocsRecordingHoldTest {

    @Test
    fun hold() {
        val requested = InstrumentationRegistry.getArguments().getString("holdSecs")
        assumeTrue("Recording hold requires an explicit holdSecs argument", requested != null)
        val secs = requireNotNull(requested?.toLongOrNull()) { "holdSecs must be an integer" }
        require(secs in 1L..600L) { "holdSecs must be between 1 and 600" }
        Log.i(TAG, "DocsRecordingHoldTest: holding instrumentation for ${secs}s")
        Thread.sleep(secs * 1000L)
    }

    private companion object {
        const val TAG = "DocsRecordingHold"
    }
}
