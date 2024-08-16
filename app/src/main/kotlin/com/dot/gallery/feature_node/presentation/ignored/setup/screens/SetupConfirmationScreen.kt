package com.dot.gallery.feature_node.presentation.ignored.setup.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.dot.gallery.R
import com.dot.gallery.core.presentation.components.SetupWizard
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.IgnoredAlbum
import com.dot.gallery.feature_node.presentation.ignored.setup.IgnoredType

@Composable
fun SetupConfirmationScreen(
    onGoBack: () -> Unit,
    onNext: () -> Unit,
    location: Int,
    type: IgnoredType,
    matchedAlbums: List<Album>,
) {
    SetupWizard(
        title = stringResource(R.string.setup_confirmation_title),
        subtitle = stringResource(R.string.setup_confirmation_subtitle),
        icon = Icons.Outlined.Checklist,
        bottomBar = {
            OutlinedButton(
                onClick = onGoBack
            ) {
                Text(text = stringResource(id = R.string.go_back))
            }

            Button(
                onClick = onNext,
                enabled = matchedAlbums.isNotEmpty()
            ) {
                Text(text = stringResource(R.string.apply))
            }
        },
        content = {
            val albumsString = stringResource(R.string.albums)
            val timelineString = stringResource(R.string.timeline)
            val bothString = stringResource(R.string.albums_and_timeline)
            ConfirmationBlock(
                title = stringResource(R.string.setup_confirmation_where),
                subtitle = remember(location) {
                    when (location) {
                        IgnoredAlbum.ALBUMS_ONLY -> albumsString
                        IgnoredAlbum.TIMELINE_ONLY -> timelineString
                        IgnoredAlbum.ALBUMS_AND_TIMELINE -> bothString
                        else -> "Unknown Error"
                    }
                }
            )

            ConfirmationBlock(
                title = stringResource(R.string.setup_confirmation_who),
                subtitle = remember(type) {
                    when (type) {
                        is IgnoredType.SELECTION -> type.selectedAlbum!!.label
                        is IgnoredType.REGEX -> type.regex
                    }
                }
            )

            ConfirmationBlock(
                title = stringResource(R.string.setup_confirmation_matched),
                subtitle = remember(matchedAlbums) {
                    matchedAlbums.joinToString("\n", limit = 10) { it.label }
                },
                extra = remember(matchedAlbums) {
                    if (matchedAlbums.size > 10) "+${matchedAlbums.size - 10}" else null
                }
            )
        }
    )
}

@Composable
private fun ConfirmationBlock(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    extra: String? = null,
) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = buildAnnotatedString {
            withStyle(
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                ).toSpanStyle()
            ) {
                appendLine(title)
            }

            withStyle(
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ).toSpanStyle()
            ) {
                appendLine(subtitle)
            }

            extra?.let {
                withStyle(
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    ).toSpanStyle()
                ) {
                    appendLine(it)
                }
            }
        }
    )
}
