/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.settings.subsettings

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.logging.LogExporter
import com.dot.gallery.feature.settings.ui.SessionLogsActions
import com.dot.gallery.feature.settings.ui.SessionLogsContent
import com.dot.gallery.feature.settings.ui.SessionLogsRange
import com.dot.gallery.feature.settings.ui.SessionLogsState
import kotlinx.coroutines.launch
import com.dot.gallery.injection.injectViewModel
import com.dot.gallery.feature.settings.subsettings.DeveloperViewModel
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.dev_copied_toast
import com.dot.gallery.generated.resources.dev_entry_copied

/**
 * Android shell for [SessionLogsContent]: collects the ViewModel state and owns
 * the platform side-effects (clipboard, Toast, issue-URL hand-off).
 */
@Composable
fun SessionLogsScreen(
    viewModel: DeveloperViewModel = injectViewModel { developerViewModel }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    val entryCopiedToast = stringResource(Res.string.dev_entry_copied)
    val copiedToast = stringResource(Res.string.dev_copied_toast)

    val state = remember(uiState) {
        SessionLogsState(
            loading = uiState.loading,
            mode = uiState.mode,
            filters = uiState.filters,
            entries = uiState.entries,
            groups = uiState.groups,
            expandedGroups = uiState.expandedGroups,
        )
    }

    val actions = SessionLogsActions(
        onModeChange = viewModel::setMode,
        onQueryChange = viewModel::setQuery,
        onToggleLevel = viewModel::toggleLevel,
        onRange = { viewModel.setRange(it) },
        onCustomRange = { start, end ->
            viewModel.setRange(SessionLogsRange.CUSTOM, customStart = start, customEnd = end)
        },
        onToggleGroup = viewModel::toggleGroup,
        onCopyEntry = { entry ->
            clipboard.setText(AnnotatedString(LogExporter.entryText(entry)))
            Toast.makeText(context, entryCopiedToast, Toast.LENGTH_SHORT).show()
        },
        onReportEntry = { entry ->
            scope.launch {
                clipboard.setText(
                    AnnotatedString(viewModel.exportClipboardText(state.entries, forGithub = true))
                )
                Toast.makeText(context, copiedToast, Toast.LENGTH_LONG).show()
                uriHandler.openUri(viewModel.issueUrlFor(entry))
            }
        },
    )

    SessionLogsContent(state = state, actions = actions)
}
