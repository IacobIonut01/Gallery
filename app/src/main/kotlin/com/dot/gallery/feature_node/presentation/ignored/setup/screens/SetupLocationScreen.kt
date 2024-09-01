package com.dot.gallery.feature_node.presentation.ignored.setup.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.core.presentation.components.SetupWizard
import com.dot.gallery.feature_node.domain.model.IgnoredAlbum
import com.dot.gallery.feature_node.presentation.ignored.setup.IgnoredType

@Composable
fun SetupLocationScreen(
    onGoBack: () -> Unit,
    onNext: () -> Unit,
    isError: Boolean,
    initialLocation: Int,
    initialType: IgnoredType,
    onLocationChanged: (Int) -> Unit,
    onTypeChanged: (IgnoredType) -> Unit
) {
    var location by remember { mutableIntStateOf(initialLocation) }
    var type by remember { mutableStateOf(initialType) }
    LaunchedEffect(location) {
        onLocationChanged(location)
    }

    LaunchedEffect(type) {
        onTypeChanged(type)
    }

    SetupWizard(
        title = stringResource(R.string.setup_location_title),
        subtitle = stringResource(R.string.setup_location_subtitle),
        icon = Icons.Outlined.Settings,
        contentPadding = 0.dp,
        bottomBar = {
            OutlinedButton(
                onClick = onGoBack
            ) {
                Text(text = stringResource(id = R.string.go_back))
            }

            Button(
                onClick = onNext,
                enabled = !isError
            ) {
                Text(text = stringResource(R.string.continue_string))
            }
        },
        content = {
            Text(
                buildAnnotatedString {
                    withStyle(
                        style = MaterialTheme.typography.bodyLarge.toSpanStyle()
                    ) {
                        append(stringResource(R.string.setup_location_location_title))
                    }
                    appendLine()
                    withStyle(
                        style = MaterialTheme.typography.bodySmall.toSpanStyle()
                    ) {
                        append(stringResource(R.string.setup_location_location_subtitle))
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            val options = mapOf(
                stringResource(R.string.setup_location_options_albums) to IgnoredAlbum.ALBUMS_ONLY,
                stringResource(R.string.setup_location_options_timeline) to IgnoredAlbum.TIMELINE_ONLY,
                stringResource(R.string.setup_location_options_both) to IgnoredAlbum.ALBUMS_AND_TIMELINE,
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ){
                options.onEachIndexed { index, (option, optLocation) ->
                    SegmentedButton(
                        selected = location == options[option],
                        onClick = { location = optLocation },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) {
                        Text(text = option)
                    }
                }
            }


            Spacer(modifier = Modifier.height(32.dp))

            Text(
                buildAnnotatedString {
                    withStyle(
                        style = MaterialTheme.typography.bodyLarge.toSpanStyle()
                    ) {
                        append(stringResource(R.string.setup_location_type_title))
                    }
                    appendLine()
                    withStyle(
                        style = MaterialTheme.typography.bodySmall.toSpanStyle()
                    ) {
                        append(stringResource(R.string.setup_location_type_subtitle))
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            val types = mapOf(
                stringResource(R.string.setup_location_types_selection) to IgnoredType.SELECTION::class,
                stringResource(R.string.setup_location_types_regex) to IgnoredType.REGEX::class,
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                types.onEachIndexed { index, (title, titleType) ->
                    SegmentedButton(
                        selected = titleType.isInstance(type),
                        onClick = { type = when (titleType) {
                            IgnoredType.SELECTION::class -> IgnoredType.SELECTION(null)
                            IgnoredType.REGEX::class -> IgnoredType.REGEX("")
                            else -> IgnoredType.SELECTION(null)
                        } },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size)
                    ) {
                        Text(text = title)
                    }
                }
            }
        }
    )
}