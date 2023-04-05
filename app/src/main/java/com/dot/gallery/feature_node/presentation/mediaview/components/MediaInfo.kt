package com.dot.gallery.feature_node.presentation.mediaview.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.dot.gallery.R
import com.dot.gallery.core.Constants.EXIF_DATE_FORMAT
import com.dot.gallery.core.Constants.TAG
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.util.ExifMetadata
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.getExifInterface
import com.dot.gallery.ui.theme.Shapes
import java.io.File
import java.io.IOException
import java.math.RoundingMode
import java.text.DecimalFormat

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaInfoRow(
    modifier: Modifier = Modifier,
    label: String,
    content: String,
    icon: ImageVector,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.medium)
            .combinedClickable(
                onClick = { onClick?.let { it() } },
                onLongClick = {
                    if (onLongClick != null) onLongClick()
                    else {
                        clipboardManager.setText(AnnotatedString(content))
                    }
                }
            ),
        overlineContent = {
            Text(text = label)
        },
        headlineContent = {
            Text(
                text = content,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription
            )
        }
    )
}

data class InfoRow(
    val label: String,
    val content: String,
    val icon: ImageVector,
    val contentDescription: String? = null,
    val onClick: (() -> Unit)? = null,
    val onLongClick: (() -> Unit)? = null,
)

fun Media.retrieveMetadata(context: Context): List<InfoRow> {
    val infoList = ArrayList<InfoRow>()
    infoList.apply {
        add(
            InfoRow(
                icon = Icons.Outlined.Photo,
                label = context.getString(R.string.label),
                content = label
            )
        )
        add(
            InfoRow(
                icon = Icons.Outlined.Info,
                label = context.getString(R.string.path),
                content = path
            )
        )
        add(
            InfoRow(
                icon = Icons.Outlined.DateRange,
                label = context.getString(R.string.date),
                content = timestamp.getDate(EXIF_DATE_FORMAT)
            )
        )
    }
    try {
        val exifMetadata = ExifMetadata(uri.getExifInterface())
        infoList.apply {
            exifMetadata.modelName?.let {
                val focalLength = exifMetadata.focalLength
                val isoValue = exifMetadata.isoValue
                val stringBuilder = StringBuilder()
                stringBuilder.append("f/${exifMetadata.apertureValue}")
                if (focalLength != 0.0)
                    stringBuilder.append(" • ${focalLength}mm")
                if (isoValue != 0)
                    stringBuilder.append(context.getString(R.string.iso) + isoValue)
                add(
                    InfoRow(
                        icon = Icons.Outlined.Camera,
                        label = it,
                        content = stringBuilder.toString()
                    )
                )
            }
            if (!exifMetadata.imageWidth.isNullOrEmpty() && !exifMetadata.imageHeight.isNullOrEmpty()) {
                val width = exifMetadata.imageWidth
                val height = exifMetadata.imageHeight
                val roundingMP = DecimalFormat("#.#").apply {
                    roundingMode = RoundingMode.DOWN
                }
                val mpValue = roundingMP.format(width.toDouble() * height.toDouble() / 1024000.0)
                val roundingSize = DecimalFormat("#.##").apply {
                    roundingMode = RoundingMode.DOWN
                }
                var fileSize = File(path).length().toDouble() / 1024.0
                var fileSizeName = context.getString(R.string.kb)
                if (fileSize > 1024.0) {
                    fileSize /= 1024.0
                    fileSizeName = context.getString(R.string.mb)
                    if (fileSize > 1024.0) {
                        fileSize /= 1024.0
                        fileSizeName = context.getString(R.string.gb)
                    }
                }
                val contentString = StringBuilder()
                contentString.append("${roundingSize.format(fileSize)} $fileSizeName")
                if (mpValue > "0") contentString.append(" • $mpValue MP")
                if (width > "0" && height > "0") contentString.append(" • $width x $height")
                add(
                    InfoRow(
                        icon = Icons.Outlined.ImageSearch,
                        label = context.getString(R.string.metadata),
                        content = contentString.toString()
                    )
                )
            }
        }
    } catch (e: IOException) {
        Log.e(TAG, "ExifInterface ERROR\n" + e.printStackTrace())
    }

    return infoList
}