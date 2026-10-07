/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.injection

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dot.gallery.injection.ComponentOwner

/**
 * `hiltViewModel()` replacement backed by the kotlin-inject [AppComponent].
 *
 * The lambda runs against the component inside the owner's [androidx.lifecycle.ViewModelStore],
 * so the returned instance is scoped exactly like a `hiltViewModel` lookup —
 * pass the same [ViewModelStoreOwner] (e.g. a NavBackStackEntry) to share an
 * instance between call sites.
 *
 * The lambda receives a [SavedStateHandle] built from the store's own creation
 * extras (nav arguments included). View models that need one expose a
 * `me.tatarka.inject.annotations.Assisted` parameter and are provided through a
 * component factory; view models without a handle simply ignore the parameter.
 */
@Composable
inline fun <reified VM : ViewModel> injectViewModel(
    key: String? = null,
    viewModelStoreOwner: ViewModelStoreOwner =
        LocalViewModelStoreOwner.current ?: error("No ViewModelStoreOwner in scope"),
    crossinline provider: AppComponent.(SavedStateHandle) -> VM
): VM {
    val component = (LocalContext.current.applicationContext as ComponentOwner).component
    return viewModel(viewModelStoreOwner = viewModelStoreOwner, key = key) {
        component.provider(createSavedStateHandle())
    }
}
