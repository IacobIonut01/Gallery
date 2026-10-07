/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.location.MapAppearance
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.core.ui.components.ChooserPreferenceDetailScreen
import com.dot.gallery.core.ui.components.PreferenceOption
import com.dot.gallery.core.ui.components.RequestInitialSettingsFocus
import com.dot.gallery.core.ui.components.SettingsItem
import com.dot.gallery.core.ui.components.SwitchPreferenceDetailScreen
import com.dot.gallery.core.ui.components.rememberSettingsFocusState
import com.dot.gallery.core.ui.components.settingsFocusTarget
import com.dot.gallery.core.ui.components.rememberPreference
import com.dot.gallery.core.ui.components.rememberSwitchPreference
import com.dot.gallery.core.ui.settings.rememberPreference
import com.dot.gallery.domain.settings.SettingsKeys
import com.dot.gallery.ui.core.icons.Albums
import com.dot.gallery.ui.theme.THEME_SEED_NEUTRAL
import com.dot.gallery.ui.theme.THEME_SEED_SYSTEM
import com.dot.gallery.ui.theme.colorSchemeFromSeed
import com.dot.gallery.ui.theme.isDarkTheme
import com.dot.gallery.ui.theme.neutralColorScheme
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.settings.generated.resources.Res
import com.dot.gallery.feature.settings.generated.resources.amoled_mode_description
import com.dot.gallery.feature.settings.generated.resources.amoled_mode_requires_dark
import com.dot.gallery.feature.settings.generated.resources.amoled_mode_requires_dark_description
import com.dot.gallery.feature.settings.generated.resources.amoled_mode_summary
import com.dot.gallery.feature.settings.generated.resources.amoled_mode_title
import com.dot.gallery.feature.settings.generated.resources.auto_contrast
import com.dot.gallery.feature.settings.generated.resources.auto_contrast_description
import com.dot.gallery.feature.settings.generated.resources.auto_contrast_summary
import com.dot.gallery.feature.settings.generated.resources.cancel
import com.dot.gallery.feature.settings.generated.resources.carto_basemap_key_configured
import com.dot.gallery.feature.settings.generated.resources.carto_basemap_key_description
import com.dot.gallery.feature.settings.generated.resources.carto_basemap_key_field
import com.dot.gallery.feature.settings.generated.resources.carto_basemap_key_not_configured
import com.dot.gallery.feature.settings.generated.resources.carto_basemap_key_title
import com.dot.gallery.feature.settings.generated.resources.color_palette_other_colors
import com.dot.gallery.feature.settings.generated.resources.color_palette_preview_description
import com.dot.gallery.feature.settings.generated.resources.color_palette_wallpaper_colors
import com.dot.gallery.feature.settings.generated.resources.dark_mode_description
import com.dot.gallery.feature.settings.generated.resources.fancy_blur
import com.dot.gallery.feature.settings.generated.resources.fancy_blur_description
import com.dot.gallery.feature.settings.generated.resources.fancy_blur_requires_android_12
import com.dot.gallery.feature.settings.generated.resources.fancy_blur_summary
import com.dot.gallery.feature.settings.generated.resources.follow_system_theme_description
import com.dot.gallery.feature.settings.generated.resources.header_today
import com.dot.gallery.feature.settings.generated.resources.map_appearance_dark
import com.dot.gallery.feature.settings.generated.resources.map_appearance_description
import com.dot.gallery.feature.settings.generated.resources.map_appearance_header
import com.dot.gallery.feature.settings.generated.resources.map_appearance_light
import com.dot.gallery.feature.settings.generated.resources.map_appearance_system
import com.dot.gallery.feature.settings.generated.resources.map_appearance_title
import com.dot.gallery.feature.settings.generated.resources.save
import com.dot.gallery.feature.settings.generated.resources.search
import com.dot.gallery.feature.settings.generated.resources.settings_appearance
import com.dot.gallery.feature.settings.generated.resources.settings_dark_mode_title
import com.dot.gallery.feature.settings.generated.resources.settings_follow_system_theme_title
import com.dot.gallery.feature.settings.generated.resources.settings_font_header
import com.dot.gallery.feature.settings.generated.resources.settings_visual_effects_header
import com.dot.gallery.feature.settings.generated.resources.shared_elements
import com.dot.gallery.feature.settings.generated.resources.shared_elements_description
import com.dot.gallery.feature.settings.generated.resources.shared_elements_summary
import com.dot.gallery.feature.settings.generated.resources.use_system_font_description
import com.dot.gallery.feature.settings.generated.resources.use_system_font_summary
import com.dot.gallery.feature.settings.generated.resources.use_system_font_title

