package com.dot.gallery.core.startup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable



@Composable
fun rememberStartupDestination(initial: String): String =
    rememberSaveable { initial }
