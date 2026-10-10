/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs

import com.dot.gallery.docs.scenes.DocsPrivateFolderSecurityScene
import com.dot.gallery.docs.scenes.DocsSetupWizardScene
import com.dot.gallery.docs.scenes.DocsVaultGateScene
import com.dot.gallery.docs.scenes.DocsVaultListScene
import com.dot.gallery.docs.scenes.DocsVaultSetupScene

/** First-launch setup, vault and security flows. */
val setupScenes: List<DocsScene> = listOf(
    DocsScene(
        id = "setup-wizard",
        title = "Setup wizard",
        pathTrace = listOf("First launch", "Setup"),
        states = listOf("welcome", "permissions", "appearance", "cloud", "models", "tips"),
    ) { state -> DocsSetupWizardScene(state) },
    DocsScene(
        id = "vault-list",
        title = "Vaults",
        pathTrace = listOf("Library", "Vault"),
    ) { state -> DocsVaultListScene(state) },
    DocsScene(
        id = "vault-setup",
        title = "Create a vault",
        pathTrace = listOf("Library", "Vault", "New vault"),
        states = listOf("available", "no-screen-lock"),
    ) { state -> DocsVaultSetupScene(state) },
    DocsScene(
        id = "vault-gate",
        title = "Vault lock",
        pathTrace = listOf("Library", "Vault", "Security"),
    ) { state -> DocsVaultGateScene(state) },
    DocsScene(
        id = "private-folder-security",
        title = "Private folder security",
        pathTrace = listOf("Settings", "Security", "Private folder", "Lock"),
    ) { state -> DocsPrivateFolderSecurityScene(state) },
)
