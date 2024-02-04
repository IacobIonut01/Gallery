package com.dot.gallery.feature_node.presentation.edit.components.crop

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.smarttoolfactory.cropper.ImageCropper
import com.smarttoolfactory.cropper.model.AspectRatio
import com.smarttoolfactory.cropper.settings.CropDefaults
import com.smarttoolfactory.cropper.settings.CropProperties

@Composable
fun Cropper(
    modifier: Modifier = Modifier,
    bitmap: Bitmap,
    colorFilter: ColorFilter? = null,
    cropEnabled: Boolean,
    crop: Boolean,
    onCropStart: () -> Unit,
    onCropSuccess: (Bitmap) -> Unit,
    cropProperties: CropProperties,
) {
    Column {
        AnimatedContent(
            targetState = (cropProperties.aspectRatio != AspectRatio.Original),
            transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
            modifier = modifier
                .weight(1f)
                .fillMaxWidth(),
            label = "cropper",
        ) { fixedAspectRatio ->
            val bmp = remember(bitmap) { bitmap.asImageBitmap() }
            ImageCropper(
                imageBitmap = bmp,
                contentDescription = null,
                cropEnabled = cropEnabled,
                colorFilter = colorFilter,
                cropProperties = cropProperties.copy(fixedAspectRatio = fixedAspectRatio),
                onCropStart = {
                    onCropStart()
                },
                crop = crop,
                cropStyle = CropDefaults.style(
                    overlayColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                onCropSuccess = { image ->
                    onCropSuccess(image.asAndroidBitmap())
                },
            )
        }
    }
}
