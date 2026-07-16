/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings.subsettings

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
import com.dot.gallery.core.Settings.Misc.rememberDisableSmoothing
import com.dot.gallery.core.Settings.Misc.rememberLongPressCutout
import com.dot.gallery.core.Settings.Misc.rememberReencodeJxlEffort
import com.dot.gallery.core.Settings.Misc.rememberReencodeLossyQuality
import com.dot.gallery.core.Settings.Misc.rememberReencodeQualityMode
import com.dot.gallery.core.Settings.Misc.rememberFullBrightnessView
import com.dot.gallery.core.Settings.Misc.rememberShowFavoriteButton
import com.dot.gallery.core.Settings.Misc.rememberShowMediaViewDateHeader
import com.dot.gallery.core.Settings.Misc.rememberVideoAutoplay
import com.dot.gallery.core.Settings.Misc.rememberVideoSurfaceRebind
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.navigate
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.util.SdkCompat
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.settings.components.BaseSettingsScreen
import com.dot.gallery.feature_node.presentation.settings.components.ChooserPreferenceDetailScreen
import com.dot.gallery.feature_node.presentation.settings.components.PreferenceOption
import com.dot.gallery.feature_node.presentation.settings.components.SwitchPreferenceDetailScreen
import com.dot.gallery.feature_node.presentation.settings.components.rememberPreference
import com.dot.gallery.feature_node.presentation.settings.components.rememberSwitchPreference
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.getEditImageCapableApps
import kotlin.math.roundToInt
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import androidx.core.graphics.drawable.toBitmap

