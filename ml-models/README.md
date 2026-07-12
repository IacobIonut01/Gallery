# ML Models Directory

This module contains the on-device machine learning models used by the ReFra application. To keep the base APK size compact, models are downloaded on-demand under **Settings > Smart Features**, unless bundled in a specific `withML` build variant.

---

## 1. Smart Search & Classification Models (CLIP)

Used for semantic search queries and automatic image classification.

*   **Tokenizer Vocab (`vocab.json`):**
    *   **Source:** OpenAI CLIP / Hugging Face `openai/clip-vit-base-patch32`
    *   **URL:** `https://raw.githubusercontent.com/IacobIonut01/ReFra/refs/heads/main/ml-models/src/main/assets/vocab.json`
    *   **SHA-256:** `e089ad92ba36837a0d31433e555c8f45fe601ab5c221d4f607ded32d9f7a4349`
*   **Tokenizer Merges (`merges.txt`):**
    *   **Source:** OpenAI CLIP / Hugging Face `openai/clip-vit-base-patch32`
    *   **URL:** `https://raw.githubusercontent.com/IacobIonut01/ReFra/refs/heads/main/ml-models/src/main/assets/merges.txt`
    *   **SHA-256:** `9fd691f7c8039210e0fced15865466c65820d09b63988b0174bfe25de299051a`
*   **Textual Encoder (`textual_quant.onnx`):**
    *   **Description:** Quantized INT8 textual CLIP model for embedding text search queries.
    *   **URL:** `https://raw.githubusercontent.com/IacobIonut01/ReFra/refs/heads/main/ml-models/src/main/assets/textual_quant.onnx`
    *   **SHA-256:** `1ebb71a5ea1897823a829af8fc8168c5cfff761969bb62aee1fafdf5a2788aba`
*   **Visual Encoder (`visual_quant.onnx`):**
    *   **Description:** Quantized INT8 visual CLIP model for indexing gallery images.
    *   **URL:** `https://raw.githubusercontent.com/IacobIonut01/ReFra/refs/heads/main/ml-models/src/main/assets/visual_quant.onnx`
    *   **SHA-256:** `a2fbb26b5f6ab5c79dd9bf99ab2dbac4711abc88dc2e20afc02a0827aa3d59c2`

---

## 2. Subject Cutout Models

Used for isolating foreground subjects in the media viewer on long-press. These are stored under `ml-models/segment/` for local testing.

### A. YOLO26-seg (Nano & Large) — Fast Cutouts
Instant, single-pass instance segmentation models optimized for COCO classes.
*   **YOLO26-seg (Nano):** `yolo26n-seg.onnx` (~10.7 MB)
    *   **Source:** Ultralytics YOLO26 Nano Segment model exported to ONNX (opset 12).
    *   **Download URL:** `https://huggingface.co/zwh20081/yolo26-onnx/resolve/main/yolo26n-seg.onnx`
    *   **SHA-256 Checksum:** `e3b81408f09f03f01c47c0aa375df6a2986837cb8a7626361e45c948950b9c3c`
*   **YOLO26-seg (Large):** `yolo26l-seg.onnx` (~107.1 MB)
    *   **Source:** Ultralytics YOLO26 Large Segment model exported to ONNX (opset 12).
    *   **SHA-256 Checksum:** `4d79b823b8a2513d04ef93b9737fc4a1246a06188b2c33df89d99414dcbca1ac`
    *   **Notes:** Provides significantly cleaner initial segments but has a larger download size. Used for local evaluation and high-performance devices.

### B. MobileSAM — Interactive Point Refinement
A two-stage Segment Anything Model optimized for edge devices, allowing users to tap to add/subtract regions to refine the mask.
*   **Image Encoder:** `mobile_sam_image_encoder.onnx` (~28.1 MB)
    *   **Source:** Acly's MobileSAM ONNX Export (Hugging Face `Acly/MobileSAM`)
    *   **Download URL:** `https://huggingface.co/Acly/MobileSAM/resolve/main/mobile_sam_image_encoder.onnx`
    *   **SHA-256 Checksum:** `580f5fb648ea1062c0aabc26217aed56921985f03f0cbbd852bba81d760cc749`
*   **Mask Decoder:** `sam_mask_decoder_single.onnx` (~16.5 MB)
    *   **Source:** Acly's MobileSAM Single-Mask ONNX Decoder Export (Hugging Face `Acly/MobileSAM`)
    *   **Download URL:** `https://huggingface.co/Acly/MobileSAM/resolve/main/sam_mask_decoder_single.onnx`
    *   **SHA-256 Checksum:** `93915fc7c993ab9d59ab8c9ccd3bce37f7509c81ab4150a74abd4d2abbd8570d`

---

## 3. Alternative/Evaluated Models (Reference Only)

*   **Qualcomm FastSAM-X:**
    *   **Model Files:** `fastsam_x.onnx` (~137 KB) + `fastsam_x.data` (~288 MB)
    *   **Source:** Qualcomm AI Hub models (Snapdragon optimized single-pass instance segmentation).
    *   **LFS URL:** `https://qaihub-public-assets.s3.us-west-2.amazonaws.com/qai-hub-models/models/fastsam_x/releases/v0.56.0/fastsam_x-onnx-float.zip`
    *   **Notes:** Evaluated during research. Excels at class-agnostic single-pass segmentation but does not support interactive point prompts and is too large for general mobile distribution.
