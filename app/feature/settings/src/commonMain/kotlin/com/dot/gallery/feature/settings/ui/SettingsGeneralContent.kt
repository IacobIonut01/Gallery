/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.ui.components.BaseSettingsScreen
import com.dot.gallery.core.ui.components.ChooserPreferenceDetailScreen
import com.dot.gallery.core.ui.components.PreferenceOption
import com.dot.gallery.core.ui.components.SwitchPreferenceDetailScreen
import com.dot.gallery.core.ui.components.rememberPreference
import com.dot.gallery.core.ui.components.rememberSwitchPreference
import com.dot.gallery.core.ui.settings.rememberPreference
import com.dot.gallery.domain.settings.ALIAS_GALLERY
import com.dot.gallery.domain.settings.ALIAS_GALLERY_MONO
import com.dot.gallery.domain.settings.ALIAS_MONO
import com.dot.gallery.domain.settings.ALIAS_REFRA
import com.dot.gallery.domain.settings.DELETE_SCOPE_ASK
import com.dot.gallery.domain.settings.DELETE_SCOPE_BOTH
import com.dot.gallery.domain.settings.DELETE_SCOPE_CLOUD
import com.dot.gallery.domain.settings.DELETE_SCOPE_DEVICE
import com.dot.gallery.domain.settings.ENCRYPT_ASK
import com.dot.gallery.domain.settings.ENCRYPT_DELETE
import com.dot.gallery.domain.settings.ENCRYPT_KEEP
import com.dot.gallery.domain.settings.SettingsKeys
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.settings.generated.resources.Res
import com.dot.gallery.feature.settings.generated.resources.allow_vibrations
import com.dot.gallery.feature.settings.generated.resources.allow_vibrations_description
import com.dot.gallery.feature.settings.generated.resources.allow_vibrations_summary
import com.dot.gallery.feature.settings.generated.resources.app_logo_description
import com.dot.gallery.feature.settings.generated.resources.app_name_description
import com.dot.gallery.feature.settings.generated.resources.change_app_logo
import com.dot.gallery.feature.settings.generated.resources.change_app_logo_summary
import com.dot.gallery.feature.settings.generated.resources.change_app_name
import com.dot.gallery.feature.settings.generated.resources.change_app_name_summary
import com.dot.gallery.feature.settings.generated.resources.delete_scope_ask
import com.dot.gallery.feature.settings.generated.resources.delete_scope_from_both
import com.dot.gallery.feature.settings.generated.resources.delete_scope_from_cloud
import com.dot.gallery.feature.settings.generated.resources.delete_scope_from_device
import com.dot.gallery.feature.settings.generated.resources.other
import com.dot.gallery.feature.settings.generated.resources.secure_mode_description
import com.dot.gallery.feature.settings.generated.resources.secure_mode_summary
import com.dot.gallery.feature.settings.generated.resources.secure_mode_title
import com.dot.gallery.feature.settings.generated.resources.settings_delete_scope_summary
import com.dot.gallery.feature.settings.generated.resources.settings_delete_scope_title
import com.dot.gallery.feature.settings.generated.resources.settings_general
import com.dot.gallery.feature.settings.generated.resources.settings_open_trash
import com.dot.gallery.feature.settings.generated.resources.settings_open_trash_summary
import com.dot.gallery.feature.settings.generated.resources.settings_trash_confirmation_requires_trash
import com.dot.gallery.feature.settings.generated.resources.settings_trash_confirmation_summary
import com.dot.gallery.feature.settings.generated.resources.settings_trash_confirmation_title
import com.dot.gallery.feature.settings.generated.resources.settings_trash_summary
import com.dot.gallery.feature.settings.generated.resources.settings_trash_title
import com.dot.gallery.feature.settings.generated.resources.trash
import com.dot.gallery.feature.settings.generated.resources.trash_confirmation_description
import com.dot.gallery.feature.settings.generated.resources.trash_confirmation_requires_trash_description
import com.dot.gallery.feature.settings.generated.resources.trash_enabled_description
import com.dot.gallery.feature.settings.generated.resources.vault
import com.dot.gallery.feature.settings.generated.resources.vault_encrypt_ask
import com.dot.gallery.feature.settings.generated.resources.vault_encrypt_behavior
import com.dot.gallery.feature.settings.generated.resources.vault_encrypt_behavior_summary
import com.dot.gallery.feature.settings.generated.resources.vault_encrypt_delete
import com.dot.gallery.feature.settings.generated.resources.vault_encrypt_keep

