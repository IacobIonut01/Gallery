/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs.scenes

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dot.gallery.data.fixture.DemoFixtures
import com.dot.gallery.domain.date.DateGrouper
import kotlin.time.Clock

/**
 * Proves the shared date-grouping pipeline end-to-end on wasmJs:
 * `DateGrouper` (kotlinx-datetime day math) + `formatEpochSec`
 * (`Intl.DateTimeFormat` actual) classify fixture timestamps into the same
 * header labels the Android timeline renders. Timestamps come from the
 * canonical [DemoFixtures] set shared by all scenes.
 */
@Composable
fun TimelineDateHeaderScene(state: String?) {
    val nowSec = Clock.System.now().epochSeconds
    val fixture = remember {
        DemoFixtures.media(nowSec).map { it.timestamp }
    }
    val grouper = remember {
        DateGrouper(
            format = "EEEE, MMMM d",
            weeklyFormat = "EEEE",
            extendedFormat = "EEEE, MMMM d, y",
            stringToday = "Today",
            stringYesterday = "Yesterday",
        )
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(fixture.map { it to grouper.classify(it) }) { (epoch, label) ->
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "epoch $epoch",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
