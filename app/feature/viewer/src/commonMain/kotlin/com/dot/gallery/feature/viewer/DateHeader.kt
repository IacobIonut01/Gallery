package com.dot.gallery.feature.viewer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dot.gallery.domain.model.MediaDateCaption
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.image_add_description


@Composable
fun DateHeader(
    modifier: Modifier = Modifier,
    mediaDateCaption: MediaDateCaption
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ).toSpanStyle()
            ) {
                appendLine(mediaDateCaption.date)
            }
            withStyle(
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ).toSpanStyle()
            ) {
                append(
                    mediaDateCaption.description.ifEmpty {
                        stringResource(Res.string.image_add_description)
                    }
                )
            }
        },
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(top = 8.dp)
    )
}