private const val DETAIL_TRASH = "trash"
private const val DETAIL_TRASH_CONFIRM = "trash_confirm"
private const val DETAIL_DELETE_SCOPE = "delete_scope"
private const val DETAIL_SECURE = "secure"
private const val DETAIL_VIBRATIONS = "vibrations"
private const val DETAIL_APP_NAME = "app_name"
private const val DETAIL_APP_LOGO = "app_logo"
private const val DETAIL_VAULT_ENCRYPT = "vault_encrypt"

/**
 * Launcher-icon previews are platform renderables (mipmap drawables on
 * Android) — callers inject them, the docs scene omits them.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsGeneralContent(
    state: SettingsGeneralState,
    actions: SettingsGeneralActions,
    appNamePreview: (@Composable () -> Unit)? = null,
    appLogoPreview: (@Composable () -> Unit)? = null,
) {
    var detailKey by rememberSaveable { mutableStateOf<String?>(null) }

    var trashCanEnabled by rememberPreference(SettingsKeys.trashEnabled)
    var trashConfirmationEnabled by rememberPreference(SettingsKeys.trashConfirmEnabled)
    var deleteScope by rememberPreference(SettingsKeys.cloudDeleteScope)
    var secureMode by rememberPreference(SettingsKeys.secureMode)
    var allowVibrations by rememberPreference(SettingsKeys.allowVibrations)
    var vaultEncryptBehavior by rememberPreference(SettingsKeys.vaultEncryptBehavior)

    when (detailKey) {
        DETAIL_TRASH -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(Res.string.settings_trash_title),
                isChecked = trashCanEnabled,
                onCheckedChange = { trashCanEnabled = it },
                description = stringResource(Res.string.trash_enabled_description),
            )
        }
        DETAIL_TRASH_CONFIRM -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(Res.string.settings_trash_confirmation_title),
                isChecked = trashConfirmationEnabled,
                onCheckedChange = { trashConfirmationEnabled = it },
                description = if (trashCanEnabled) {
                    stringResource(Res.string.trash_confirmation_description)
                } else {
                    stringResource(Res.string.trash_confirmation_requires_trash_description)
                },
                enabled = trashCanEnabled,
            )
        }
        DETAIL_DELETE_SCOPE -> {
            BackHandler { detailKey = null }
            ChooserPreferenceDetailScreen(
                title = stringResource(Res.string.settings_delete_scope_title),
                description = stringResource(Res.string.settings_delete_scope_summary),
                options = listOf(
                    PreferenceOption(
                        DELETE_SCOPE_ASK,
                        stringResource(Res.string.delete_scope_ask),
                        deleteScope == DELETE_SCOPE_ASK
                    ),
                    PreferenceOption(
                        DELETE_SCOPE_DEVICE,
                        stringResource(Res.string.delete_scope_from_device),
                        deleteScope == DELETE_SCOPE_DEVICE
                    ),
                    PreferenceOption(
                        DELETE_SCOPE_CLOUD,
                        stringResource(Res.string.delete_scope_from_cloud),
                        deleteScope == DELETE_SCOPE_CLOUD
                    ),
                    PreferenceOption(
                        DELETE_SCOPE_BOTH,
                        stringResource(Res.string.delete_scope_from_both),
                        deleteScope == DELETE_SCOPE_BOTH
                    ),
                ),
                onOptionSelected = { deleteScope = it },
            )
        }
        DETAIL_SECURE -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(Res.string.secure_mode_title),
                isChecked = secureMode,
                onCheckedChange = { secureMode = it },
                description = stringResource(Res.string.secure_mode_description),
                preview = { checked -> SecureModePreview(checked) },
            )
        }
        DETAIL_VIBRATIONS -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(Res.string.allow_vibrations),
                isChecked = allowVibrations,
                onCheckedChange = { allowVibrations = it },
                description = stringResource(Res.string.allow_vibrations_description),
            )
        }
        DETAIL_APP_NAME -> {
            BackHandler { detailKey = null }
            ChooserPreferenceDetailScreen(
                title = stringResource(Res.string.change_app_name),
                description = stringResource(Res.string.app_name_description),
                preview = appNamePreview?.let { p -> { p() } },
                options = listOf(
                    PreferenceOption(ALIAS_REFRA, ALIAS_REFRA, state.appNameAlias == ALIAS_REFRA),
                    PreferenceOption(
                        ALIAS_GALLERY, ALIAS_GALLERY,
                        state.appNameAlias == ALIAS_GALLERY
                    ),
                ),
                onOptionSelected = { actions.onAppAliasChange(it, state.appLogoAlias) },
            )
        }
        DETAIL_APP_LOGO -> {
            BackHandler { detailKey = null }
            ChooserPreferenceDetailScreen(
                title = stringResource(Res.string.change_app_logo),
                description = stringResource(Res.string.app_logo_description),
                preview = appLogoPreview?.let { p -> { p() } },
                options = listOf(
                    PreferenceOption(ALIAS_REFRA, ALIAS_REFRA, state.appLogoAlias == ALIAS_REFRA),
                    PreferenceOption(
                        ALIAS_GALLERY, ALIAS_GALLERY,
                        state.appLogoAlias == ALIAS_GALLERY
                    ),
                    PreferenceOption(ALIAS_MONO, ALIAS_MONO, state.appLogoAlias == ALIAS_MONO),
                    PreferenceOption(
                        ALIAS_GALLERY_MONO, ALIAS_GALLERY_MONO,
                        state.appLogoAlias == ALIAS_GALLERY_MONO
                    ),
                ),
                onOptionSelected = { actions.onAppAliasChange(state.appNameAlias, it) },
            )
        }
        DETAIL_VAULT_ENCRYPT -> {
            BackHandler { detailKey = null }
            ChooserPreferenceDetailScreen(
                title = stringResource(Res.string.vault_encrypt_behavior),
                description = stringResource(Res.string.vault_encrypt_behavior_summary),
                options = listOf(
                    PreferenceOption(
                        ENCRYPT_ASK,
                        stringResource(Res.string.vault_encrypt_ask),
                        vaultEncryptBehavior == ENCRYPT_ASK
                    ),
                    PreferenceOption(
                        ENCRYPT_DELETE,
                        stringResource(Res.string.vault_encrypt_delete),
                        vaultEncryptBehavior == ENCRYPT_DELETE
                    ),
                    PreferenceOption(
                        ENCRYPT_KEEP,
                        stringResource(Res.string.vault_encrypt_keep),
                        vaultEncryptBehavior == ENCRYPT_KEEP
                    ),
                ),
                onOptionSelected = { vaultEncryptBehavior = it },
            )
        }
        else -> {
            GeneralListScreen(
                trashCanEnabled = trashCanEnabled,
                onTrashChange = { trashCanEnabled = it },
                trashConfirmationEnabled = trashConfirmationEnabled,
                onTrashConfirmChange = { trashConfirmationEnabled = it },
                deleteScope = deleteScope,
                secureMode = secureMode,
                onSecureChange = { secureMode = it },
                allowVibrations = allowVibrations,
                onVibrationsChange = { allowVibrations = it },
                appNameAlias = state.appNameAlias,
                appLogoAlias = state.appLogoAlias,
                vaultEncryptBehavior = vaultEncryptBehavior,
                supportsTrash = state.supportsTrash,
                onDetailClick = { detailKey = it },
                onOpenTrash = actions.onOpenTrash,
                searchRoute = actions.searchRoute,
                searchSink = actions.searchSink,
            )
        }
    }
}

@Composable
private fun GeneralListScreen(
    trashCanEnabled: Boolean,
    onTrashChange: (Boolean) -> Unit,
    trashConfirmationEnabled: Boolean,
    onTrashConfirmChange: (Boolean) -> Unit,
    deleteScope: String,
    secureMode: Boolean,
    onSecureChange: (Boolean) -> Unit,
    allowVibrations: Boolean,
    onVibrationsChange: (Boolean) -> Unit,
    appNameAlias: String,
    appLogoAlias: String,
    vaultEncryptBehavior: String,
    supportsTrash: Boolean,
    onDetailClick: (String) -> Unit,
    onOpenTrash: () -> Unit,
    searchRoute: String?,
    searchSink: ((String, List<String>) -> Unit)?,
) {
    val listState = rememberLazyListState()

    val openTrashPref = rememberPreference(
        title = stringResource(Res.string.settings_open_trash),
        summary = stringResource(Res.string.settings_open_trash_summary),
        onClick = onOpenTrash,
        screenPosition = Position.Top,
    )

    val trashCanEnabledPref = rememberSwitchPreference(
        trashCanEnabled,
        title = stringResource(Res.string.settings_trash_title),
        summary = stringResource(Res.string.settings_trash_summary),
        isChecked = trashCanEnabled,
        onCheck = onTrashChange,
        onClick = { onDetailClick(DETAIL_TRASH) },
        screenPosition = Position.Middle
    )

    val trashConfirmationEnabledPref = rememberSwitchPreference(
        trashConfirmationEnabled,
        trashCanEnabled,
        title = stringResource(Res.string.settings_trash_confirmation_title),
        summary = stringResource(
            if (trashCanEnabled) {
                Res.string.settings_trash_confirmation_summary
            } else {
                Res.string.settings_trash_confirmation_requires_trash
            }
        ),
        enabled = trashCanEnabled,
        isChecked = trashConfirmationEnabled,
        onCheck = onTrashConfirmChange,
        onClick = { onDetailClick(DETAIL_TRASH_CONFIRM) },
        screenPosition = Position.Bottom
    )

    val secureModePref = rememberSwitchPreference(
        secureMode,
        title = stringResource(Res.string.secure_mode_title),
        summary = stringResource(Res.string.secure_mode_summary),
        isChecked = secureMode,
        onCheck = onSecureChange,
        onClick = { onDetailClick(DETAIL_SECURE) },
        screenPosition = Position.Top
    )

    val allowVibrationsPref = rememberSwitchPreference(
        allowVibrations,
        title = stringResource(Res.string.allow_vibrations),
        summary = stringResource(Res.string.allow_vibrations_summary),
        isChecked = allowVibrations,
        onCheck = onVibrationsChange,
        onClick = { onDetailClick(DETAIL_VIBRATIONS) },
        screenPosition = Position.Middle
    )

    val deleteScopeSummary = when (deleteScope) {
        DELETE_SCOPE_DEVICE -> stringResource(Res.string.delete_scope_from_device)
        DELETE_SCOPE_CLOUD -> stringResource(Res.string.delete_scope_from_cloud)
        DELETE_SCOPE_BOTH -> stringResource(Res.string.delete_scope_from_both)
        else -> stringResource(Res.string.delete_scope_ask)
    }
    val deleteScopePref = rememberPreference(
        deleteScope,
        title = stringResource(Res.string.settings_delete_scope_title),
        summary = deleteScopeSummary,
        onClick = { onDetailClick(DETAIL_DELETE_SCOPE) },
        screenPosition = Position.Middle
    )

    val appNamePref = rememberPreference(
        appNameAlias,
        title = stringResource(Res.string.change_app_name),
        summary = stringResource(Res.string.change_app_name_summary),
        onClick = { onDetailClick(DETAIL_APP_NAME) },
        screenPosition = Position.Middle
    )

    val appLogoPref = rememberPreference(
        appLogoAlias,
        title = stringResource(Res.string.change_app_logo),
        summary = stringResource(Res.string.change_app_logo_summary),
        onClick = { onDetailClick(DETAIL_APP_LOGO) },
        screenPosition = Position.Bottom
    )

    val vaultEncryptBehaviorSummary = when (vaultEncryptBehavior) {
        ENCRYPT_DELETE -> stringResource(Res.string.vault_encrypt_delete)
        ENCRYPT_KEEP -> stringResource(Res.string.vault_encrypt_keep)
        else -> stringResource(Res.string.vault_encrypt_ask)
    }
    val vaultEncryptPref = rememberPreference(
        vaultEncryptBehavior,
        title = stringResource(Res.string.vault_encrypt_behavior),
        summary = vaultEncryptBehaviorSummary,
        onClick = { onDetailClick(DETAIL_VAULT_ENCRYPT) },
        screenPosition = Position.Alone
    )

    val trashHeader = stringResource(Res.string.trash)
    val otherHeader = stringResource(Res.string.other)
    val vaultHeader = stringResource(Res.string.vault)

    val settingsList: SnapshotStateList<SettingsEntity> = remember(
        openTrashPref, trashCanEnabledPref, trashConfirmationEnabledPref,
        secureModePref, allowVibrationsPref, deleteScopePref, appNamePref,
        appLogoPref, vaultEncryptPref, trashHeader, otherHeader, vaultHeader,
        supportsTrash,
    ) {
        mutableStateListOf<SettingsEntity>().apply {
            if (supportsTrash) {
                add(SettingsEntity.Header(title = trashHeader))
                add(openTrashPref)
                add(trashCanEnabledPref)
                add(trashConfirmationEnabledPref)
            }
            add(SettingsEntity.Header(title = otherHeader))
            add(secureModePref)
            add(allowVibrationsPref)
            add(deleteScopePref)
            add(appNamePref)
            add(appLogoPref)
            add(SettingsEntity.Header(title = vaultHeader))
            add(vaultEncryptPref)
        }
    }

    BaseSettingsScreen(
        title = stringResource(Res.string.settings_general),
        settingsList = settingsList,
        listState = listState,
        searchRoute = searchRoute,
        searchSink = searchSink,
    )
}

@Composable
private fun SecureModePreview(isChecked: Boolean) {
    val overlayAlpha by animateFloatAsState(if (isChecked) 1f else 0f, label = "overlay")
    val cellColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(3.dp)).background(cellColor))
            repeat(2) {
                Row(Modifier.weight(1f).fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
                    repeat(3) {
                        Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(cellColor))
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f * overlayAlpha),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f * overlayAlpha)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (overlayAlpha > 0f) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = overlayAlpha)
                )
            }
        }
    }
}