private const val DETAIL_BRIGHTNESS = "brightness"
private const val DETAIL_DATE_HEADER = "date_header"
private const val DETAIL_FAV_BUTTON = "fav_button"
private const val DETAIL_EDITOR = "editor"
private const val DETAIL_AUTO_HIDE_VIDEO = "auto_hide_video"
private const val DETAIL_AUTO_PLAY = "auto_play"
private const val DETAIL_SURFACE_REBIND = "surface_rebind"
private const val DETAIL_DISABLE_SMOOTHING = "disable_smoothing"
private const val DETAIL_LONG_PRESS_CUTOUT = "long_press_cutout"

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
    var videoSurfaceRebind by rememberVideoSurfaceRebind()
    var disableSmoothing by rememberDisableSmoothing()
    var longPressCutout by rememberLongPressCutout()
    var reencodeMode by rememberReencodeQualityMode()
    var reencodeLossyQuality by rememberReencodeLossyQuality()
    var reencodeJxlEffort by rememberReencodeJxlEffort()

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
        DETAIL_DISABLE_SMOOTHING -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(R.string.disable_smoothing_title),
                isChecked = disableSmoothing,
                onCheckedChange = { disableSmoothing = it },
                description = stringResource(R.string.disable_smoothing_description),
                preview = { checked -> SmoothingPreview(disableSmoothing = checked) },
            )
        }
        DETAIL_LONG_PRESS_CUTOUT -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(R.string.long_press_cutout_title),
                isChecked = longPressCutout,
                onCheckedChange = { longPressCutout = it },
                description = stringResource(R.string.long_press_cutout_description),
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
        DETAIL_SURFACE_REBIND -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(R.string.video_surface_rebind),
                isChecked = videoSurfaceRebind,
                onCheckedChange = { videoSurfaceRebind = it },
                description = stringResource(R.string.video_surface_rebind_description),
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
                disableSmoothing = disableSmoothing,
                onDisableSmoothingChange = { disableSmoothing = it },
                longPressCutout = longPressCutout,
                onLongPressCutoutChange = { longPressCutout = it },
                autoHideOnVideoPlay = autoHideOnVideoPlay,
                onAutoHideChange = { autoHideOnVideoPlay = it },
                autoPlayVideo = autoPlayVideo,
                onAutoPlayChange = { autoPlayVideo = it },
                videoSurfaceRebind = videoSurfaceRebind,
                onSurfaceRebindChange = { videoSurfaceRebind = it },
                reencodeMode = reencodeMode,
                onReencodeModeChange = { reencodeMode = it },
                reencodeLossyQuality = reencodeLossyQuality,
                onReencodeLossyChange = { reencodeLossyQuality = it },
                reencodeJxlEffort = reencodeJxlEffort,
                onReencodeJxlEffortChange = { reencodeJxlEffort = it },
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
    disableSmoothing: Boolean,
    onDisableSmoothingChange: (Boolean) -> Unit,
    longPressCutout: Boolean,
    onLongPressCutoutChange: (Boolean) -> Unit,
    autoHideOnVideoPlay: Boolean,
    onAutoHideChange: (Boolean) -> Unit,
    autoPlayVideo: Boolean,
    onAutoPlayChange: (Boolean) -> Unit,
    videoSurfaceRebind: Boolean,
    onSurfaceRebindChange: (Boolean) -> Unit,
    reencodeMode: String,
    onReencodeModeChange: (String) -> Unit,
    reencodeLossyQuality: Int,
    onReencodeLossyChange: (Int) -> Unit,
    reencodeJxlEffort: Int,
    onReencodeJxlEffortChange: (Int) -> Unit,
    onDetailClick: (String) -> Unit,
    listState: LazyListState,
) {
    @Composable
    fun settings(): SnapshotStateList<SettingsEntity> {
        val context = LocalContext.current
        val eventHandler = LocalEventHandler.current

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
            screenPosition = Position.Middle
        )

        val disableSmoothingPref = rememberSwitchPreference(
            disableSmoothing,
            title = stringResource(R.string.disable_smoothing_title),
            summary = stringResource(R.string.disable_smoothing_summary),
            isChecked = disableSmoothing,
            onCheck = onDisableSmoothingChange,
            onClick = { onDetailClick(DETAIL_DISABLE_SMOOTHING) },
            screenPosition = Position.Middle
        )

        val longPressCutoutPref = rememberSwitchPreference(
            longPressCutout,
            title = stringResource(R.string.long_press_cutout_title),
            summary = stringResource(R.string.long_press_cutout_summary),
            isChecked = longPressCutout,
            onCheck = onLongPressCutoutChange,
            onClick = { onDetailClick(DETAIL_LONG_PRESS_CUTOUT) },
            screenPosition = Position.Middle
        )

        val slideshowPref = remember(context) {
            SettingsEntity.Preference(
                title = context.getString(R.string.slideshow),
                summary = context.getString(R.string.slideshow_settings_summary),
                onClick = { eventHandler.navigate(Screen.SlideshowSettingsScreen()) },
                screenPosition = Position.Bottom
            )
        }

        // ── Save quality (format-preserving overwrite) ──
        val saveQualityHeader = remember(context) {
            SettingsEntity.Header(title = context.getString(R.string.reencode_quality_title))
        }
        val isManualQuality = reencodeMode == Settings.Misc.REENCODE_MODE_MANUAL
        val manualQualityPref = rememberSwitchPreference(
            reencodeMode,
            title = stringResource(R.string.reencode_quality_mode_manual),
            summary = stringResource(
                if (isManualQuality) R.string.reencode_quality_summary
                else R.string.reencode_quality_mode_auto_summary
            ),
            isChecked = isManualQuality,
            onCheck = { manual ->
                onReencodeModeChange(
                    if (manual) Settings.Misc.REENCODE_MODE_MANUAL
                    else Settings.Misc.REENCODE_MODE_AUTO
                )
            },
            screenPosition = if (isManualQuality) Position.Top else Position.Alone
        )
        val lossyQualityPref = SettingsEntity.SeekPreference(
            title = stringResource(R.string.reencode_quality_lossy),
            currentValue = reencodeLossyQuality.toFloat(),
            minValue = 1f,
            maxValue = 100f,
            step = 0,
            valueMultiplier = 1,
            onSeek = { onReencodeLossyChange(it.roundToInt().coerceIn(1, 100)) },
            screenPosition = Position.Middle
        )
        val jxlEffortPref = SettingsEntity.SeekPreference(
            title = stringResource(R.string.reencode_quality_jxl_effort),
            currentValue = reencodeJxlEffort.toFloat(),
            minValue = 1f,
            maxValue = 9f,
            step = 0,
            valueMultiplier = 1,
            onSeek = { onReencodeJxlEffortChange(it.roundToInt().coerceIn(1, 9)) },
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
            screenPosition = Position.Middle
        )

        val videoSurfaceRebindPref = rememberSwitchPreference(
            videoSurfaceRebind,
            title = stringResource(R.string.video_surface_rebind),
            summary = stringResource(R.string.video_surface_rebind_summary),
            isChecked = videoSurfaceRebind,
            onCheck = onSurfaceRebindChange,
            onClick = { onDetailClick(DETAIL_SURFACE_REBIND) },
            screenPosition = Position.Bottom
        )

        return remember(
            fullBrightnessViewPref, showMediaDateHeaderPref, showFavoriteButtonPref,
            defaultEditorPref, disableSmoothingPref, longPressCutoutPref, slideshowPref,
            saveQualityHeader, manualQualityPref, lossyQualityPref, jxlEffortPref, isManualQuality,
            autoHideOnVideoPlayPref, autoPlayVideoPref, videoSurfaceRebindPref
        ) {
            mutableStateListOf<SettingsEntity>().apply {
                add(viewingHeader)
                add(fullBrightnessViewPref)
                add(showMediaDateHeaderPref)
                if (SdkCompat.supportsFavorites) {
                    add(showFavoriteButtonPref)
                }
                add(defaultEditorPref)
                add(disableSmoothingPref)
                add(longPressCutoutPref)
                add(slideshowPref)

                add(saveQualityHeader)
                add(manualQualityPref)
                if (isManualQuality) {
                    add(lossyQualityPref)
                    add(jxlEffortPref)
                }

                add(videoPlaybackHeader)
                add(autoHideOnVideoPlayPref)
                add(autoPlayVideoPref)
                add(videoSurfaceRebindPref)
            }
        }
    }

    BaseSettingsScreen(
        title = stringResource(R.string.settings_media_viewer),
        settingsList = settings(),
        listState = listState,
        searchRoute = Screen.SettingsMediaViewerScreen(),
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

/**
 * Preview for the "Disable smoothing" setting. Draws a tiny pixel-art sample scaled up so the
 * difference between nearest-neighbor (crisp, [disableSmoothing] = true) and bilinear (smoothed,
 * [disableSmoothing] = false) filtering is clearly visible — the same effect applied in the viewer.
 */
@Composable
private fun SmoothingPreview(disableSmoothing: Boolean) {
    val sample = remember { createSmoothingSampleBitmap() }
    val filterQuality = if (disableSmoothing) FilterQuality.None else FilterQuality.Low
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawImage(
                image = sample,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(sample.width, sample.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = filterQuality
            )
        }
    }
}

