package com.dot.gallery.feature_node.presentation.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import com.dot.gallery.core.Constants
import com.dot.gallery.feature_node.domain.model.ImageFilter
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.gpuImage
import com.dot.gallery.feature_node.domain.model.mapToImageFilters
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import com.dot.gallery.feature_node.presentation.util.flipHorizontally
import com.dot.gallery.feature_node.presentation.util.flipVertically
import com.dot.gallery.feature_node.presentation.util.rotate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.co.cyberagent.android.gpuimage.GPUImage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import javax.inject.Inject

@HiltViewModel
class EditViewModel @Inject constructor(
    private val loader: ImageLoader,
    private val request: ImageRequest.Builder,
    private val mediaUseCases: MediaUseCases,
    @ApplicationContext
    private val applicationContext: Context
) : ViewModel() {

    private var _mediaRef = MutableStateFlow<Media?>(null)
    val mediaRef = _mediaRef.asStateFlow()
    private var _image = MutableStateFlow<Bitmap?>(null)
    val image = _image.asStateFlow()
    private var _filters = MutableStateFlow<List<ImageFilter>>(emptyList())
    val filters = _filters.asStateFlow()
    val currentFilter = mutableStateOf<ImageFilter?>(null)
    private var origImage: Bitmap? = null
    private var gpuImage: GPUImage? = null
    var currentUri: Uri = Uri.EMPTY
    var modifiedImages: SnapshotStateList<Bitmap> = mutableStateListOf()
    private var addedFilters = mutableStateListOf<ImageFilter>()
    var canRevert = mutableStateOf(modifiedImages.isNotEmpty())

    fun loadImage(uri: Uri) {
        currentUri = uri
        viewModelScope.launch(Dispatchers.IO) {
            mediaUseCases.getMediaListByUrisUseCase(listOf(uri), reviewMode = false).collectLatest {
                val data = it.data ?: emptyList()
                _mediaRef.emit(data.firstOrNull())
                loader.execute(
                    request.data(uri)
                        .apply { size(Size.ORIGINAL) }
                        .target { drawable ->
                            launch(Dispatchers.IO) {
                                val bitmap = drawable.toBitmapOrNull(config = Bitmap.Config.HARDWARE)
                                _image.emit(bitmap)
                                origImage = bitmap
                                bitmap?.let { image ->
                                    gpuImage = applicationContext.gpuImage(image.copy(Bitmap.Config.ARGB_8888, true))
                                    _filters.emit(
                                        gpuImage!!.mapToImageFilters()
                                    )
                                }
                            }
                        }
                        .build()
                )
            }
        }
    }

    fun applyFilter(imageFilter: ImageFilter) {
        viewModelScope.launch(Dispatchers.IO) {
            addedFilters.add(imageFilter)
            gpuImage?.let {
                currentFilter.value = imageFilter
                it.setFilter(imageFilter.filter)
                updateImage(it.bitmapWithFilterApplied, onImageUpdated = {})
            } ?: Log.e(Constants.TAG, "GPUImage is not initialized")
        }
    }

    fun revert() {
        viewModelScope.launch(Dispatchers.IO) {
            if (modifiedImages.isNotEmpty()) {
                _image.emit(modifiedImages.last())
                modifiedImages.removeLast()
            }
            if (addedFilters.isNotEmpty()) {
                addedFilters.removeLast()
                currentFilter.value = addedFilters.lastOrNull()
            }
            canRevert.value = modifiedImages.size > 0
        }
    }

    fun updateImage(bitmap: Bitmap, onImageUpdated: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            canRevert.value = true
            image.value?.let { modifiedImages.add(it) }
            _image.emit(bitmap)
            onImageUpdated()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private val imageDispatcher = newFixedThreadPoolContext(4, "imageDispatcher")

    fun flipVertically() {
        viewModelScope.launch(imageDispatcher) {
            _image.value?.let {
                _image.emit(it.flipVertically())
            }
        }
    }

    fun flipHorizontally() {
        viewModelScope.launch(imageDispatcher) {
            _image.value?.let {
                _image.emit(it.flipHorizontally())
            }
        }
    }

    fun setAngle(angle: Float) {
        viewModelScope.launch(imageDispatcher) {
            origImage?.let {
                _image.emit(it.rotate(angle))
            }
        }
    }


    fun addAngle(angle: Float) {
        viewModelScope.launch(imageDispatcher) {
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

    private val bitmapFormat: CompressFormat
        get() = when (mediaRef.value?.mimeType?.substringAfterLast("/")?.lowercase()) {
            "png" -> CompressFormat.PNG
            "jpeg", "jpg" -> CompressFormat.JPEG
            "webp" -> CompressFormat.WEBP_LOSSLESS
            else -> CompressFormat.PNG
        }

}