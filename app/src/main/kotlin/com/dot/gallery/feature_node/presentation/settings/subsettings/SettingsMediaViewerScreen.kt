/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings.subsettings

import androidx.activity.compose.BackHandler
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import com.dot.gallery.feature_node.presentation.util.LocalHazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.LocalTextStyle
import com.dot.gallery.R
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Position
import com.dot.gallery.core.Settings
import com.dot.gallery.core.Settings.Misc.rememberAllowBlur
import com.dot.gallery.core.Settings.Misc.rememberDateHeaderFormat
import com.dot.gallery.core.Settings.Misc.rememberAutoHideOnVideoPlay
import com.dot.gallery.core.Settings.Misc.rememberDefaultImageEditor
import com.dot.gallery.core.Settings.Misc.rememberFullBrightnessView
import com.dot.gallery.core.Settings.Misc.rememberShowFavoriteButton
import com.dot.gallery.core.Settings.Misc.rememberShowMediaViewDateHeader
import com.dot.gallery.core.Settings.Misc.rememberVideoAutoplay
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.util.SdkCompat
import com.dot.gallery.feature_node.presentation.settings.components.BaseSettingsScreen
import com.dot.gallery.feature_node.presentation.settings.components.ChooserPreferenceDetailScreen
import com.dot.gallery.feature_node.presentation.settings.components.PreferenceOption
import com.dot.gallery.feature_node.presentation.settings.components.SwitchPreferenceDetailScreen
import com.dot.gallery.feature_node.presentation.settings.components.rememberPreference
import com.dot.gallery.feature_node.presentation.settings.components.rememberSwitchPreference
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.getEditImageCapableApps
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import androidx.core.graphics.drawable.toBitmap

private const val DETAIL_BRIGHTNESS = "brightness"
private const val DETAIL_DATE_HEADER = "date_header"
private const val DETAIL_FAV_BUTTON = "fav_button"
private const val DETAIL_EDITOR = "editor"
private const val DETAIL_AUTO_HIDE_VIDEO = "auto_hide_video"
private const val DETAIL_AUTO_PLAY = "auto_play"

@Composable
fun SettingsMediaViewerScreen() {
    var detailKey by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var fullBrightnessView by rememberFullBrightnessView()
    var showMediaDateHeader by rememberShowMediaViewDateHeader()
    var showFavoriteButton by rememberShowFavoriteButton()
    var defaultEditor by rememberDefaultImageEditor()
    var autoHideOnVideoPlay by rememberAutoHideOnVideoPlay()
    var autoPlayVideo by rememberVideoAutoplay()

    val editApps = remember(context, context::getEditImageCapableApps)

    when (detailKey) {
        DETAIL_BRIGHTNESS -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(R.string.full_brightness_view_title),
                isChecked = fullBrightnessView,
                onCheckedChange = { fullBrightnessView = it },
                description = stringResource(R.string.full_brightness_view_description),
                preview = { checked -> FullBrightnessPreview(checked) },
            )
        }
        DETAIL_DATE_HEADER -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(R.string.show_date_header),
                isChecked = showMediaDateHeader,
                onCheckedChange = { showMediaDateHeader = it },
                description = stringResource(R.string.show_date_header_description),
                preview = { checked -> DateHeaderPreview(checked) },
                useColumnLayout = true,
            )
        }
        DETAIL_FAV_BUTTON -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(R.string.show_favorite_button),
                isChecked = showFavoriteButton,
                onCheckedChange = { showFavoriteButton = it },
                description = stringResource(R.string.show_favorite_button_description),
                preview = { checked -> FavoriteButtonPreview(checked) },
                useColumnLayout = true,
            )
        }
        DETAIL_EDITOR -> {
            BackHandler { detailKey = null }
            val editorOptions = remember(defaultEditor, editApps) {
                val builtinLabel = context.getString(R.string.default_image_editor_builtin)
                val options = mutableListOf(
                    PreferenceOption(Settings.Misc.EDITOR_BUILTIN, builtinLabel, defaultEditor == Settings.Misc.EDITOR_BUILTIN)
                )
                editApps.forEach { app ->
                    val pkg = app.activityInfo.packageName
                    val label = app.loadLabel(context.packageManager).toString()
                    options.add(PreferenceOption(pkg, label, defaultEditor == pkg))
                }
                options.toList()
            }
            ChooserPreferenceDetailScreen(
                title = stringResource(R.string.default_image_editor),
                description = stringResource(R.string.default_editor_description),
                preview = { EditorPreview(defaultEditor, editApps) },
                options = editorOptions,
                onOptionSelected = { defaultEditor = it },
            )
        }
        DETAIL_AUTO_HIDE_VIDEO -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(R.string.auto_hide_on_video_play),
                isChecked = autoHideOnVideoPlay,
                onCheckedChange = { autoHideOnVideoPlay = it },
                description = stringResource(R.string.auto_hide_on_video_play_description),
            )
        }
        DETAIL_AUTO_PLAY -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(R.string.auto_play_video),
                isChecked = autoPlayVideo,
                onCheckedChange = { autoPlayVideo = it },
                description = stringResource(R.string.auto_play_video_description),
            )
        }
        else -> {
            MediaViewerListScreen(
                fullBrightnessView = fullBrightnessView,
                onBrightnessChange = { fullBrightnessView = it },
                showMediaDateHeader = showMediaDateHeader,
                onDateHeaderChange = { showMediaDateHeader = it },
                showFavoriteButton = showFavoriteButton,
                onFavButtonChange = { showFavoriteButton = it },
                defaultEditor = defaultEditor,
                editApps = editApps,
                autoHideOnVideoPlay = autoHideOnVideoPlay,
                onAutoHideChange = { autoHideOnVideoPlay = it },
                autoPlayVideo = autoPlayVideo,
                onAutoPlayChange = { autoPlayVideo = it },
                onDetailClick = { detailKey = it },
                listState = listState,
            )
        }
    }
}

