/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ml

enum class ModelGroup(val subDir: String, val files: List<String>) {
    SEARCH(
        subDir = "clip",
        files = listOf("visual_quant.onnx", "textual_quant.onnx", "vocab.json", "merges.txt")
    ),
    CUTOUT(
        subDir = "sam",
        files = listOf("mobile_sam_image_encoder.onnx", "sam_mask_decoder_single.onnx")
    ),

    /** UltraFace RFB-320 lightweight face detector — powers auto face-blur in the editor. */
    FACE_DETECT(
        subDir = "face_detect",
        files = listOf("version-RFB-320.onnx")
    ),

    /** ArcFace/MobileFaceNet face-embedding model — powers on-device Person grouping. */
    FACE_RECOGNITION(
        subDir = "face_recog",
        files = listOf("arcface.onnx")
    );

    companion object {
        /** The group a model file belongs to (defaults to [SEARCH] for unknown names). */
        fun of(fileName: String): ModelGroup = entries.firstOrNull { fileName in it.files } ?: SEARCH
    }
}
