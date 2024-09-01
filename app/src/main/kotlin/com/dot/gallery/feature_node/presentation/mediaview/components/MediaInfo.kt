/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.dot.gallery.R
import com.dot.gallery.core.Constants.TAG
import com.dot.gallery.feature_node.domain.model.InfoRow
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.util.ExifMetadata
import com.dot.gallery.feature_node.presentation.util.formatMinSec
import com.dot.gallery.feature_node.presentation.util.formattedFileSize
import com.dot.gallery.ui.theme.Shapes
import java.io.File
import java.io.IOException

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaInfoRow(
    modifier: Modifier = Modifier,
    label: String,
    content: String,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
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
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        headlineContent = {
            Text(
                text = label,
                fontWeight = FontWeight.Bold
            )
        },
        supportingContent = {
            Text(text = content)
        },
        trailingContent = trailingContent
    )
}

fun Media.retrieveMetadata(context: Context, exifMetadata: ExifMetadata?, onLabelClick: () -> Unit): List<InfoRow> {
    val infoList = ArrayList<InfoRow>()
    if (trashed == 1) {
        infoList.apply {
            add(
                InfoRow(
                    icon = Icons.Outlined.Photo,
                    trailingIcon = Icons.Outlined.Edit,
                    label = context.getString(R.string.label),
                    onClick = onLabelClick,
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
        }
        return infoList
    }
    try {
        infoList.apply {
            if (exifMetadata != null && !exifMetadata.modelName.isNullOrEmpty()) {
                val aperture = exifMetadata.apertureValue
                val focalLength = exifMetadata.focalLength
                val isoValue = exifMetadata.isoValue
                val stringBuilder = StringBuilder()
                if (aperture != 0.0)
                    stringBuilder.append("f/$aperture")
                if (focalLength != 0.0)
                    stringBuilder.append(" • ${focalLength}mm")
                if (isoValue != 0)
                    stringBuilder.append(context.getString(R.string.iso) + isoValue)
                add(
                    InfoRow(
                        icon = Icons.Outlined.Camera,
                        label = "${exifMetadata.manufacturerName} ${exifMetadata.modelName}",
                        content = stringBuilder.toString()
                    )
                )
            }
            add(
                InfoRow(
                    icon = Icons.Outlined.Photo,
                    trailingIcon = Icons.Outlined.Edit,
                    label = context.getString(R.string.label),
                    onClick = onLabelClick,
                    content = label
                )
            )
            val formattedFileSize = File(path).formattedFileSize(context)
            val contentString = StringBuilder()
            contentString.append(formattedFileSize)
            if (mimeType.contains("video")) {
                contentString.append(" • ${duration.formatMinSec()}")
            } else if (exifMetadata != null && exifMetadata.imageWidth != 0 && exifMetadata.imageHeight != 0) {
                val width = exifMetadata.imageWidth
                val height = exifMetadata.imageHeight
                val imageMp = exifMetadata.imageMp
                if (imageMp > "0") contentString.append(" • $imageMp MP")
                if (width > 0 && height > 0) contentString.append(" • $width x $height")
            }
            val icon = if (mimeType.contains("video")) Icons.Outlined.VideoFile else Icons.Outlined.ImageSearch
            add(
                InfoRow(
                    icon = icon,
                    label = context.getString(R.string.metadata),
                    content = contentString.toString()
                )
            )


            add(
                InfoRow(
                    icon = Icons.Outlined.Info,
                    label = context.getString(R.string.path),
                    content = path.substringBeforeLast("/")
                )
            )
        }
    } catch (e: IOException) {
        Log.e(TAG, "ExifInterface ERROR\n" + e.printStackTrace())
    }

    return infoList
}