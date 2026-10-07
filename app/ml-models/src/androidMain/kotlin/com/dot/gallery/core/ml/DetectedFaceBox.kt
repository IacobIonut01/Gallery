/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.core.graphics.scale
import java.nio.FloatBuffer
import java.util.Collections
import com.dot.gallery.core.logging.printWarning


/**
 * A single detected face: bounding box in normalized [0,1] image coordinates plus the detector
 * confidence and (optionally) a recognition embedding.
 */
data class DetectedFaceBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float,
    val embedding: FloatArray? = null
) {
    val rectF get() = RectF(left, top, right, bottom)
}