@Composable
private fun MediaViewerListScreen(
    fullBrightnessView: Boolean,
    onBrightnessChange: (Boolean) -> Unit,
    showMediaDateHeader: Boolean,
    onDateHeaderChange: (Boolean) -> Unit,
    showFavoriteButton: Boolean,
    onFavButtonChange: (Boolean) -> Unit,
    defaultEditor: String,
    editApps: List<android.content.pm.ResolveInfo>,
    autoHideOnVideoPlay: Boolean,
    onAutoHideChange: (Boolean) -> Unit,
    autoPlayVideo: Boolean,
    onAutoPlayChange: (Boolean) -> Unit,
    onDetailClick: (String) -> Unit,
    listState: LazyListState,
) {
    @Composable
    fun settings(): SnapshotStateList<SettingsEntity> {
        val context = LocalContext.current

        val viewingHeader = remember(context) {
            SettingsEntity.Header(title = context.getString(R.string.media_view))
        }

        val fullBrightnessViewPref = rememberSwitchPreference(
            fullBrightnessView,
            title = stringResource(R.string.full_brightness_view_title),
            summary = stringResource(R.string.full_brightness_view_summary),
            isChecked = fullBrightnessView,
            onCheck = onBrightnessChange,
            onClick = { onDetailClick(DETAIL_BRIGHTNESS) },
            screenPosition = Position.Top
        )

        val showMediaDateHeaderPref = rememberSwitchPreference(
            showMediaDateHeader,
            title = stringResource(R.string.show_date_header),
            summary = stringResource(R.string.show_date_header_summary),
            isChecked = showMediaDateHeader,
            onCheck = onDateHeaderChange,
            onClick = { onDetailClick(DETAIL_DATE_HEADER) },
            screenPosition = Position.Middle
        )

        val showFavoriteButtonPref = rememberSwitchPreference(
            showFavoriteButton,
            title = stringResource(R.string.show_favorite_button),
            summary = stringResource(R.string.show_favorite_button_summary),
            isChecked = showFavoriteButton,
            onCheck = onFavButtonChange,
            onClick = { onDetailClick(DETAIL_FAV_BUTTON) },
            screenPosition = Position.Middle
        )

        val editorSummary = remember(defaultEditor, editApps) {
            if (defaultEditor == Settings.Misc.EDITOR_BUILTIN) {
                context.getString(R.string.default_image_editor_builtin)
            } else {
                editApps.find { it.activityInfo.packageName == defaultEditor }
                    ?.loadLabel(context.packageManager)?.toString()
                    ?: context.getString(R.string.default_image_editor_builtin)
            }
        }
        val defaultEditorPref = rememberPreference(
            defaultEditor,
            title = stringResource(R.string.default_image_editor),
            summary = editorSummary,
            onClick = { onDetailClick(DETAIL_EDITOR) },
            screenPosition = Position.Bottom
        )

        val videoPlaybackHeader = remember(context) {
            SettingsEntity.Header(title = context.getString(R.string.video_playback))
        }

        val autoHideOnVideoPlayPref = rememberSwitchPreference(
            autoHideOnVideoPlay,
            title = stringResource(R.string.auto_hide_on_video_play),
            summary = stringResource(R.string.auto_hide_on_video_play_summary),
            isChecked = autoHideOnVideoPlay,
            onCheck = onAutoHideChange,
            onClick = { onDetailClick(DETAIL_AUTO_HIDE_VIDEO) },
            screenPosition = Position.Top
        )

        val autoPlayVideoPref = rememberSwitchPreference(
            autoPlayVideo,
            title = stringResource(R.string.auto_play_video),
            summary = stringResource(R.string.auto_play_video_summary),
            isChecked = autoPlayVideo,
            onCheck = onAutoPlayChange,
            onClick = { onDetailClick(DETAIL_AUTO_PLAY) },
            screenPosition = Position.Bottom
        )

        return remember(
            fullBrightnessViewPref, showMediaDateHeaderPref, showFavoriteButtonPref,
            defaultEditorPref, autoHideOnVideoPlayPref, autoPlayVideoPref
        ) {
            mutableStateListOf<SettingsEntity>().apply {
                add(viewingHeader)
                add(fullBrightnessViewPref)
                add(showMediaDateHeaderPref)
                if (SdkCompat.supportsFavorites) {
                    add(showFavoriteButtonPref)
                }
                add(defaultEditorPref)

                add(videoPlaybackHeader)
                add(autoHideOnVideoPlayPref)
                add(autoPlayVideoPref)
            }
        }
    }

    BaseSettingsScreen(
        title = stringResource(R.string.settings_media_viewer),
        settingsList = settings(),
        listState = listState,
    )
}

