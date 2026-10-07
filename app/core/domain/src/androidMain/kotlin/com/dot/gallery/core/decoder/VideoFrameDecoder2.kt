package com.dot.gallery.core.decoder

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.BitmapParams
import androidx.exifinterface.media.ExifInterface
import com.github.panpf.sketch.ComponentRegistry
import com.github.panpf.sketch.Image
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.asImage
import com.github.panpf.sketch.decode.DecodeConfig
import com.github.panpf.sketch.decode.DecodeException
import com.github.panpf.sketch.decode.Decoder
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.decode.internal.DecodeHelper
import com.github.panpf.sketch.decode.internal.ExifOrientationHelper
import com.github.panpf.sketch.decode.internal.HelperDecoder
import com.github.panpf.sketch.decode.internal.checkImageInfo
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.request.videoFrameOption
import com.github.panpf.sketch.source.ContentDataSource
import com.github.panpf.sketch.source.DataSource
import com.github.panpf.sketch.source.getFileOrNull
import com.github.panpf.sketch.util.Rect
import com.github.panpf.sketch.util.Size
import com.github.panpf.sketch.util.div
import com.dot.gallery.core.util.and
import com.dot.gallery.core.util.or
import com.dot.gallery.domain.uri.path


/*
 * Copyright (C) 2024 panpf <panpfpanpf@outlook.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



fun ComponentRegistry.Builder.supportVideoFrame2(): ComponentRegistry.Builder = apply {
    add(VideoFrameDecoder2.Factory())
}

/**
 * Decode a frame of a video file and convert it to Bitmap
 *
 * Notes: Android O(26/8.0) and before versions do not support scale to read frames,
 * resulting in slow decoding speed and large memory consumption in the case of large videos and causes memory jitter
 *
 * The following decoding related properties are supported:
 *
 * * sizeResolver: Only sampleSize
 * * sizeMultiplier
 * * precisionDecider: Only LESS_PIXELS and SMALLER_SIZE is supported
 * * colorSpace: Only on Android 30 or later
 * * videoFrameMicros
 * * videoFramePercent
 * * videoFrameOption
 *
 * The following decoding related properties are not supported:
 *
 * * scaleDecider
 * * colorType
 *
 * @see com.github.panpf.sketch.video.test.decode.VideoFrameDecoderTest
 */
class VideoFrameDecoder2(
    requestContext: RequestContext,
    dataSource: DataSource,
    mimeType: String,
) : HelperDecoder(
    requestContext = requestContext,
    dataSource = dataSource,
    decodeHelperFactory = {
        VideoFrameDecodeHelper2(
            sketch = requestContext.sketch,
            request = requestContext.request,
            dataSource = dataSource,
            mimeType = mimeType
        )
    }
) {

    class Factory : Decoder.Factory {

        override val key: String = "VideoFrameDecoder"

        override val sortWeight: Int = 0

        override fun create(
            requestContext: RequestContext,
            fetchResult: FetchResult
        ): VideoFrameDecoder2? {
            val mimeType = fetchResult.mimeType ?: return null
            if (!isApplicable(mimeType)) return null
            return VideoFrameDecoder2(
                requestContext = requestContext,
                dataSource = fetchResult.dataSource,
                mimeType = mimeType
            )
        }

        private fun isApplicable(mimeType: String): Boolean {
            return mimeType.startsWith("video/")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other != null && this::class == other::class
        }

        override fun hashCode(): Int {
            return this::class.hashCode()
        }

        override fun toString(): String = "VideoFrameDecoder"
    }
}