package com.dot.gallery.feature_node.presentation.edit.components.filters

import android.media.effect.EffectFactory
import androidx.annotation.FloatRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.dot.gallery.feature_node.presentation.edit.components.utils.CustomEffect
import com.dot.gallery.feature_node.presentation.edit.components.utils.PhotoFilter
import com.dot.gallery.feature_node.presentation.edit.components.views.ImageFilterView

@Composable
fun TextureComponent(
    filterState: FilterState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var currentBitmap by remember(filterState) { mutableStateOf(filterState.bitmap) }
    val filterView = remember(context) { ImageFilterView(context) }

    LaunchedEffect(filterState) {
        filterState.setFilterView(filterView)
    }

    LaunchedEffect(currentBitmap) {
        if (currentBitmap != filterState.bitmap) {
            currentBitmap = filterState.bitmap
            filterView.setSourceBitmap(currentBitmap.asAndroidBitmap())
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { filterView }
    )
}

fun brightnessEffect(@FloatRange(from = 0.0, to = 2.0) value: Float = 1.0f): CustomEffect {
    return CustomEffect.Builder(EffectFactory.EFFECT_BRIGHTNESS)
        .setParameter("brightness", value)
        .build()
}

fun contrastEffect(@FloatRange(from = 1.0, to = 2.0) value: Float = 1.0f): CustomEffect {
    return CustomEffect.Builder(EffectFactory.EFFECT_CONTRAST)
        .setParameter("contrast", value)
        .build()
}

fun temperatureEffect(@FloatRange(from = 0.0, to = 1.0) value: Float = 0.5f): CustomEffect {
    return CustomEffect.Builder(EffectFactory.EFFECT_CONTRAST)
        .setParameter("contrast", value)
        .build()
}

class FilterState(val bitmap: ImageBitmap) {

    private var filterView: ImageFilterView? = null

    var currentFilter: MutableState<PhotoFilter> = mutableStateOf(filterView?.currentFilter ?: PhotoFilter.NONE)
        set(value) {
            filterView?.setFilterEffect(value.value)
            _hasFilterApplied.value = filterView?.hasFilterApplied ?: false
            field = value
        }

    var currentCustomFilter: MutableState<CustomEffect?> = mutableStateOf(filterView?.currentCustomFilter)
        set(value) {
            filterView?.setFilterEffect(value.value)
            _hasFilterApplied.value = filterView?.hasFilterApplied ?: false
            field = value
        }

    private val _hasFilterApplied: MutableState<Boolean> = mutableStateOf(filterView?.hasFilterApplied ?: false)
    val hasFilterApplied: State<Boolean> = _hasFilterApplied

    internal fun setFilterView(filterView: ImageFilterView) {
        this.filterView = filterView
    }

}

@Composable
fun rememberFilterState(key: Any? = Unit, sourceImage: ImageBitmap) = remember(key) {
    FilterState(sourceImage)
}