@Composable
private fun FullBrightnessPreview(isChecked: Boolean) {
    val bgBrightness = if (isChecked) 1f else 0.5f
    Box(
        modifier = Modifier
            .padding(24.dp)
            .size(width = 140.dp, height = 120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = bgBrightness),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = bgBrightness)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Sun/brightness indicator
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Color.White.copy(alpha = if (isChecked) 0.9f else 0.3f)
                )
        )
    }
}

@Composable
@OptIn(ExperimentalHazeMaterialsApi::class)
private fun DateHeaderPreview(isChecked: Boolean) {
    val dateHeaderFormat by rememberDateHeaderFormat()
    val currentMillis = remember { System.currentTimeMillis() / 1000 }
    val textStyle = LocalTextStyle.current
    val allowBlur by rememberAllowBlur()
    val followTheme = remember(allowBlur) { !allowBlur }
    val contentColor by animateColorAsState(
        targetValue = if (followTheme) MaterialTheme.colorScheme.onSurface else Color.White,
        label = "contentColor"
    )
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
    val backgroundModifier = remember(surfaceContainer) {
        Modifier.background(color = surfaceContainer, shape = CircleShape)
    }

    val currentDate = remember(currentMillis, dateHeaderFormat, textStyle) {
        buildAnnotatedString {
            val date = currentMillis.getDate(dateHeaderFormat)
            if (date.isNotEmpty()) {
                val top = date.substringBefore("\n")
                val bottom = date.substringAfter("\n")
                withStyle(
                    style = textStyle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ).toSpanStyle()
                ) {
                    appendLine(top)
                }
                withStyle(
                    style = textStyle.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp
                    ).toSpanStyle()
                ) {
                    append(bottom)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.image_sample_2),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(32.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black.copy(alpha = 0.1f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clip(CircleShape)
                    .then(backgroundModifier)
                    .hazeEffect(
                        state = LocalHazeState.current,
                        style = HazeMaterials.ultraThin(
                            containerColor = surfaceContainer
                        )
                    ),
                onClick = { }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.height(48.dp)
                )
            }
            if (isChecked) {
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            IconButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clip(CircleShape)
                    .then(backgroundModifier)
                    .hazeEffect(
                        state = LocalHazeState.current,
                        style = HazeMaterials.ultraThin(
                            containerColor = surfaceContainer
                        )
                    ),
                onClick = { }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.height(48.dp)
                )
            }
        }
    }
}

