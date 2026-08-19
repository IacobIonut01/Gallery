/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.setup

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.dot.gallery.BuildConfig
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.core.Settings
import com.dot.gallery.core.Settings.Misc.rememberAppLogoAlias
import com.dot.gallery.core.Settings.Misc.rememberAppNameAlias
import com.dot.gallery.core.Settings.Misc.rememberSetupCompletedVersion
import com.dot.gallery.core.presentation.components.util.hasMediaAccess
import com.dot.gallery.feature_node.presentation.setup.components.LocalSetupAnimatedVisibilityScope
import com.dot.gallery.feature_node.presentation.setup.components.LocalSetupSharedTransitionScope
import com.dot.gallery.feature_node.presentation.setup.components.SetupAnimatedBackground
import com.dot.gallery.feature_node.presentation.setup.pages.SetupAiModelsPage
import com.dot.gallery.feature_node.presentation.setup.pages.SetupCloudPage
import com.dot.gallery.feature_node.presentation.setup.pages.SetupLooksFeelPage
import com.dot.gallery.feature_node.presentation.setup.pages.SetupPermissionsPage
import com.dot.gallery.feature_node.presentation.setup.pages.SetupTipsPage
import com.dot.gallery.feature_node.presentation.setup.pages.SetupWelcomePage
import com.dot.gallery.feature_node.presentation.util.changeAppAlias
import com.dot.gallery.feature_node.presentation.util.currentLauncherAlias
import com.dot.gallery.feature_node.presentation.util.launcherAliasFor
import com.dot.gallery.feature_node.presentation.util.restartApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SetupPage { WELCOME, PERMISSIONS, LOOKS, CLOUD, AI_MODELS, TIPS }

/**
 * First-launch / out-of-the-box setup wizard. Pages adapt to the build variant:
 * the Cloud page is omitted in offline builds (or when no remote provider is bundled),
 * and the AI Models page is omitted in offline builds unless models are bundled (WithML).
 * Completing the wizard records [Settings.Misc.CURRENT_SETUP_VERSION] so it is only shown
 * again when the wizard itself is reworked.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SetupScreen(onComplete: () -> Unit = {}) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var setupCompletedVersion by rememberSetupCompletedVersion()
    val appNameAlias by rememberAppNameAlias()
    val appLogoAlias by rememberAppLogoAlias()

    val pages = remember {
        buildList {
            add(SetupPage.WELCOME)
            add(SetupPage.PERMISSIONS)
            add(SetupPage.LOOKS)
            if (!BuildConfig.OFFLINE_MODE && ProviderType.hasAnyRemoteProvider()) add(SetupPage.CLOUD)
            if (!BuildConfig.OFFLINE_MODE || BuildConfig.ML_MODELS_BUNDLED) add(SetupPage.AI_MODELS)
            add(SetupPage.TIPS)
        }
    }

    var index by rememberSaveable { mutableIntStateOf(0) }
    val safeIndex = index.coerceIn(0, pages.lastIndex)
    val totalSteps = pages.size - 1

    val goNext: () -> Unit = { if (safeIndex < pages.lastIndex) index = safeIndex + 1 }
    val goBack: () -> Unit = {
        if (safeIndex > 0) index = safeIndex - 1 else activity?.finish()
        Unit
    }
    val finish: () -> Unit = finish@{
        // Permission may have been revoked from system settings while later setup pages were open.
        // Recheck before persisting completion; selected-photos access is a valid result.
        if (!context.hasMediaAccess()) {
            index = pages.indexOf(SetupPage.PERMISSIONS)
            return@finish
        }
        setupCompletedVersion = Settings.Misc.CURRENT_SETUP_VERSION
        // Apply the remembered app name + logo now that setup is complete. Restart the app
        // (so the launcher reflects the change immediately) only when it actually differs
        // from what is currently applied; otherwise continue normally.
        if (context.currentLauncherAlias() != launcherAliasFor(appNameAlias, appLogoAlias)) {
            context.changeAppAlias(appNameAlias, appLogoAlias)
            // Give DataStore a moment to persist the completed-setup flag before the
            // process is killed and relaunched, otherwise setup could show again.
            scope.launch {
                delay(300)
                context.restartApplication()
            }
        } else {
            onComplete()
        }
    }

    BackHandler(enabled = true) { goBack() }

    // A single SharedTransitionLayout wraps the pager so common chrome (back button, progress
    // indicator, action button) morphs between pages, with one continuous animated background
    // drawn behind it to avoid the per-page restart/flicker.
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        SetupAnimatedBackground()

        AnimatedContent(
            targetState = safeIndex,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val forward = targetState >= initialState
                val dir = if (forward) 1 else -1
                (slideInHorizontally(tween(350)) { full -> dir * full } + fadeIn(tween(350)))
                    .togetherWith(
                        slideOutHorizontally(tween(350)) { full -> -dir * full } + fadeOut(tween(350))
                    )
                    .using(SizeTransform(clip = false))
            },
            label = "setup-pages"
        ) { idx ->
            CompositionLocalProvider(
                LocalSetupSharedTransitionScope provides this@SharedTransitionLayout,
                LocalSetupAnimatedVisibilityScope provides this@AnimatedContent
            ) {
                val stepNumber = idx
                when (pages[idx]) {
                    SetupPage.WELCOME -> SetupWelcomePage(onNext = goNext)
                    SetupPage.PERMISSIONS -> SetupPermissionsPage(stepNumber, totalSteps, goBack, goNext)
                    SetupPage.LOOKS -> SetupLooksFeelPage(stepNumber, totalSteps, goBack, goNext)
                    SetupPage.CLOUD -> SetupCloudPage(stepNumber, totalSteps, goBack, goNext, onSkip = goNext)
                    SetupPage.AI_MODELS -> SetupAiModelsPage(stepNumber, totalSteps, goBack, goNext, onSkip = goNext)
                    SetupPage.TIPS -> SetupTipsPage(stepNumber, totalSteps, goBack, onFinish = finish)
                }
            }
        }
    }
}
