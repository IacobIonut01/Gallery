/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.painter.Painter
import com.dot.gallery.core.ui.settings.rememberPreference
import com.dot.gallery.domain.settings.SettingsKeys
import com.dot.gallery.feature.setup.domain.settings.SetupKeys


/**
 * First-launch / out-of-the-box setup wizard. Pages adapt to the build variant via
 * [SetupState.pages]: the Cloud page is omitted in offline builds (or when no remote
 * provider is bundled), and the AI Models page is omitted in offline builds unless
 * models are bundled (WithML). Completing the wizard records
 * [SetupKeys.CURRENT_SETUP_VERSION] so it is only shown again when the wizard itself
 * is reworked.
 *
 * [logoPainter] resolves an app-logo alias ([com.dot.gallery.domain.settings.ALIAS_REFRA]
 * et al.) to its launcher artwork — mipmap drawables on Android, so the seam stays
 * platform-side.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SetupContent(
    state: SetupState,
    actions: SetupActions,
    logoPainter: @Composable (logoAlias: String) -> Painter,
    modifier: Modifier = Modifier,
) {
    var setupCompletedVersion by rememberPreference(SetupKeys.setupCompletedVersion)
    val appLogoAlias by rememberPreference(SettingsKeys.appLogoAlias)

    var index by rememberSaveable { mutableIntStateOf(0) }
    val safeIndex = index.coerceIn(0, state.pages.lastIndex)
    val totalSteps = state.pages.size - 1

    val goNext: () -> Unit = { if (safeIndex < state.pages.lastIndex) index = safeIndex + 1 }
    val goBack: () -> Unit = {
        if (safeIndex > 0) index = safeIndex - 1 else actions.onExit()
        Unit
    }
    val finish: () -> Unit = finish@{
        // Permission may have been revoked from system settings while later setup pages were open.
        // Recheck before persisting completion; selected-photos access is a valid result.
        if (!actions.hasMediaAccess()) {
            index = state.pages.indexOf(SetupPage.PERMISSIONS)
            return@finish
        }
        setupCompletedVersion = SetupKeys.CURRENT_SETUP_VERSION
        actions.onFinish()
    }

    BackHandler(enabled = true) { goBack() }

    // A single SharedTransitionLayout wraps the pager so common chrome (back button, progress
    // indicator, action button) morphs between pages, with one continuous animated background
    // drawn behind it to avoid the per-page restart/flicker.
    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
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
                when (state.pages[idx]) {
                    SetupPage.WELCOME -> SetupWelcomeContent(
                        appIcon = logoPainter(appLogoAlias),
                        versionName = state.versionName,
                        onNext = goNext
                    )
                    SetupPage.PERMISSIONS -> SetupPermissionsContent(
                        state = state.permissions,
                        actions = actions.permissions,
                        stepNumber = stepNumber,
                        totalSteps = totalSteps,
                        onBack = goBack,
                        onNext = goNext
                    )
                    SetupPage.LOOKS -> SetupLooksFeelContent(
                        state = state.looksFeel,
                        actions = actions.looksFeel,
                        logoPainter = logoPainter,
                        stepNumber = stepNumber,
                        totalSteps = totalSteps,
                        onBack = goBack,
                        onNext = goNext
                    )
                    SetupPage.CLOUD -> SetupCloudContent(
                        state = state.cloud,
                        actions = actions.cloud,
                        stepNumber = stepNumber,
                        totalSteps = totalSteps,
                        onBack = goBack,
                        onNext = goNext,
                        onSkip = goNext
                    )
                    SetupPage.AI_MODELS -> SetupAiModelsContent(
                        state = state.aiModels,
                        actions = actions.aiModels,
                        stepNumber = stepNumber,
                        totalSteps = totalSteps,
                        onBack = goBack,
                        onNext = goNext,
                        onSkip = goNext
                    )
                    SetupPage.TIPS -> SetupTipsContent(
                        stepNumber = stepNumber,
                        totalSteps = totalSteps,
                        onBack = goBack,
                        onFinish = finish
                    )
                }
            }
        }
    }
}