@Composable
private fun FavoriteButtonPreview(isChecked: Boolean) {
    val iconTint = MaterialTheme.colorScheme.onSurface
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.image_sample_2),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(8.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black.copy(alpha = 0.1f))
        )
        // Bottom floating action pill matching MediaViewQuickBottomBar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100))
                    .background(surfaceContainer.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Share
                Box(Modifier.size(32.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Share, null, Modifier.size(18.dp), tint = iconTint)
                }
                // Copy to Clipboard
                Box(Modifier.size(32.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp), tint = iconTint)
                }
                // Favorite (conditionally shown based on setting)
                if (isChecked) {
                    Box(Modifier.size(32.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Favorite, null, Modifier.size(18.dp), tint = iconTint)
                    }
                }
                // Edit
                Box(Modifier.size(32.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp), tint = iconTint)
                }
                // Trash
                Box(Modifier.size(32.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(18.dp), tint = iconTint)
                }
            }
        }
    }
}

@Composable
private fun EditorPreview(
    currentEditor: String,
    editApps: List<android.content.pm.ResolveInfo>
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Auto-scroll to selected item when selection changes
    val allEditors = remember(editApps) {
        val list = mutableListOf(Settings.Misc.EDITOR_BUILTIN)
        editApps.forEach { list.add(it.activityInfo.packageName) }
        list
    }
    val selectedIndex = remember(currentEditor, allEditors) {
        allEditors.indexOf(currentEditor).coerceAtLeast(0)
    }
    LaunchedEffect(selectedIndex) {
        // Estimate scroll position: each card ~120dp + 12dp spacing
        val targetPx = (selectedIndex * 132 * context.resources.displayMetrics.density).toInt()
        scrollState.animateScrollTo(
            (targetPx - 100).coerceAtLeast(0)
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .horizontalScroll(scrollState)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EditorOptionCard(
                label = stringResource(R.string.default_image_editor_builtin),
                selected = currentEditor == Settings.Misc.EDITOR_BUILTIN,
                icon = {
                    Image(
                        painter = rememberDrawablePainter(
                            drawable = AppCompatResources.getDrawable(context, R.mipmap.ic_launcher_round)
                        ),
                        contentDescription = stringResource(R.string.default_image_editor_builtin),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                }
            )
            editApps.forEach { app ->
                val packageName = app.activityInfo.packageName
                val appLabel = remember(app) { app.loadLabel(context.packageManager).toString() }
                val appIcon = remember(app) {
                    try { app.loadIcon(context.packageManager).toBitmap().asImageBitmap() }
                    catch (_: Exception) { null }
                }
                if (appIcon != null) {
                    EditorOptionCard(
                        label = appLabel,
                        selected = currentEditor == packageName,
                        icon = {
                            Image(
                                bitmap = appIcon,
                                contentDescription = appLabel,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                            )
                        }
                    )
                }
            }
        }
        // Soft fade-out gradient on left edge
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(32.dp, 160.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            Color.Transparent
                        )
                    )
                )
        )
        // Soft fade-out gradient on right edge
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(32.dp, 160.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                )
        )
    }
}

@Composable
private fun EditorOptionCard(
    label: String,
    selected: Boolean,
    icon: @Composable () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else Color.Transparent

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}
