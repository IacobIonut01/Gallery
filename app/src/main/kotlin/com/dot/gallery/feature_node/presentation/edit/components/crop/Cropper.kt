package com.dot.gallery.feature_node.presentation.edit.components.crop

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smarttoolfactory.cropper.ImageCropper
import com.smarttoolfactory.cropper.model.AspectRatio
import com.smarttoolfactory.cropper.settings.CropDefaults
import com.smarttoolfactory.cropper.settings.CropProperties

@Composable
fun Cropper(
    modifier: Modifier = Modifier,
    bitmap: Bitmap,
    crop: Boolean,
    imageCropStarted: () -> Unit,
    imageCropFinished: (Bitmap) -> Unit,
    cropProperties: CropProperties,
) {
    Column {
        AnimatedContent(
            targetState = (cropProperties.aspectRatio != AspectRatio.Original) to bitmap,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = modifier
                .weight(1f)
                .fillMaxWidth(),
            label = "cropper",
        ) { (fixedAspectRatio, bitmap) ->
            val bmp = remember(bitmap) { bitmap.asImageBitmap() }
            ImageCropper(
                backgroundModifier = Modifier.transparencyChecker(),
                imageBitmap = bmp,
                contentDescription = null,
                cropProperties = cropProperties.copy(fixedAspectRatio = fixedAspectRatio),
                onCropStart = {
                    imageCropStarted()
                },
                crop = crop,
                cropStyle = CropDefaults.style(
                    overlayColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                cropRotation = 0f,
                onCropSuccess = { image ->
                    imageCropFinished(image.asAndroidBitmap())
                },
            )
        }
    }
}

@Composable
private fun Modifier.transparencyChecker(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    checkerWidth: Dp = 10.dp,
    checkerHeight: Dp = 10.dp
) = drawBehind {
    val width = this.size.width
    val height = this.size.height

    val checkerWidthPx = checkerWidth.toPx()
    val checkerHeightPx = checkerHeight.toPx()

    val horizontalSteps = (width / checkerWidthPx).toInt()
    val verticalSteps = (height / checkerHeightPx).toInt()

    for (y in 0..verticalSteps) {
        for (x in 0..horizontalSteps) {
            val isGrayTile = ((x + y) % 2 == 1)
            drawRect(
                color = if (isGrayTile) {
                    colorScheme.surfaceColorAtElevation(20.dp)
                } else colorScheme.surface,
                topLeft = Offset(x * checkerWidthPx, y * checkerHeightPx),
                size = Size(checkerWidthPx, checkerHeightPx)
            )
        }
    }
}