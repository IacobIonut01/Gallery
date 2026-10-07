/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.settings

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.Settings
import com.dot.gallery.core.navigate
import com.dot.gallery.feature.settings.ui.SettingsRoute
import com.dot.gallery.feature.settings.ui.SettingsScreenActions
import com.dot.gallery.feature.settings.ui.SettingsScreenContent
import com.dot.gallery.feature.settings.ui.SettingsScreenState
import com.dot.gallery.feature.help.ui.SupportSheet
import com.dot.gallery.core.presentation.util.Screen
import kotlinx.coroutines.launch
import com.dot.gallery.cloud.core.hasAnyRemoteProvider
import com.dot.gallery.core.ui.util.rememberAppBottomSheetState
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.dev_unlocked


/**
 * Android shell for the settings hub — owns preferences, navigation, the
 * support sheet and the developer-unlock toast; the screen body lives in
 * `:feature:settings` ([SettingsScreenContent]).
 */
@Composable
fun SettingsScreen() {
    var bannerDismissed by Settings.Misc.rememberHeaderBannerDismissed()
    var developerMode by Settings.Misc.rememberDeveloperMode()
    val context = LocalContext.current
    val unlockedMessage = stringResource(Res.string.dev_unlocked)

    val supportState = rememberAppBottomSheetState()
    val scope = rememberCoroutineScope()
    SupportSheet(state = supportState)

    val eventHandler = LocalEventHandler.current
    val hasCloudProviders = remember { ProviderType.hasAnyRemoteProvider() }

    SettingsScreenContent(
        state = SettingsScreenState(
            developerMode = developerMode,
            bannerDismissed = bannerDismissed,
            hasCloudProviders = hasCloudProviders,
        ),
        actions = SettingsScreenActions(
            onNavigate = { route ->
                eventHandler.navigate(
                    when (route) {
                        SettingsRoute.APPEARANCE -> Screen.ColorPaletteScreen()
                        SettingsRoute.TIMELINE_ALBUMS -> Screen.SettingsTimelineAlbumsScreen()
                        SettingsRoute.MEDIA_VIEWER -> Screen.SettingsMediaViewerScreen()
                        SettingsRoute.NAVIGATION -> Screen.SettingsNavigationScreen()
                        SettingsRoute.GENERAL -> Screen.SettingsGeneralScreen()
                        SettingsRoute.SECURITY -> Screen.SettingsSecurityScreen()
                        SettingsRoute.BACKUP -> Screen.SettingsBackupScreen()
                        SettingsRoute.CLOUD_ACCOUNTS -> Screen.CloudAccountsScreen()
                        SettingsRoute.SMART_FEATURES -> Screen.SettingsSmartFeaturesScreen()
                        SettingsRoute.HELP -> Screen.HelpScreen()
                        SettingsRoute.DEVELOPER -> Screen.DeveloperScreen()
                    }
                )
            },
            onDismissBanner = { bannerDismissed = true },
            onRestoreBanner = { bannerDismissed = false },
            onShowSupport = { scope.launch { supportState.show() } },
            onDeveloperUnlocked = {
                developerMode = true
                Toast.makeText(context, unlockedMessage, Toast.LENGTH_LONG).show()
            },
        ),
    )
}
