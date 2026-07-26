/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.descriptor

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import com.dot.gallery.R
import com.dot.gallery.cloud.core.ProviderType

/**
 * Visual grouping for the "add a provider" gallery. Drives the section headers on the
 * accounts screen so new providers slot into the right family without touching the screen.
 */
enum class ProviderCategory {
    /** Shown as its own card with no section header (e.g. Immich). */
    STANDALONE,

    /** WebDAV-protocol servers grouped under "ownCloud variants". */
    OWNCLOUD_VARIANT,

    /** LAN network filesystems (SMB/NFS) grouped under "Network shares". */
    NETWORK_SHARE
}

/** The credential value triple the add-form holds; used for visibility + validation rules. */
data class CredentialValues(
    val apiKey: String,
    val username: String,
    val password: String
)

/** Identifies which piece of [CredentialValues] a [CredentialField] binds to. */
enum class CredentialFieldKind { API_KEY, USERNAME, PASSWORD }

/**
 * A single text field rendered in the credentials section of the add-form. The screen maps
 * [kind] onto the relevant state value + updater, so all per-provider wording and behaviour
 * lives here in the descriptor instead of in a chain of `when` branches.
 */
data class CredentialField(
    val kind: CredentialFieldKind,
    val labelRes: Int,
    val hintRes: Int? = null,
    val isSecret: Boolean = false,
    val keyboardType: KeyboardType = KeyboardType.Text,
    /** Field is only rendered when this predicate holds (e.g. Immich hides user/pass once an API key is set). */
    val visibleWhen: (CredentialValues) -> Boolean = { true }
)

/**
 * Data-driven description of how a [ProviderType] should be presented in the connection UI.
 * Both [com.dot.gallery.cloud.ui.CloudAddServerScreen] and
 * [com.dot.gallery.cloud.ui.CloudAccountsScreen] consume these so adding a provider is a
 * registry entry rather than edits scattered across the screens.
 */
data class ProviderUiDescriptor(
    val providerType: ProviderType,
    val category: ProviderCategory,
    /** Fallback Material vector used when no brand [iconRes] is provided. */
    val icon: ImageVector,
    /** Optional brand logo drawable. Takes precedence over [icon] when set. */
    @param:DrawableRes val iconRes: Int? = null,
    /** When true the icon is tinted with the surrounding content color (monochrome marks); when false it renders at its native colors (e.g. the multi-color Azure logo). */
    val iconTinted: Boolean = true,
    val urlRegex: Regex,
    val urlHintRes: Int,
    val credentialFields: List<CredentialField>,
    /** Reachable on the local network only — surfaces a LAN-only note + offline hints. */
    val isLanOnly: Boolean = false,
    /** Optional per-provider "how to connect" help text shown on the add screen. */
    val setupHintRes: Int? = null,
    /** Whether the entered credentials are sufficient to enable Save. */
    val credentialsSatisfied: (CredentialValues) -> Boolean = { true }
)

/** Registry of [ProviderUiDescriptor]s keyed by [ProviderType]. */
object ProviderUiDescriptors {

    private val httpRegex = Regex("^https?://.+")
    private val smbRegex = Regex("^smb://.+", RegexOption.IGNORE_CASE)
    private val nfsRegex = Regex("^nfs://.+", RegexOption.IGNORE_CASE)

    private val usernameField = CredentialField(
        kind = CredentialFieldKind.USERNAME,
        labelRes = R.string.cloud_username
    )
    private val passwordField = CredentialField(
        kind = CredentialFieldKind.PASSWORD,
        labelRes = R.string.cloud_password,
        isSecret = true,
        keyboardType = KeyboardType.Password
    )

    private val webDavFields = listOf(usernameField, passwordField)

