/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.ignored.ui.setup.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dot.gallery.feature.ignored.ui.IgnoredPreviewHost
import org.jetbrains.compose.ui.tooling.preview.Preview


/**
 * A component showing a regex pattern example with description.
 * Used to help users understand regex patterns.
 */
@Composable
fun RegexExample(
    description: String,
    pattern: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = pattern,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ========== Previews ==========

@Preview
@Composable
private fun RegexExamplePreview() {
    IgnoredPreviewHost {
        RegexExample(
            description = "Match anything with \"Screenshots\" in its path:",
            pattern = ".*Screenshots.*"
        )
    }
}

@Preview
@Composable
private fun RegexExampleContainsPreview() {
    IgnoredPreviewHost {
        RegexExample(
            description = "Match everything inside the WhatsApp folder:",
            pattern = ".*/WhatsApp/.*"
        )
    }
}

@Preview
@Composable
private fun RegexExampleComplexPreview() {
    IgnoredPreviewHost {
        RegexExample(
            description = "Match everything under DCIM on internal storage:",
            pattern = "/storage/emulated/0/DCIM/.*"
        )
    }
}
