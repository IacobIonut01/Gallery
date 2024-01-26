package com.dot.gallery.feature_node.presentation.setup

import android.app.Activity
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dot.gallery.BuildConfig
import com.dot.gallery.R
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Settings.Misc.rememberIsMediaManager
import com.dot.gallery.feature_node.presentation.util.launchManageMedia
import com.dot.gallery.ui.theme.GalleryTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SetupScreen(
    onPermissionGranted: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var firstLaunch by remember { mutableStateOf(true) }
    var permissionGranted by remember { mutableStateOf(false) }
    val mediaPermissions = rememberMultiplePermissionsState(Constants.PERMISSIONS) {
        firstLaunch = false
        permissionGranted = it.all { item -> item.value }
    }
    val appName = "${stringResource(id = R.string.app_name)} v${BuildConfig.VERSION_NAME}"
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            onPermissionGranted()
        } else if (!firstLaunch) Toast.makeText(context,
            context.getString(R.string.some_permissions_are_not_granted), Toast.LENGTH_LONG)
            .show()
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { (context as Activity).finish() }
                ) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }

                Button(
                    onClick = {
                        scope.launch {
                            mediaPermissions.launchMultiplePermissionRequest()
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.get_started))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(top = 24.dp)
                .verticalScroll(state = rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_gallery_thumbnail),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = buildAnnotatedString {
                    val headLineMedium = MaterialTheme.typography.headlineMedium.toSpanStyle()
                    val bodyLarge = MaterialTheme.typography.bodyLarge.toSpanStyle()
                    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
                    withStyle(style = ParagraphStyle(textAlign = TextAlign.Center)) {
                        withStyle(
                            style = headLineMedium
                        ) {
                            append(context.getString(R.string.welcome))
                        }
                        appendLine()
                        withStyle(
                            style = bodyLarge
                                .copy(color = onSurfaceVariant)
                        ) {
                            append(appName)
                        }
                    }
                }
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = buildAnnotatedString {
                        val style = MaterialTheme.typography.bodyMedium.toSpanStyle()
                        withStyle(style = style) {
                            append(stringResource(R.string.setup_summary))
                        }
                        appendLine()
                        appendLine()
                        withStyle(style = style) {
                            append(stringResource(R.string.required))
                        }
                        appendLine()
                        context.requiredPermissionsList.forEach { (title, summary) ->
                            withStyle(style = style.copy(fontWeight = FontWeight.Bold)) {
                                append("• $title")
                            }
                            appendLine()
                            withStyle(style = style) {
                                append("    • $summary")
                            }
                            appendLine()
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            appendLine()
                            withStyle(style = style) {
                                append(stringResource(R.string.optional))
                            }
                            appendLine()
                            withStyle(style = style.copy(fontWeight = FontWeight.Bold)) {
                                append(stringResource(R.string.permission_manage_media_title))
                            }
                            appendLine()
                            withStyle(style = style) {
                                append(stringResource(R.string.permission_manage_media_summary))
                            }
                        }
                    }
                )
                var useMediaManager by rememberIsMediaManager()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !useMediaManager) {
                    Button(
                        onClick = {
                            scope.launch {
                                context.launchManageMedia()
                                useMediaManager = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    ) {
                        Text(text = stringResource(R.string.allow_to_manage_media))
                    }
                }
            }
        }
    }
}

private val Context.requiredPermissionsList: Array<Pair<String, String>> get() {
    return arrayOf(
        getString(R.string.read_media_images) to getString(R.string.read_media_images_summary),
        getString(R.string.read_media_videos) to getString(R.string.read_media_videos_summary),
        getString(R.string.access_media_location) to getString(R.string.access_media_location_summary),
        getString(R.string.internet) to getString(R.string.internet_summary)
    )
}

@Preview
@Composable
fun SetupPreview() {
    GalleryTheme {
        SetupScreen()
    }
}