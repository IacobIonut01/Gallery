/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.picker.ui

import androidx.compose.runtime.Immutable
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.Vault


/**
 * Callbacks [PickerContent] reports to the app shell. The picker UI keeps all
 * navigation/selection state internally; the app only owns platform seams —
 * credential prompts (biometric + vault password), the private-folder gate and
 * the pick-result hand-off.
 *
 * Every auth callback takes a continuation it invokes once the credential
 * check succeeds, so the module keeps owning its internal navigation state.
 */
@Immutable
data class PickerActions(
    /** Close icon in the tab bar — aborts the pick (finishes the Activity). */
    val onClose: () -> Unit,
    /**
     * A locked [album] was tapped. The app runs its unlock prompt; call [open]
     * to enter the album on success or [noDeviceSecurity] when the device has
     * no credential enrolled so the content can show the info sheet.
     */
    val onLockedAlbum: (album: Album, open: () -> Unit, noDeviceSecurity: () -> Unit) -> Unit,
    /** Private-folder card tapped — the app runs its gate; call [open] to reveal the section. */
    val onPrivateFolder: (open: () -> Unit) -> Unit,
    /** Vault card tapped — the app runs the vault-list gate; call [reveal] once cleared. */
    val onVaultGate: (reveal: () -> Unit) -> Unit,
    /**
     * A [vault] was tapped in the chooser (or reached via the single-vault fast
     * path). The app authenticates it; call [open] to enter the vault.
     */
    val onVault: (vault: Vault, open: () -> Unit) -> Unit,
    /**
     * FAB — deliver the current selection. The app materializes shareable URIs
     * (decrypting vault items / downloading cloud items) and finishes the
     * pick; [onProgress] drives the preparing dialog. Returns `false` when
     * nothing could be prepared so the content can surface a toast.
     */
    val onPickConfirmed: suspend (selected: List<Media>, onProgress: (done: Int, total: Int) -> Unit) -> Boolean,
)
