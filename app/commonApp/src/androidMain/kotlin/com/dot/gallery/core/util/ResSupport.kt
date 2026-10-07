/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.util

import android.content.Context
import android.content.res.Resources
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString as cmpGetPluralString
import org.jetbrains.compose.resources.getString as cmpGetString

/**
 * Blocking bridges from `Context`/`Resources` to Compose Multiplatform resources
 * for non-composable call sites (workers, managers, launchers). CMP's own
 * blockingMain layer uses the same `runBlocking` bridge on Android, so the
 * blocking behavior matches what the platform does internally for `stringResource`.
 *
 * Composable code should prefer `stringResource`/`pluralStringResource` — these
 * exist so the `R.string` → `Res.string` migration does not force suspend
 * signatures through non-suspending call graphs.
 */
fun Context.getString(res: StringResource, vararg formatArgs: Any): String =
    runBlocking { cmpGetString(res, *formatArgs) }

fun Resources.getString(res: StringResource, vararg formatArgs: Any): String =
    runBlocking { cmpGetString(res, *formatArgs) }

fun Context.getText(res: StringResource): CharSequence = getString(res)

fun Context.getQuantityString(
    res: PluralStringResource,
    quantity: Int,
    vararg formatArgs: Any,
): String = runBlocking { cmpGetPluralString(res, quantity, *formatArgs) }

fun Resources.getQuantityString(
    res: PluralStringResource,
    quantity: Int,
    vararg formatArgs: Any,
): String = runBlocking { cmpGetPluralString(res, quantity, *formatArgs) }
