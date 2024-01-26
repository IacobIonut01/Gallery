package com.dot.gallery.feature_node.domain.model

import android.content.Context
import android.graphics.Bitmap
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorMatrixFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageRGBFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter

data class ImageFilter(
    val name: String = "",
    val filter: GPUImageFilter,
    val filterPreview: Bitmap
)

fun Context.gpuImage(bitmap: Bitmap) = GPUImage(this).apply { setImage(bitmap) }

fun GPUImage.mapToImageFilters(): List<ImageFilter> {
    val gpuImage = this
    val imgFilters: ArrayList<ImageFilter> = ArrayList()

    //region:: Filters
    // Normal
    GPUImageFilter().also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "None",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Retro
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.2f, 0.0f,
            0.1f, 0.1f, 1.0f, 0.0f,
            1.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Retro",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Just
    GPUImageColorMatrixFilter(
        0.9f,
        floatArrayOf(
            0.4f, 0.6f, 0.5f, 0.0f,
            0.0f, 0.4f, 1.0f, 0.0f,
            0.05f, 0.1f, 0.4f, 0.4f,
            1.0f, 1.0f, 1.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Just",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Hume
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            1.25f, 0.0f, 0.2f, 0.0f,
            0.0f, 1.0f, 0.2f, 0.0f,
            0.0f, 0.3f, 1.0f, 0.3f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Hume",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Desert
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0.6f, 0.4f, 0.2f, 0.05f,
            0.0f, 0.8f, 0.3f, 0.05f,
            0.3f, 0.3f, 0.5f, 0.08f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Desert",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Old Times
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            1.0f, 0.05f, 0.0f, 0.0f,
            -0.2f, 1.1f, -0.2f, 0.11f,
            0.2f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Old Times",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Limo
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            1.0f, 0.0f, 0.08f, 0.0f,
            0.4f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.1f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Limo",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Sepia
    GPUImageSepiaToneFilter().also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Sepia",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Solar
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            1.5f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Solar",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Wole
    GPUImageSaturationFilter(2.0f).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Wole",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Neutron
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0f, 1f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0.6f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Neutron",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Bright
    GPUImageRGBFilter(1.1f, 1.3f, 1.6f).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Bright",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Milk
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.64f, 0.5f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Milk",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // BW
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "BW",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Clue
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Clue",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Muli
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Muli",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Aero
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0f, 0f, 1f, 0f,
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Aero",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Classic
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0.763f, 0.0f, 0.2062f, 0f,
            0.0f, 0.9416f, 0.0f, 0f,
            0.1623f, 0.2614f, 0.8052f, 0f,
            0f, 0f, 0f, 1f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Classic",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Atom
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0.5162f, 0.3799f, 0.3247f, 0f,
            0.039f, 1.0f, 0f, 0f,
            -0.4773f, 0.461f, 1.0f, 0f,
            0f, 0f, 0f, 1f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Atom",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Mars
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0.0f, 0.0f, 0.5183f, 0.3183f,
            0.0f, 0.5497f, 0.5416f, 0f,
            0.5237f, 0.5269f, 0.0f, 0f,
            0f, 0f, 0f, 1f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Mars",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Yeli
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            1.0f, -0.3831f, 0.3883f, 0.0f,
            0.0f, 1.0f, 0.2f, 0f,
            -0.1961f, 0.0f, 1.0f, 0f,
            0f, 0f, 0f, 1f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Yeli",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }
    //endregion
    return imgFilters
}