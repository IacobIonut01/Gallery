/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.settings.subsettings

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.Settings
import com.dot.gallery.core.logging.LogEntry
import com.dot.gallery.core.logging.LogExporter
import com.dot.gallery.core.logging.LogLevel
import com.dot.gallery.core.navigate
import com.dot.gallery.core.navigateUp
import com.dot.gallery.feature.settings.ui.DeveloperScreenActions
import com.dot.gallery.feature.settings.ui.DeveloperScreenContent
import com.dot.gallery.feature.settings.ui.DeveloperScreenState
import com.dot.gallery.core.presentation.util.Screen
import kotlinx.coroutines.launch
import com.dot.gallery.injection.injectViewModel
import com.dot.gallery.feature.settings.subsettings.DeveloperViewModel
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.dev_copied_toast
import com.dot.gallery.generated.resources.dev_dev_mode_disabled
import com.dot.gallery.generated.resources.dev_entry_copied
import com.dot.gallery.generated.resources.dev_logs_cleared
import com.dot.gallery.generated.resources.dev_logs_copied
import com.dot.gallery.generated.resources.dev_test_error_done
import com.dot.gallery.generated.resources.share
import com.dot.gallery.core.util.getString

/**
 * Android shell for [DeveloperScreenContent]: collects the ViewModel state and
 * owns the platform side-effects (clipboard, Toast, share intent, navigation)
 * plus the developer-mode prefs.
 */
@Composable
fun DeveloperScreen(
    viewModel: DeveloperViewModel = injectViewModel { developerViewModel }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val eventHandler = LocalEventHandler.current

    var developerMode by Settings.Misc.rememberDeveloperMode()
    var breadcrumbsPref by Settings.Misc.rememberDevLogBreadcrumbs()
    var minLevelPref by Settings.Misc.rememberDevLogMinLevel()

    val copiedToast = stringResource(Res.string.dev_copied_toast)
    val logsCopiedToast = stringResource(Res.string.dev_logs_copied)
    val entryCopiedToast = stringResource(Res.string.dev_entry_copied)
    val clearedToast = stringResource(Res.string.dev_logs_cleared)
    val disabledToast = stringResource(Res.string.dev_dev_mode_disabled)
    val testDoneToast = stringResource(Res.string.dev_test_error_done)

    fun copyEntries(entries: List<LogEntry>, forGithub: Boolean, toast: String) {
        scope.launch {
            clipboard.setText(AnnotatedString(viewModel.exportClipboardText(entries, forGithub)))
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
        }
    }

    fun shareZip(entries: List<LogEntry>) {
        scope.launch {
            val file = viewModel.exportShareFile(entries)
            runCatching {
                context.startActivity(
                    Intent.createChooser(
                        LogExporter.shareIntent(context, file),
                        context.getString(Res.string.share)
                    )
                )
            }
        }
    }

    /** Copies the full (filtered) log text for the issue body, then opens GitHub. */
    fun reportBug(entry: LogEntry?) {
        scope.launch {
            clipboard.setText(
                AnnotatedString(viewModel.exportClipboardText(uiState.entries, forGithub = true))
            )
            Toast.makeText(context, copiedToast, Toast.LENGTH_LONG).show()
            uriHandler.openUri(viewModel.issueUrlFor(entry))
            if (entry?.logLevel == LogLevel.CRASH) viewModel.markCrashReported()
        }
    }

    val state = remember(uiState) {
        DeveloperScreenState(
            stats = uiState.stats,
            totalCount = uiState.totalCount,
            latestErrors = uiState.latestErrors,
            entries = uiState.entries,
            unreportedCrashEntry = uiState.unreportedCrashEntry,
            minLevel = uiState.minLevel,
            breadcrumbsEnabled = uiState.breadcrumbsEnabled,
        )
    }

    val actions = DeveloperScreenActions(
        onOpenSessionLogs = { eventHandler.navigate(Screen.SessionLogsScreen()) },
        onReportBug = ::reportBug,
        onShareZip = ::shareZip,
        onCopyEntries = { copyEntries(it, forGithub = false, toast = logsCopiedToast) },
        onCopyEntry = { entry ->
            clipboard.setText(AnnotatedString(LogExporter.entryText(entry)))
            Toast.makeText(context, entryCopiedToast, Toast.LENGTH_SHORT).show()
        },
        onClearLogs = {
            viewModel.clearLogs()
            Toast.makeText(context, clearedToast, Toast.LENGTH_SHORT).show()
        },
        onDisableDeveloperMode = {
            viewModel.disableDeveloperMode()
            developerMode = false
            Toast.makeText(context, disabledToast, Toast.LENGTH_SHORT).show()
            eventHandler.navigateUp()
        },
        onToggleBreadcrumbs = { value ->
            breadcrumbsPref = value
            viewModel.setBreadcrumbsEnabled(value)
        },
        onSetMinLevel = { level ->
            viewModel.setMinLevel(level)
            minLevelPref = level.name
        },
        onTestError = {
            viewModel.logTestError()
            Toast.makeText(context, testDoneToast, Toast.LENGTH_SHORT).show()
        },
        onMarkCrashReported = viewModel::markCrashReported,
    )

    DeveloperScreenContent(state = state, actions = actions)
}
