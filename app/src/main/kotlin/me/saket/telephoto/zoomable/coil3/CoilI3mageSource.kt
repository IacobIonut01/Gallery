@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

package me.saket.telephoto.zoomable.coil3

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import coil3.Image
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asDrawable
import coil3.compose.LocalPlatformContext
import coil3.decode.DataSource
import coil3.gif.AnimatedImageDecoder
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.request.transitionFactory
import coil3.size.Dimension
import coil3.size.Precision
import coil3.size.SizeResolver
import coil3.svg.SvgDecoder
import coil3.transition.CrossfadeTransition
import com.dot.gallery.feature_node.presentation.util.JxlDecoder
import com.github.awxkee.avifcoil.decoder.HeifDecoder3
import com.google.accompanist.drawablepainter.DrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.saket.telephoto.subsamplingimage.ImageBitmapOptions
import me.saket.telephoto.subsamplingimage.SubSamplingImageSource
import me.saket.telephoto.zoomable.ZoomableImageSource
import me.saket.telephoto.zoomable.ZoomableImageSource.ResolveResult
import me.saket.telephoto.zoomable.coil3.Resolver.ImageSourceCreationResult.EligibleForSubSampling
import me.saket.telephoto.zoomable.coil3.Resolver.ImageSourceCreationResult.ImageDeletedOnlyFromDiskCache
import me.saket.telephoto.zoomable.internal.RememberWorker
import me.saket.telephoto.zoomable.internal.copy
import java.io.File
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import coil3.size.Size as CoilSize

@Composable
fun ZoomableImageSource.Companion.coil3(
  model: Any?,
  imageLoader: ImageLoader = LocalPlatformContext.current.imageLoader
): ZoomableImageSource {
  return remember(model, imageLoader) {
    Coil3ImageSource(model, imageLoader)
  }
}

internal class Coil3ImageSource(
  private val model: Any?,
  private val imageLoader: ImageLoader,
) : ZoomableImageSource {

  @Composable
  override fun resolve(canvasSize: Flow<Size>): ResolveResult {
    val context = LocalContext.current
    val resolver = remember(this) {
      Resolver(
        request = model as? ImageRequest
          ?: ImageRequest.Builder(context)
            .data(model)
            .build(),
        imageLoader = imageLoader,
        sizeResolver = { canvasSize.first().toCoilSize() }
      )
    }
    return resolver.resolved
  }

  private fun Size.toCoilSize() = CoilSize(
    width = if (width.isFinite()) Dimension(width.roundToInt()) else Dimension.Undefined,
    height = if (height.isFinite()) Dimension(height.roundToInt()) else Dimension.Undefined
  )
}

