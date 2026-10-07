/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.ui.components.BaseSettingsScreen
import com.dot.gallery.core.ui.components.settings
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.cloud_account_unavailable


@Composable
internal fun AccountSettingsStateScreen(
    title: String,
    state: AccountSettingsLoadState
) {
    BaseSettingsScreen(
        title = title,
        settingsList = remember { emptyList<SettingsEntity>().toMutableStateList() },
        topContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state == AccountSettingsLoadState.LOADING) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = stringResource(Res.string.cloud_account_unavailable),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}
