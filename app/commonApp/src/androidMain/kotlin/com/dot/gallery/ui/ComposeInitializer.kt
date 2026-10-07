package com.dot.gallery.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ProcessLifecycleInitializer
import androidx.navigation.compose.rememberNavController
import androidx.startup.Initializer
import com.dot.gallery.core.work.workScheduler
import dev.brewkits.kmpworkmanager.KmpWorkManager
import dev.brewkits.kmpworkmanager.generated.AndroidWorkerFactoryGenerated
import com.dot.gallery.core.presentation.components.AppBarContainer
import com.dot.gallery.core.presentation.components.NavigationComp
import com.dot.gallery.core.presentation.util.Screen
import com.dot.gallery.ui.theme.GalleryTheme
import com.dot.gallery.ui.theme.AppThemeLocals
import com.dot.gallery.app.presentation.timeline.TimelineScreen



@SuppressLint("EnsureInitializerMetadata")
class ComposeInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        // The androidx WorkManagerInitializer is manifest-disabled; kmpworkmanager owns the
        // inner WorkManager. App-Startup initializers run before Application.onCreate, so
        // init here (idempotent) for the screens this preview composes.
        if (!KmpWorkManager.isInitialized()) {
            KmpWorkManager.initialize(context.applicationContext, AndroidWorkerFactoryGenerated())
        }
        ComposeView(context).setContent {
            AppThemeLocals {
                GalleryTheme {
                val navController = rememberNavController()
                val isScrolling = remember { mutableStateOf(false) }
                val bottomBarState = rememberSaveable { mutableStateOf(true) }
                val systemBarFollowThemeState = rememberSaveable { mutableStateOf(true) }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    content = { paddingValues ->
                        AppBarContainer(
                            navController = navController,
                            paddingValues = paddingValues,
                            bottomBarState = bottomBarState.value,
                            isScrolling = isScrolling.value
                        ) { sharedTransitionScope ->
                            NavigationComp(
                                navController = navController,
                                sharedTransitionScope = sharedTransitionScope,
                                paddingValues = paddingValues,
                                bottomBarState = bottomBarState,
                                systemBarFollowThemeState = systemBarFollowThemeState,
                                toggleRotate = { },
                                isScrolling = isScrolling,
                                initialStartDestination = Screen.TimelineScreen()
                            )
                        }
                    }
                )
            }
            }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(ProcessLifecycleInitializer::class.java)
    }
}