    private val descriptors: Map<ProviderType, ProviderUiDescriptor> = buildMap {
        put(
            ProviderType.IMMICH,
            ProviderUiDescriptor(
                providerType = ProviderType.IMMICH,
                category = ProviderCategory.STANDALONE,
                icon = Icons.Outlined.Cloud,
                iconRes = R.drawable.ic_provider_immich,
                urlRegex = httpRegex,
                urlHintRes = R.string.cloud_server_url_hint,
                setupHintRes = R.string.cloud_help_immich,
                credentialFields = listOf(
                    CredentialField(
                        kind = CredentialFieldKind.API_KEY,
                        labelRes = R.string.cloud_api_key,
                        hintRes = R.string.cloud_api_key_hint,
                        isSecret = true
                    ),
                    CredentialField(
                        kind = CredentialFieldKind.USERNAME,
                        labelRes = R.string.cloud_email,
                        keyboardType = KeyboardType.Email,
                        visibleWhen = { it.apiKey.isBlank() }
                    ),
                    CredentialField(
                        kind = CredentialFieldKind.PASSWORD,
                        labelRes = R.string.cloud_password,
                        isSecret = true,
                        keyboardType = KeyboardType.Password,
                        visibleWhen = { it.apiKey.isBlank() }
                    )
                ),
                credentialsSatisfied = {
                    it.apiKey.isNotBlank() || (it.username.isNotBlank() && it.password.isNotBlank())
                }
            )
        )

        val webDavSatisfied: (CredentialValues) -> Boolean =
            { it.username.isNotBlank() && it.password.isNotBlank() }
        val webDavHints = mapOf(
            ProviderType.OWNCLOUD to R.string.cloud_help_owncloud,
            ProviderType.NEXTCLOUD to R.string.cloud_help_nextcloud,
            ProviderType.WEBDAV to R.string.cloud_help_webdav
        )
        val webDavBrandIcons = mapOf(
            ProviderType.OWNCLOUD to R.drawable.ic_provider_owncloud,
            ProviderType.NEXTCLOUD to R.drawable.ic_provider_nextcloud
        )
        for (type in listOf(ProviderType.OWNCLOUD, ProviderType.NEXTCLOUD, ProviderType.WEBDAV)) {
            val fields = if (type == ProviderType.NEXTCLOUD) {
                listOf(
                    usernameField,
                    passwordField.copy(labelRes = R.string.cloud_nextcloud_app_password)
                )
            } else webDavFields
            put(
                type,
                ProviderUiDescriptor(
                    providerType = type,
                    category = ProviderCategory.OWNCLOUD_VARIANT,
                    icon = Icons.Outlined.CloudQueue,
                    iconRes = webDavBrandIcons[type],
                    urlRegex = httpRegex,
                    urlHintRes = R.string.cloud_server_url_hint,
                    setupHintRes = webDavHints[type],
                    credentialFields = fields,
                    credentialsSatisfied = webDavSatisfied
                )
            )
        }

        put(
            ProviderType.SMB,
            ProviderUiDescriptor(
                providerType = ProviderType.SMB,
                category = ProviderCategory.NETWORK_SHARE,
                icon = Icons.Outlined.Lan,
                iconRes = R.drawable.ic_provider_azure,
                iconTinted = false,
                urlRegex = smbRegex,
                urlHintRes = R.string.cloud_smb_url_hint,
                isLanOnly = true,
                setupHintRes = R.string.cloud_help_smb,
                credentialFields = listOf(
                    CredentialField(
                        kind = CredentialFieldKind.USERNAME,
                        labelRes = R.string.cloud_username,
                        hintRes = R.string.cloud_smb_username_hint
                    ),
                    passwordField
                )
            )
        )

        put(
            ProviderType.NFS,
            ProviderUiDescriptor(
                providerType = ProviderType.NFS,
                category = ProviderCategory.NETWORK_SHARE,
                icon = Icons.Outlined.Lan,
                urlRegex = nfsRegex,
                urlHintRes = R.string.cloud_nfs_url_hint,
                isLanOnly = true,
                setupHintRes = R.string.cloud_help_nfs,
                credentialFields = listOf(
                    CredentialField(
                        kind = CredentialFieldKind.USERNAME,
                        labelRes = R.string.cloud_nfs_uid_gid,
                        hintRes = R.string.cloud_nfs_uid_gid_hint
                    )
                )
            )
        )
    }

    private fun fallback(type: ProviderType) = ProviderUiDescriptor(
        providerType = type,
        category = ProviderCategory.STANDALONE,
        icon = Icons.Outlined.Cloud,
        urlRegex = httpRegex,
        urlHintRes = R.string.cloud_server_url_hint,
        credentialFields = webDavFields
    )

    fun forType(type: ProviderType): ProviderUiDescriptor = descriptors[type] ?: fallback(type)
}