/**
 * Builds a small (pixel-art) scene with a sun, sky gradient and two mountains. The curves and
 * diagonals make the smoothing/no-smoothing difference obvious when the bitmap is scaled up.
 */
private fun createSmoothingSampleBitmap(): ImageBitmap {
    val n = 20
    val bitmap = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)

    val skyTop = 0xFF3A6EA5.toInt()
    val skyBottom = 0xFFBFE3F2.toInt()
    val sun = 0xFFFFD34E.toInt()
    val mountainBack = 0xFF6D8C5A.toInt()
    val mountainFront = 0xFF3F6034.toInt()

    val sunCx = n * 0.72f
    val sunCy = n * 0.30f
    val sunR = n * 0.16f

    for (y in 0 until n) {
        for (x in 0 until n) {
            var color = lerpArgb(skyTop, skyBottom, y / (n - 1f))

            val dx = x - sunCx
            val dy = y - sunCy
            if (dx * dx + dy * dy <= sunR * sunR) {
                color = sun
            }

            val backLine = n * 0.62f - (x - n * 0.2f) * 0.35f
            if (y >= backLine) color = mountainBack

            val frontLine = n * 0.95f - kotlin.math.abs(x - n * 0.55f) * 0.9f
            if (y >= frontLine) color = mountainFront

            bitmap.setPixel(x, y, color)
        }
    }
    return bitmap.asImageBitmap()
}

private fun lerpArgb(start: Int, end: Int, fraction: Float): Int {
    val f = fraction.coerceIn(0f, 1f)
    val a = ((start ushr 24 and 0xFF) + (((end ushr 24 and 0xFF) - (start ushr 24 and 0xFF)) * f)).toInt()
    val r = ((start ushr 16 and 0xFF) + (((end ushr 16 and 0xFF) - (start ushr 16 and 0xFF)) * f)).toInt()
    val g = ((start ushr 8 and 0xFF) + (((end ushr 8 and 0xFF) - (start ushr 8 and 0xFF)) * f)).toInt()
    val b = ((start and 0xFF) + (((end and 0xFF) - (start and 0xFF)) * f)).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
