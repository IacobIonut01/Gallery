package com.dot.gallery

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dot.gallery.core.work.taggedTasksFlow
import dev.brewkits.kmpworkmanager.KmpWorkManager
import dev.brewkits.kmpworkmanager.background.domain.TaskState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The `MetadataLocationRepair` unique work is enqueued by app startup
 * (GalleryApp/MainActivity) — it only exists while the real app process is
 * instrumented, so this lives in the app tier (androidApp androidTest), not the
 * self-instrumented :app:commonApp deviceTest APK.
 */
@RunWith(AndroidJUnit4::class)
class MetadataLocationRepairEnqueueTest {

    @Test
    fun startupEnqueuesLocationRepair() = runBlocking {
        // KmpWorkManager is initialized by GalleryApp when the real app process
        // is instrumented — getInstance() reads that registry directly.
        val scheduler = KmpWorkManager.getInstance().backgroundTaskScheduler
        val tasks = withTimeout(10_000) {
            scheduler.taggedTasksFlow("MetadataLocationRepair").first { it.isNotEmpty() }
        }
        assertTrue(tasks.none { it.state is TaskState.Cancelled })
    }
}