private const val DETAIL_FOLLOW_SYSTEM = "follow_system"
private const val DETAIL_DARK_MODE = "dark_mode"
private const val DETAIL_AMOLED = "amoled"
private const val DETAIL_BLUR = "blur"
private const val DETAIL_AUTO_CONTRAST = "auto_contrast"
private const val DETAIL_SHARED = "shared"
private const val DETAIL_SYSTEM_FONT = "system_font"
private const val DETAIL_MAP_APPEARANCE = "map_appearance"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ColorPaletteContent(state: ColorPaletteState) {
    var detailKey by rememberSaveable { mutableStateOf<String?>(null) }
    val contentScrollState = rememberScrollState()
    val initialFocusRequester = remember { FocusRequester() }
    RequestInitialSettingsFocus(initialFocusRequester, detailKey == null)
    var themeColorSeed by rememberPreference(SettingsKeys.themeColorSeed)
    var forceTheme by rememberPreference(SettingsKeys.forceTheme)
    var darkModeValue by rememberPreference(SettingsKeys.darkMode)
    var amoledModeValue by rememberPreference(SettingsKeys.amoledMode)
    var allowBlur by rememberPreference(SettingsKeys.allowBlur)
    var autoContrast by rememberPreference(SettingsKeys.autoContrast)
    var sharedElements by rememberPreference(SettingsKeys.sharedElements)
    var useSystemFont by rememberPreference(SettingsKeys.useSystemFont)
    var mapAppearance by rememberMapAppearance()
    var userCartoKey by rememberPreference(SettingsKeys.cartoBasemapKey)
    var cartoKeyDraft by remember { mutableStateOf("") }
    var showCartoKeyDialog by remember { mutableStateOf(false) }
    val isDark = isDarkTheme()

    when (detailKey) {
        DETAIL_FOLLOW_SYSTEM -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(Res.string.settings_follow_system_theme_title),
                isChecked = !forceTheme,
                onCheckedChange = { forceTheme = !it },
                description = stringResource(Res.string.follow_system_theme_description),
            )
            return
        }
        DETAIL_DARK_MODE -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(Res.string.settings_dark_mode_title),
                isChecked = darkModeValue,
                onCheckedChange = { darkModeValue = it },
                description = stringResource(Res.string.dark_mode_description),
                preview = { checked -> DarkModePreview(checked) },
                enabled = forceTheme,
            )
            return
        }
        DETAIL_AMOLED -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(Res.string.amoled_mode_title),
                isChecked = amoledModeValue,
                onCheckedChange = { amoledModeValue = it },
                description = stringResource(
                    if (isDark) Res.string.amoled_mode_description
                    else Res.string.amoled_mode_requires_dark_description
                ),
                preview = { checked -> AmoledPreview(checked) },
                enabled = isDark,
            )
            return
        }
        DETAIL_BLUR -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(Res.string.fancy_blur),
                isChecked = allowBlur,
                onCheckedChange = { allowBlur = it },
                description = stringResource(Res.string.fancy_blur_description),
                preview = { checked -> BlurPreview(checked) },
                enabled = state.supportsBlur,
            )
            return
        }
        DETAIL_AUTO_CONTRAST -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(Res.string.auto_contrast),
                isChecked = autoContrast,
                onCheckedChange = { autoContrast = it },
                description = stringResource(Res.string.auto_contrast_description),
            )
            return
        }
        DETAIL_SHARED -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(Res.string.shared_elements),
                isChecked = sharedElements,
                onCheckedChange = { sharedElements = it },
                description = stringResource(Res.string.shared_elements_description),
            )
            return
        }
        DETAIL_SYSTEM_FONT -> {
            BackHandler { detailKey = null }
            SwitchPreferenceDetailScreen(
                title = stringResource(Res.string.use_system_font_title),
                isChecked = useSystemFont,
                onCheckedChange = { useSystemFont = it },
                description = stringResource(Res.string.use_system_font_description),
            )
            return
        }
        DETAIL_MAP_APPEARANCE -> {
            BackHandler { detailKey = null }
            ChooserPreferenceDetailScreen(
                title = stringResource(Res.string.map_appearance_title),
                description = stringResource(Res.string.map_appearance_description),
                options = listOf(
                    PreferenceOption(MapAppearance.SYSTEM, stringResource(Res.string.map_appearance_system), mapAppearance == MapAppearance.SYSTEM),
                    PreferenceOption(MapAppearance.LIGHT, stringResource(Res.string.map_appearance_light), mapAppearance == MapAppearance.LIGHT),
                    PreferenceOption(MapAppearance.DARK, stringResource(Res.string.map_appearance_dark), mapAppearance == MapAppearance.DARK),
                ),
                onOptionSelected = { mapAppearance = it },
            )
            return
        }
    }

    var selectedTab by rememberSaveable(themeColorSeed) {
        mutableIntStateOf(
            if (themeColorSeed == THEME_SEED_SYSTEM) 0
            else if (presetPalettes.any { it.hexKey == themeColorSeed }) 1
            else 0
        )
    }

    val previewScheme = remember(themeColorSeed, isDark, amoledModeValue) {
        if (themeColorSeed == THEME_SEED_SYSTEM) {
            null
        } else if (themeColorSeed == THEME_SEED_NEUTRAL) {
            neutralColorScheme(isDark, amoledModeValue)
        } else {
            val seedArgb = themeColorSeed.toLongOrNull(16)?.toInt() ?: return@remember null
            colorSchemeFromSeed(seedArgb, isDark, amoledModeValue)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.settings_appearance)
                    )
                },
                navigationIcon = {
                    NavigationBackButton()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        val containerSize = LocalWindowInfo.current.containerSize
        val isLandscape = containerSize.width > containerSize.height

        // Theme switches (shared between layouts)
        val followSystemPref = rememberSwitchPreference(
            forceTheme,
            title = stringResource(Res.string.settings_follow_system_theme_title),
            summary = stringResource(Res.string.follow_system_theme_description),
            isChecked = !forceTheme,
            onCheck = { forceTheme = !it },
            onClick = { detailKey = DETAIL_FOLLOW_SYSTEM },
            screenPosition = Position.Top
        )
        val darkModePref = rememberSwitchPreference(
            darkModeValue, forceTheme,
            title = stringResource(Res.string.settings_dark_mode_title),
            summary = stringResource(Res.string.dark_mode_description),
            enabled = forceTheme,
            isChecked = darkModeValue,
            onCheck = { darkModeValue = it },
            onClick = { detailKey = DETAIL_DARK_MODE },
            screenPosition = Position.Middle
        )
        val amoledModePref = rememberSwitchPreference(
            amoledModeValue,
            isDark,
            title = stringResource(Res.string.amoled_mode_title),
            summary = stringResource(
                if (isDark) Res.string.amoled_mode_summary
                else Res.string.amoled_mode_requires_dark
            ),
            enabled = isDark,
            isChecked = amoledModeValue,
            onCheck = { amoledModeValue = it },
            onClick = { detailKey = DETAIL_AMOLED },
            screenPosition = Position.Bottom
        )

        val effectsHeaderTitle = stringResource(Res.string.settings_visual_effects_header)
        val effectsHeader = remember(effectsHeaderTitle) {
            SettingsEntity.Header(title = effectsHeaderTitle)
        }
        val allowBlurPref = rememberSwitchPreference(
            allowBlur,
            state.supportsBlur,
            title = stringResource(Res.string.fancy_blur),
            summary = stringResource(
                if (state.supportsBlur) Res.string.fancy_blur_summary
                else Res.string.fancy_blur_requires_android_12
            ),
            isChecked = allowBlur,
            onCheck = { allowBlur = it },
            onClick = { detailKey = DETAIL_BLUR },
            enabled = state.supportsBlur,
            screenPosition = Position.Top
        )
        val autoContrastPref = rememberSwitchPreference(
            autoContrast,
            title = stringResource(Res.string.auto_contrast),
            summary = stringResource(Res.string.auto_contrast_summary),
            isChecked = autoContrast,
            onCheck = { autoContrast = it },
            onClick = { detailKey = DETAIL_AUTO_CONTRAST },
            screenPosition = Position.Middle
        )
        val sharedElementsPref = rememberSwitchPreference(
            sharedElements,
            title = stringResource(Res.string.shared_elements),
            summary = stringResource(Res.string.shared_elements_summary),
            isChecked = sharedElements,
            onCheck = { sharedElements = it },
            onClick = { detailKey = DETAIL_SHARED },
            screenPosition = Position.Bottom
        )

        val fontHeaderTitle = stringResource(Res.string.settings_font_header)
        val fontHeader = remember(fontHeaderTitle) {
            SettingsEntity.Header(title = fontHeaderTitle)
        }
        val useSystemFontPref = rememberSwitchPreference(
            useSystemFont,
            title = stringResource(Res.string.use_system_font_title),
            summary = stringResource(Res.string.use_system_font_summary),
            isChecked = useSystemFont,
            onCheck = { useSystemFont = it },
            onClick = { detailKey = DETAIL_SYSTEM_FONT },
            screenPosition = Position.Alone
        )
        val mapAppearanceLabel = when (mapAppearance) {
            MapAppearance.SYSTEM -> stringResource(Res.string.map_appearance_system)
            MapAppearance.LIGHT -> stringResource(Res.string.map_appearance_light)
            MapAppearance.DARK -> stringResource(Res.string.map_appearance_dark)
        }
        val mapAppearancePref = rememberPreference(
            mapAppearance,
            title = stringResource(Res.string.map_appearance_title),
            summary = mapAppearanceLabel,
            onClick = { detailKey = DETAIL_MAP_APPEARANCE },
            screenPosition = if (state.showManualCartoKey) Position.Top else Position.Alone
        )
        val cartoKeyPref = rememberPreference(
            userCartoKey,
            title = stringResource(Res.string.carto_basemap_key_title),
            summary = stringResource(
                if (userCartoKey.isBlank()) Res.string.carto_basemap_key_not_configured
                else Res.string.carto_basemap_key_configured
            ),
            onClick = {
                cartoKeyDraft = userCartoKey
                showCartoKeyDialog = true
            },
            screenPosition = Position.Bottom
        )

        val swatchesContent: @Composable () -> Unit = {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { fullWidth -> direction * fullWidth } + fadeIn())
                        .togetherWith(slideOutHorizontally { fullWidth -> -direction * fullWidth } + fadeOut())
                        .using(SizeTransform(clip = false))
                },
                label = "swatchesAnimation"
            ) { tab ->
                if (tab == 0) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item(key = "system") {
                            SystemColorOption(
                                isSelected = themeColorSeed == THEME_SEED_SYSTEM,
                                onClick = { themeColorSeed = THEME_SEED_SYSTEM }
                            )
                        }
                        items(state.wallpaperPalettes, key = { it.hexKey }) { palette ->
                            val isSelected = themeColorSeed == palette.hexKey
                            val scheme = remember(palette.seedArgb, isDark, amoledModeValue) {
                                colorSchemeFromSeed(palette.seedArgb, isDark, amoledModeValue)
                            }
                            ColorCircleItem(
                                scheme = scheme,
                                isSelected = isSelected,
                                onClick = { themeColorSeed = palette.hexKey }
                            )
                        }
                        item(key = "neutral_divider") {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                        item(key = "neutral") {
                            val neutralScheme = remember(isDark, amoledModeValue) {
                                neutralColorScheme(isDark, amoledModeValue)
                            }
                            ColorCircleItem(
                                scheme = neutralScheme,
                                isSelected = themeColorSeed == THEME_SEED_NEUTRAL,
                                onClick = { themeColorSeed = THEME_SEED_NEUTRAL }
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(presetPalettes, key = { it.hexKey }) { palette ->
                            val isSelected = themeColorSeed == palette.hexKey
                            val scheme = remember(palette.seedArgb, isDark, amoledModeValue) {
                                colorSchemeFromSeed(palette.seedArgb, isDark, amoledModeValue)
                            }
                            ColorCircleItem(
                                scheme = scheme,
                                isSelected = isSelected,
                                onClick = { themeColorSeed = palette.hexKey }
                            )
                        }
                    }
                }
            }
        }

        val tabsContent: @Composable () -> Unit = {
            Surface(
                modifier = Modifier.padding(horizontal = 24.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PillTab(
                        selected = selectedTab == 0,
                        text = stringResource(Res.string.color_palette_wallpaper_colors),
                        modifier = Modifier.focusRequester(initialFocusRequester),
                        onClick = { selectedTab = 0 }
                    )
                    PillTab(
                        selected = selectedTab == 1,
                        text = stringResource(Res.string.color_palette_other_colors),
                        onClick = { selectedTab = 1 }
                    )
                }
            }
        }

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Left: Phone preview
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PhonePreview(
                        colorScheme = previewScheme,
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .aspectRatio(0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Right: Controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(contentScrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(Res.string.color_palette_preview_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    swatchesContent()

                    Spacer(modifier = Modifier.height(16.dp))

                    tabsContent()

                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsItem(item = followSystemPref)
                    SettingsItem(item = darkModePref)
                    SettingsItem(item = amoledModePref)

                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsItem(item = effectsHeader)
                    SettingsItem(item = allowBlurPref)
                    SettingsItem(item = autoContrastPref)
                    SettingsItem(item = sharedElementsPref)

                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsItem(item = fontHeader)
                    SettingsItem(item = useSystemFontPref)

                    if (state.mapsEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsItem(item = SettingsEntity.Header(title = stringResource(Res.string.map_appearance_header)))
                        SettingsItem(item = mapAppearancePref)
                        if (state.showManualCartoKey) SettingsItem(item = cartoKeyPref)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(contentScrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                PhonePreview(
                    colorScheme = previewScheme,
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .aspectRatio(0.5f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(Res.string.color_palette_preview_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                swatchesContent()

                Spacer(modifier = Modifier.height(16.dp))

                tabsContent()

                Spacer(modifier = Modifier.height(24.dp))

                SettingsItem(item = followSystemPref)
                SettingsItem(item = darkModePref)
                SettingsItem(item = amoledModePref)

                Spacer(modifier = Modifier.height(16.dp))
                SettingsItem(item = effectsHeader)
                SettingsItem(item = allowBlurPref)
                SettingsItem(item = autoContrastPref)
                SettingsItem(item = sharedElementsPref)

                Spacer(modifier = Modifier.height(16.dp))
                SettingsItem(item = fontHeader)
                SettingsItem(item = useSystemFontPref)

                if (state.mapsEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsItem(item = SettingsEntity.Header(title = stringResource(Res.string.map_appearance_header)))
                    SettingsItem(item = mapAppearancePref)
                    if (state.showManualCartoKey) SettingsItem(item = cartoKeyPref)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (state.showManualCartoKey && showCartoKeyDialog) {
        AlertDialog(
            onDismissRequest = {
                cartoKeyDraft = ""
                showCartoKeyDialog = false
            },
            title = { Text(stringResource(Res.string.carto_basemap_key_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(Res.string.carto_basemap_key_description),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = cartoKeyDraft,
                        onValueChange = { cartoKeyDraft = it },
                        label = { Text(stringResource(Res.string.carto_basemap_key_field)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        userCartoKey = cartoKeyDraft.trim()
                        cartoKeyDraft = ""
                        showCartoKeyDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        cartoKeyDraft = ""
                        showCartoKeyDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SystemColorOption(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberSettingsFocusState()
    Box(
        modifier = modifier
            .settingsFocusTarget(focusState)
            .size(56.dp)
            .clip(CircleShape)
            .then(
                if (isSelected || focusState.hasFocus) Modifier.border(
                    3.dp, MaterialTheme.colorScheme.primary, CircleShape
                ) else Modifier
            )
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ColorCircleItem(
    scheme: ColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val focusState = rememberSettingsFocusState()
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected || focusState.hasFocus) 3.dp else 0.dp,
        label = "borderWidth"
    )
    val borderColor by animateColorAsState(
        targetValue = if (focusState.hasFocus) MaterialTheme.colorScheme.primary else if (isSelected) scheme.primary else Color.Transparent,
        label = "borderColor"
    )

    Box(
        modifier = Modifier
            .settingsFocusTarget(focusState)
            .size(56.dp)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape)
            .padding(borderWidth)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.matchParentSize()) {
            Row(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(scheme.primary)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(scheme.secondary)
                )
            }
            Row(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(scheme.tertiary)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(scheme.primaryContainer)
                )
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PhonePreview(
    colorScheme: ColorScheme?,
    modifier: Modifier = Modifier,
) {
    val currentScheme = MaterialTheme.colorScheme
    val scheme = colorScheme ?: currentScheme

    MaterialTheme(colorScheme = scheme) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            PortraitPreviewContent()
        }
    }
}

@Composable
private fun PortraitPreviewContent() {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "12:30",
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .size(width = 10.dp, height = 6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            )
        }

        // Search bar row with favorites + settings buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Search bar
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = stringResource(Res.string.search),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Favorites button
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryFixed
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryFixed,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            // Settings button
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.tertiaryFixed
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryFixed,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // "Today" header
        Text(
            text = stringResource(Res.string.header_today),
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )

        // Photo grid (4 columns)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom GalleryNavBar
        Surface(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .height(32.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timeline (selected)
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Photo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(12.dp)
                    )
                }
                // Albums
                Icon(
                    imageVector = com.dot.gallery.ui.core.Icons.Albums,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                // Library
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Home indicator
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(42.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun PillTab(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberSettingsFocusState()
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "pillTabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "pillTabText"
    )
    Surface(
        modifier = modifier
            .settingsFocusTarget(focusState)
            .clip(CircleShape)
            .border(
                width = if (focusState.hasFocus) 2.dp else 0.dp,
                color = if (focusState.hasFocus) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = backgroundColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = textColor
        )
    }
}

@Composable
private fun DarkModePreview(isChecked: Boolean) {
    val bg by animateColorAsState(if (isChecked) Color(0xFF121212) else Color(0xFFFEFBFF), label = "bg")
    val surface by animateColorAsState(if (isChecked) Color(0xFF2B2B2B) else Color(0xFFE8DEE8), label = "sf")
    val content by animateColorAsState(if (isChecked) Color(0xFFDADADA) else Color(0xFF1C1C1C), label = "ct")
    MiniPhoneFrame(backgroundColor = bg, surfaceColor = surface, contentColor = content)
}

@Composable
private fun AmoledPreview(isChecked: Boolean) {
    val bg by animateColorAsState(if (isChecked) Color.Black else Color(0xFF121212), label = "bg")
    val surface by animateColorAsState(if (isChecked) Color(0xFF0D0D0D) else Color(0xFF2B2B2B), label = "sf")
    MiniPhoneFrame(backgroundColor = bg, surfaceColor = surface, contentColor = Color(0xFFDADADA))
}

@Composable
private fun BlurPreview(isChecked: Boolean) {
    val backgroundColor = Color(0xFF121212)
    val contentColor = Color(0xFFDADADA)
    val surfaceColor = Color(0xFF2B2B2B)
    val barAlpha by animateFloatAsState(if (isChecked) 0.55f else 1f, label = "barAlpha")
    val gridColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
    )
    Box(
        modifier = Modifier
            .padding(24.dp)
            .size(width = 120.dp, height = 200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        // Media grid content (extends full height, behind bars)
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(5) { row ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(3) { col ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(gridColors[(row + col) % gridColors.size])
                        )
                    }
                }
            }
        }
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(surfaceColor.copy(alpha = barAlpha))
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .size(30.dp, 5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(contentColor.copy(alpha = 0.25f))
            )
        }
        // Bottom bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(20.dp)
                .background(surfaceColor.copy(alpha = barAlpha))
        )
    }
}

@Composable
private fun MiniPhoneFrame(
    backgroundColor: Color,
    surfaceColor: Color,
    contentColor: Color,
) {
    Box(
        modifier = Modifier
            .padding(24.dp)
            .size(width = 120.dp, height = 200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(24.dp).background(surfaceColor))
            Column(Modifier.weight(1f).padding(6.dp), Arrangement.spacedBy(3.dp)) {
                Box(Modifier.size(40.dp, 5.dp).clip(RoundedCornerShape(2.dp)).background(contentColor.copy(alpha = 0.25f)))
                repeat(3) {
                    Row(Modifier.weight(1f), Arrangement.spacedBy(3.dp)) {
                        repeat(3) {
                            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(surfaceColor))
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(20.dp).background(surfaceColor))
        }
    }
}
