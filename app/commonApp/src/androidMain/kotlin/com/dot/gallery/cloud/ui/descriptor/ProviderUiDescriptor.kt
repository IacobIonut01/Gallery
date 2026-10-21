/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.descriptor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.ui.CloudAccountsScreen
import com.dot.gallery.cloud.ui.CloudAddServerScreen
import com.dot.gallery.core.presentation.util.add
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.cloud_api_key
import com.dot.gallery.generated.resources.cloud_api_key_hint
import com.dot.gallery.generated.resources.cloud_email
import com.dot.gallery.generated.resources.cloud_help_immich
import com.dot.gallery.generated.resources.cloud_help_nextcloud
import com.dot.gallery.generated.resources.cloud_help_nfs
import com.dot.gallery.generated.resources.cloud_help_owncloud
import com.dot.gallery.generated.resources.cloud_help_smb
import com.dot.gallery.generated.resources.cloud_help_webdav
import com.dot.gallery.generated.resources.cloud_nextcloud_app_password
import com.dot.gallery.generated.resources.cloud_nfs_uid_gid
import com.dot.gallery.generated.resources.cloud_nfs_uid_gid_hint
import com.dot.gallery.generated.resources.cloud_nfs_url_hint
import com.dot.gallery.generated.resources.cloud_password
import com.dot.gallery.generated.resources.cloud_server_url_hint
import com.dot.gallery.generated.resources.cloud_smb_url_hint
import com.dot.gallery.generated.resources.cloud_smb_username_hint
import com.dot.gallery.generated.resources.cloud_username
import com.dot.gallery.generated.resources.ic_provider_azure
import com.dot.gallery.generated.resources.cloud_help_photoprism
import com.dot.gallery.generated.resources.cloud_photoprism_token
import com.dot.gallery.generated.resources.cloud_photoprism_token_hint
import com.dot.gallery.generated.resources.cloud_photoprism_username
import com.dot.gallery.generated.resources.cloud_photoprism_username_hint
import com.dot.gallery.generated.resources.ic_provider_immich
import com.dot.gallery.generated.resources.ic_provider_photoprism
import com.dot.gallery.generated.resources.ic_provider_nextcloud
import com.dot.gallery.generated.resources.ic_provider_owncloud
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource


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
    val labelRes: StringResource,
    val hintRes: StringResource? = null,
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
    val iconRes: DrawableResource? = null,
    /** When true the icon is tinted with the surrounding content color (monochrome marks); when false it renders at its native colors (e.g. the multi-color Azure logo). */
    val iconTinted: Boolean = true,
    val urlRegex: Regex,
    val urlHintRes: StringResource,
    val credentialFields: List<CredentialField>,
    /** Reachable on the local network only — surfaces a LAN-only note + offline hints. */
    val isLanOnly: Boolean = false,
    /** Optional per-provider "how to connect" help text shown on the add screen. */
    val setupHintRes: StringResource? = null,
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
        labelRes = Res.string.cloud_username
    )
    private val passwordField = CredentialField(
        kind = CredentialFieldKind.PASSWORD,
        labelRes = Res.string.cloud_password,
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
                iconRes = Res.drawable.ic_provider_immich,
                urlRegex = httpRegex,
                urlHintRes = Res.string.cloud_server_url_hint,
                setupHintRes = Res.string.cloud_help_immich,
                credentialFields = listOf(
                    CredentialField(
                        kind = CredentialFieldKind.API_KEY,
                        labelRes = Res.string.cloud_api_key,
                        hintRes = Res.string.cloud_api_key_hint,
                        isSecret = true
                    ),
                    CredentialField(
                        kind = CredentialFieldKind.USERNAME,
                        labelRes = Res.string.cloud_email,
                        keyboardType = KeyboardType.Email,
                        visibleWhen = { it.apiKey.isBlank() }
                    ),
                    CredentialField(
                        kind = CredentialFieldKind.PASSWORD,
                        labelRes = Res.string.cloud_password,
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

        put(
            ProviderType.PHOTOPRISM,
            ProviderUiDescriptor(
                providerType = ProviderType.PHOTOPRISM,
                category = ProviderCategory.STANDALONE,
                icon = Icons.Outlined.Cloud,
                iconRes = Res.drawable.ic_provider_photoprism,
                urlRegex = httpRegex,
                urlHintRes = Res.string.cloud_server_url_hint,
                setupHintRes = Res.string.cloud_help_photoprism,
                // Username/password first so password managers fill the right pair; app
                // password/token last (secret, but not a login password).
                credentialFields = listOf(
                    CredentialField(
                        kind = CredentialFieldKind.USERNAME,
                        labelRes = Res.string.cloud_photoprism_username,
                        hintRes = Res.string.cloud_photoprism_username_hint,
                        visibleWhen = { it.apiKey.isBlank() }
                    ),
                    CredentialField(
                        kind = CredentialFieldKind.PASSWORD,
                        labelRes = Res.string.cloud_password,
                        isSecret = true,
                        keyboardType = KeyboardType.Password,
                        visibleWhen = { it.apiKey.isBlank() }
                    ),
                    CredentialField(
                        kind = CredentialFieldKind.API_KEY,
                        labelRes = Res.string.cloud_photoprism_token,
                        hintRes = Res.string.cloud_photoprism_token_hint,
                        isSecret = true
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
            ProviderType.OWNCLOUD to Res.string.cloud_help_owncloud,
            ProviderType.NEXTCLOUD to Res.string.cloud_help_nextcloud,
            ProviderType.WEBDAV to Res.string.cloud_help_webdav
        )
        val webDavBrandIcons = mapOf(
            ProviderType.OWNCLOUD to Res.drawable.ic_provider_owncloud,
            ProviderType.NEXTCLOUD to Res.drawable.ic_provider_nextcloud
        )
        for (type in listOf(ProviderType.OWNCLOUD, ProviderType.NEXTCLOUD, ProviderType.WEBDAV)) {
            val fields = if (type == ProviderType.NEXTCLOUD) {
                listOf(
                    usernameField,
                    passwordField.copy(labelRes = Res.string.cloud_nextcloud_app_password)
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
                    urlHintRes = Res.string.cloud_server_url_hint,
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
                iconRes = Res.drawable.ic_provider_azure,
                iconTinted = false,
                urlRegex = smbRegex,
                urlHintRes = Res.string.cloud_smb_url_hint,
                isLanOnly = true,
                setupHintRes = Res.string.cloud_help_smb,
                credentialFields = listOf(
                    CredentialField(
                        kind = CredentialFieldKind.USERNAME,
                        labelRes = Res.string.cloud_username,
                        hintRes = Res.string.cloud_smb_username_hint
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
                urlHintRes = Res.string.cloud_nfs_url_hint,
                isLanOnly = true,
                setupHintRes = Res.string.cloud_help_nfs,
                credentialFields = listOf(
                    CredentialField(
                        kind = CredentialFieldKind.USERNAME,
                        labelRes = Res.string.cloud_nfs_uid_gid,
                        hintRes = Res.string.cloud_nfs_uid_gid_hint
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
        urlHintRes = Res.string.cloud_server_url_hint,
        credentialFields = webDavFields
    )

    fun forType(type: ProviderType): ProviderUiDescriptor = descriptors[type] ?: fallback(type)
}
