package com.dot.gallery.feature_node.presentation.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Environment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.feature_node.domain.model.ImageFilter
import com.dot.gallery.feature_node.domain.model.ImageModification
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import com.dot.gallery.feature_node.presentation.edit.components.adjustments.Adjustment
import com.dot.gallery.feature_node.presentation.edit.components.adjustments.AdjustmentFilter
import com.dot.gallery.feature_node.presentation.util.flipHorizontally
import com.dot.gallery.feature_node.presentation.util.mapToImageFilters
import com.dot.gallery.feature_node.presentation.util.rotate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases,
    @ApplicationContext
    private val applicationContext: Context
) : ViewModel() {

    private var filterJob: Job? = null
    private var adjustmentJob: Job? = null
    private var cropperJob: Job? = null
    private var updateJob: Job? = null

    private var gpuImage: GPUImage? = null
    private var origImage: Bitmap? = null
    private var baseImage: Bitmap? = null
    var currentUri: Uri = Uri.EMPTY

    private val _mediaRef = MutableStateFlow<Media?>(null)
    val mediaRef = _mediaRef.asStateFlow()
    private val _image = MutableStateFlow<Bitmap?>(null)
    val image = _image.asStateFlow()
    private val _filters = MutableStateFlow<List<ImageFilter>>(emptyList())
    val filters = _filters.asStateFlow()
    val currentFilter = mutableStateOf<ImageFilter?>(null)

    private var modifiedImages = mutableStateListOf<Bitmap>()
    val modifications = mutableStateListOf<ImageModification>()
    var canRevert = mutableStateOf(modifiedImages.isNotEmpty())

    fun addFilter(filter: ImageFilter) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.IO) {
            if (baseImage == null) baseImage = image.value
            addModification(ImageModification(filter = filter))
        }
    }

    fun addAdjustment(isScrolling: Boolean, adjustment: Pair<AdjustmentFilter, Float>) {
        adjustmentJob?.cancel()
        adjustmentJob = viewModelScope.launch(Dispatchers.IO) {
            if (baseImage == null) baseImage = image.value
            if (!isScrolling) {
                if (adjustment.second == adjustment.first.defaultValue) {
                    modifications.firstOrNull { it.adjustment?.first == adjustment.first }?.let {
                        println("Removed: ${adjustment.first.tag}")
                        removeModification(it)
                    }
                } else {
                    println("Added: ${adjustment.first.tag} with ${adjustment.second}")
                    addModification(ImageModification(adjustment = adjustment))
                }
            }
        }
    }

    fun addCroppedImage(croppedImage: Bitmap) {
        cropperJob?.cancel()
        cropperJob = viewModelScope.launch(Dispatchers.IO) {
            baseImage = null
            addModification(ImageModification(croppedImage = croppedImage), updateFilters = true)
        }
    }

    fun loadImage(uri: Uri) {
        currentUri = uri
        viewModelScope.launch(Dispatchers.IO) {
            mediaUseCases.getMediaListByUrisUseCase(listOf(uri), reviewMode = false)
                .collectLatest {
                    val data = it.data ?: emptyList()
                    _image.emit(null)
                    origImage = null
                    gpuImage = null
                    /*loader.execute(
                        request.data(uri)
                            .apply { size(Size.ORIGINAL) }
                            .target { drawable ->
                                launch(Dispatchers.IO) {
                                    drawable.toBitmapOrNull()?.copy(Bitmap.Config.ARGB_8888, true)?.let { bitmap ->
                                        _mediaRef.emit(data.firstOrNull())
                                        _image.emit(bitmap)
                                        origImage = bitmap
                                        gpuImage = applicationContext.gpuImage(bitmap)
                                        _filters.emit(
                                            gpuImage?.mapToImageFilters() ?: emptyList()
                                        )
                                    }
                                }
                            }
                            .build()
                    )*/
                }
        }
    }

    fun revert() {
        viewModelScope.launch(Dispatchers.IO) {
            if (modifiedImages.isNotEmpty()) {
                modifications.removeLastOrNull()
                currentFilter.value = modifications.lastOrNull()?.filter
                updateImage(
                    modifiedImages.last(),
                    isRevertAction = true,
                    updateFilters = modifications.lastOrNull()?.croppedImage != null
                )
                modifiedImages.removeLastOrNull()
            }
            canRevert.value = modifiedImages.size > 0
        }
    }

    fun flipHorizontally() {
        viewModelScope.launch(Dispatchers.IO) {
            _image.value?.let {
                _image.emit(it.flipHorizontally())
            }
        }
    }

    /*fun setAngle(angle: Float) {
          viewModelScope.launch(Dispatchers.IO) {
              origImage?.let {
                  _image.emit(it.rotate(angle))
              }
          }
      }*/

    fun addAngle(angle: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            _image.value?.let {
                _image.emit(it.rotate(angle))
            }
        }
    }

    fun saveImage(
        image: ImageBitmap,
        asCopy: Boolean,
        onFinish: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val done = if (asCopy) {
                mediaUseCases.mediaHandleUseCase.saveImage(
                    bitmap = image.asAndroidBitmap(),
                    format = bitmapFormat,
                    relativePath = mediaRef.value?.relativePath
                        ?: (Environment.DIRECTORY_PICTURES + "/Edited"),
                    displayName = mediaRef.value?.label
                        ?: "Edited_Picture_${System.currentTimeMillis()}",
                    mimeType = mediaRef.value?.mimeType ?: "image/png"
                ) != null
            } else {
                mediaUseCases.mediaHandleUseCase.overrideImage(
                    uri = mediaRef.value!!.uri,
                    bitmap = image.asAndroidBitmap(),
                    format = bitmapFormat,
                    relativePath = mediaRef.value?.relativePath
                        ?: (Environment.DIRECTORY_PICTURES + "/Edited"),
                    displayName = mediaRef.value?.label
                        ?: "Edited_Picture_${System.currentTimeMillis()}",
                    mimeType = mediaRef.value?.mimeType ?: "image/png"
                )
            }
            onFinish(done)
        }
    }

    private fun addModification(modification: ImageModification, updateFilters: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (modifications.any { it.adjustment != null && it.adjustment.first == modification.adjustment?.first }) {
                modifications.replaceAll { mod ->
                    if (mod.adjustment?.first == modification.adjustment?.first) {
                        modification
                    } else {
                        mod
                    }
                }
            } else {
                modifications.add(modification)
            }
            modification.croppedImage?.let {
                updateImage(it, updateFilters = updateFilters)
            } ?: gpuImage?.let {
                if (baseImage != null) {
                    it.setImage(baseImage)
                    if (modification.adjustment != null) {
                        it.setFilter(modification.adjustment.first.filter(modification.adjustment.second))
                        updateImage(it.bitmapWithFilterApplied, updateFilters = true)
                    } else if (modification.filter != null) {
                        it.setFilter(modification.filter.filter)
                        currentFilter.value = modification.filter
                        updateImage(it.bitmapWithFilterApplied, updateFilters = false)
                    }
                } else {
                    throw IllegalStateException("Base image is null")
                }
            }
        }
    }

    private fun removeModification(modification: ImageModification) {
        viewModelScope.launch(Dispatchers.IO) {
            modifications.remove(modification)
            modifications.lastOrNull()?.let { mod ->
                mod.croppedImage?.let {
                    updateImage(it, updateFilters = true)
                } ?: gpuImage?.let {
                    if (baseImage != null) {
                        it.setImage(baseImage)
                        if (modification.adjustment != null) {
                            it.setFilter(modification.adjustment.first.filter(modification.adjustment.second))
                            updateImage(it.bitmapWithFilterApplied, updateFilters = true)
                        } else if (modification.filter != null) {
                            it.setFilter(modification.filter.filter)
                            currentFilter.value = modification.filter
                            updateImage(it.bitmapWithFilterApplied, updateFilters = false)
                        }
                    } else {
                        throw IllegalStateException("Base image is null")
                    }
                }
            }
        }
    }

    private fun updateImage(
        bitmap: Bitmap,
        isRevertAction: Boolean = false,
        updateFilters: Boolean = true,
        onImageUpdated: () -> Unit = {}
    ) {
        updateJob?.cancel()
        updateJob = viewModelScope.launch(Dispatchers.IO) {
            if (!isRevertAction) {
                canRevert.value = true
                image.value?.let { modifiedImages.add(it) }
            }
            _image.emit(bitmap)
            if (updateFilters) {
                gpuImage?.setImage(bitmap)
                _filters.emit(
                    gpuImage!!.mapToImageFilters()
                )
            }
            onImageUpdated()
        }
    }

    private val bitmapFormat: CompressFormat
        get() = when (mediaRef.value?.mimeType?.substringAfterLast("/")?.lowercase()) {
            "png" -> CompressFormat.PNG
            "jpeg", "jpg" -> CompressFormat.JPEG
            "webp" -> CompressFormat.WEBP_LOSSLESS
            else -> CompressFormat.PNG
        }

    companion object {
        val adjustmentFilters = listOf(
            AdjustmentFilter(
                tag = Adjustment.BRIGHTNESS,
                name = "Brightness",
                icon = Icons.Default.Exposure,
                minValue = -1f,
                maxValue = 1f,
                defaultValue = 0f,
                filter = { value ->
                    GPUImageBrightnessFilter(value)
                }
            ),
            AdjustmentFilter(
                tag = Adjustment.CONTRAST,
                name = "Contrast",
                icon = Icons.Default.Contrast,
                minValue = 0f,
                maxValue = 4f,
                defaultValue = 1f,
                filter = { value ->
                    GPUImageContrastFilter(value)
                }
            ),
            AdjustmentFilter(
                tag = Adjustment.SATURATION,
                name = "Saturation",
                icon = Icons.Default.InvertColors,
                minValue = 0f,
                maxValue = 2f,
                defaultValue = 1f,
                filter = { value ->
                    GPUImageSaturationFilter(value)
                }
            ),
        )
    }

}