internal class Resolver(
  internal val request: ImageRequest,
  internal val imageLoader: ImageLoader,
  private val sizeResolver: SizeResolver,
) : RememberWorker() {

  internal var resolved: ResolveResult by mutableStateOf(
    ResolveResult(delegate = null)
  )

  @OptIn(ExperimentalCoilApi::class)
  override suspend fun work() {
    val imageLoader = imageLoader
        .newBuilder()
        .components {
            add(HeifDecoder3.Factory(request.context))
            // SVGs
            add(SvgDecoder.Factory(false))
            add(JxlDecoder.Factory())
            // GIFs
            add(AnimatedImageDecoder.Factory())
        }.build()
    val result = imageLoader.execute(
      request.newBuilder()
        .size(request.defined.sizeResolver ?: sizeResolver)
        // There's no easy way to be certain whether an image will require sub-sampling in
        // advance so assume it'll be needed and force Coil to write this image to disk.
        .diskCachePolicy(
          when (request.diskCachePolicy) {
            CachePolicy.ENABLED -> CachePolicy.ENABLED
            CachePolicy.READ_ONLY -> CachePolicy.ENABLED
            CachePolicy.WRITE_ONLY,
            CachePolicy.DISABLED -> CachePolicy.WRITE_ONLY
          }
        )
        // This will unfortunately replace any existing target, but it is also the only
        // way to read placeholder images set using ImageRequest#placeholderMemoryCacheKey.
        // Placeholder images should be small in size so sub-sampling isn't needed here.
        .target(
          onStart = {
            resolved = resolved.copy(
              placeholder = it?.asPainter(request.context.resources),
            )
          }
        )
        // Increase memory cache hit rate because the image will anyway fit the canvas
        // size at draw time.
        .precision(
          when (request.defined.precision) {
            Precision.EXACT -> request.precision
            else -> Precision.INEXACT
          }
        )
        .build()
    )

    val imageSource = when (val it = result.toSubSamplingImageSource()) {
      null -> null
      is EligibleForSubSampling -> it.source
      is ImageDeletedOnlyFromDiskCache -> {
        return
      }
    }
    resolved = resolved.copy(
      crossfadeDuration = result.crossfadeDuration(),
      delegate = if (result is SuccessResult && imageSource != null) {
        ZoomableImageSource.SubSamplingDelegate(
          source = imageSource,
          imageOptions = ImageBitmapOptions(from = result.asBitmap()!!)
        )
      } else {
        ZoomableImageSource.PainterDelegate(
          painter = result.asDrawable()?.asPainter()
        )
      },
    )
  }

  @OptIn(ExperimentalCoilApi::class)
  private fun ImageResult.asDrawable() = image?.asDrawable(request.context.resources)

  @OptIn(ExperimentalCoilApi::class)
  private fun ImageResult.asBitmap() = (image?.asDrawable(request.context.resources) as? BitmapDrawable)?.bitmap

  @OptIn(ExperimentalCoilApi::class)
  private fun Image.asPainter(resources: Resources) = asDrawable(resources).asPainter()

  private sealed interface ImageSourceCreationResult {
    data class EligibleForSubSampling(val source: SubSamplingImageSource) : ImageSourceCreationResult

    /** Image was deleted from the disk cache, but is still present in the memory cache. */
    data object ImageDeletedOnlyFromDiskCache : ImageSourceCreationResult
  }

  @OptIn(ExperimentalCoilApi::class)
  private suspend fun ImageResult.toSubSamplingImageSource(): ImageSourceCreationResult? {
    val result = this
    val source = if (result is SuccessResult && result.asDrawable() is BitmapDrawable) {
      val preview = result.asBitmap()?.asImageBitmap()
      when {
        // Prefer reading of images directly from files whenever possible because
        // it is significantly faster than reading from their input streams.
        result.diskCacheKey != null -> {
          val diskCache = imageLoader.diskCache!!
          val snapshot = withContext(Dispatchers.IO) {  // IO because openSnapshot() can delete files.
            diskCache.openSnapshot(result.diskCacheKey!!)
          }
          if (snapshot == null) {
            return when (result.dataSource) {
              DataSource.MEMORY_CACHE -> ImageDeletedOnlyFromDiskCache
              else -> error("Coil returned an image that is missing from its disk cache")
            }
          }
          SubSamplingImageSource.file(snapshot.data, preview, onClose = snapshot::close)
        }
        result.dataSource.let { it == DataSource.DISK || it == DataSource.MEMORY_CACHE } -> {
          // Possible reasons for reaching this code path:
          // - Locally stored images such as assets, resource, etc.
          // - Remote image that wasn't saved to disk because of a "no-store" HTTP header.
          result.request.mapRequestDataToUriOrNull()?.let { uri ->
            SubSamplingImageSource.contentUriOrNull(uri, preview)
          }
        }

        else -> {
          // Image wasn't saved to the disk. Telephoto won't be able to load this image in its full
          // quality. It'll attempt to display the bitmap directly as a fallback, but that can
          // potentially cause an OutOfMemoryError when the bitmap is drawn.
          return null
        }
      }
    } else {
      return null
    }
    return if (source?.canBeSubSampled() == true) EligibleForSubSampling(
      source
    ) else null
  }


  private fun ImageResult.crossfadeDuration(): Duration {
    val transitionFactory = request.transitionFactory
    return if (this is SuccessResult && transitionFactory is CrossfadeTransition.Factory) {
      // I'm intentionally not using factory.create() because it optimizes crossfade duration
      // to zero if the image was fetched from memory cache. SubSamplingImage will only read
      // bitmaps from the disk so there will always be some delay in showing the image.
      transitionFactory.durationMillis.milliseconds
    } else {
      Duration.ZERO
    }
  }

  private fun ImageRequest.mapRequestDataToUriOrNull(): Uri? {
    val dummyOptions = Options(request.context) // Good enough for mappers that only use the context.
    return when (val mapped = imageLoader.components.map(data, dummyOptions)) {
      is Uri -> mapped
      is File -> Uri.parse(mapped.path)
      else -> null
    }
  }

  private fun Drawable.asPainter(): Painter {
    return DrawablePainter(mutate())
  }
}