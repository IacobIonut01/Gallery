/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dot.gallery.BuildConfig
import com.dot.gallery.R
import com.dot.gallery.ui.theme.GalleryTheme

@Composable
fun SettingsAppHeader() {

    val appName = stringResource(id = R.string.app_name)
    val appVersion = remember { "v${BuildConfig.VERSION_NAME}" }
    val appDeveloper = stringResource(R.string.app_dev, stringResource(R.string.app_dev_name))

    val donateImage = painterResource(id = R.drawable.ic_donate)
    val donateTitle = stringResource(R.string.donate)
    val donateContentDesc = stringResource(R.string.donate_button_cd)
    val donateUrl = stringResource(R.string.donate_url)

    val githubImage = painterResource(id = R.drawable.ic_github)
    val githubTitle = stringResource(R.string.github)
    val githubContentDesc = stringResource(R.string.github_button_cd)
    val githubUrl = stringResource(R.string.github_url)

    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .padding(all = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(all = 24.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = appName,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = appVersion,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = appDeveloper,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { uriHandler.openUri(donateUrl) },
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = .12f),
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f),
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .weight(1f)
                    .semantics {
                        contentDescription = donateContentDesc
                    }
            ) {
                Icon(painter = donateImage, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = donateTitle)
            }
            Button(
                onClick = { uriHandler.openUri(githubUrl) },
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    disabledContentColor = MaterialTheme.colorScheme.onTertiary.copy(alpha = .12f),
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    disabledContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = .12f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .height(52.dp)
                    .semantics {
                        contentDescription = githubContentDesc
                    }
            ) {
                Icon(painter = githubImage, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = githubTitle)
            }
        }
    }
}

@Preview
@Composable
fun Preview() {
    GalleryTheme {
        SettingsAppHeader()
    }
}