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
A two-stage Segment Anything Model optimized for edge devices, allowing users to tap to add/subtract regions to refine the mask. **These two files are bundled in this module's `src/main/assets/` for the `withML` variant** (and mirrored via `BASE_DOWNLOAD_URL` for on-demand download in `noML` builds).
*   **Image Encoder:** `mobile_sam_image_encoder.onnx` (~28.1 MB)
    *   **Source:** Acly's MobileSAM ONNX Export (Hugging Face `Acly/MobileSAM`)
    *   **Download URL:** `https://huggingface.co/Acly/MobileSAM/resolve/main/mobile_sam_image_encoder.onnx`
    *   **SHA-256 Checksum:** `580f5fb648ea1062c0aabc26217aed56921985f03f0cbbd852bba81d760cc749`
*   **Mask Decoder:** `sam_mask_decoder_single.onnx` (~16.5 MB)
    *   **Source:** Acly's MobileSAM Single-Mask ONNX Decoder Export (Hugging Face `Acly/MobileSAM`)
    *   **Download URL:** `https://huggingface.co/Acly/MobileSAM/resolve/main/sam_mask_decoder_single.onnx`
    *   **SHA-256 Checksum:** `93915fc7c993ab9d59ab8c9ccd3bce37f7509c81ab4150a74abd4d2abbd8570d`

> **License & attribution:** MobileSAM is released under the **Apache License 2.0** by Chaoning Zhang et al.
> ("Faster Segment Anything: Towards Lightweight SAM for Mobile Applications", 2023), building on Meta AI's
> Segment Anything Model (SAM). The ONNX exports used here are redistributed from the Hugging Face repository
> [`Acly/MobileSAM`](https://huggingface.co/Acly/MobileSAM) under the same Apache-2.0 terms. The bundled copies
> in `src/main/assets/` are byte-for-byte identical to the upstream files (verify via the SHA-256 checksums above).

---

## 3. Faces & People Models

Used for the editor's automatic face-blur action and fully on-device Person grouping in the Library. **Both files are bundled in this module's `src/main/assets/` for the `withML` variant** and downloaded on-demand from their upstream sources in `noML` builds.

### A. Face Detection — UltraFace RFB-320
Lightweight single-pass face detector that powers the editor's **"Blur faces"** action.
*   **Detector:** `version-RFB-320.onnx` (~1.3 MB)
    *   **Source:** ONNX Model Zoo — UltraFace (Ultra-Light-Fast-Generic-Face-Detector-1MB)
    *   **Download URL:** `https://github.com/onnx/models/raw/main/validated/vision/body_analysis/ultraface/models/version-RFB-320.onnx`
    *   **SHA-256 Checksum:** `34cd7e60aeff28744c657de7a3dc64e872d506741de66987f3426f2b79f88017`

### B. Face Recognition — ArcFace
Face-embedding model (112x112 → 512-d) used to cluster the same person across photos, fully on-device. Requires the detector above.
*   **Recognizer:** `arcface.onnx` (~130 MB)
    *   **Source:** Hugging Face `garavv/arcface-onnx` (`arc.onnx`, redistributed as `arcface.onnx`)
    *   **Download URL:** `https://huggingface.co/garavv/arcface-onnx/resolve/main/arc.onnx`
    *   **SHA-256 Checksum:** `ffe014a45c9488506719d37fd578ece6661bb385535b36e8039975fa5d4683db`

> **Note:** The bundled copies in `src/main/assets/` are byte-for-byte identical to the upstream files (verify via the SHA-256 checksums above). Detection works independently of recognition, so auto face-blur functions with only the detector installed.

---

## Large models: split & reassemble (GitHub 100 MB limit)

GitHub rejects any file over 100 MB, so oversized model assets (e.g. `arcface.onnx`, ~130 MB) are **not** committed whole. Instead they are split into `<100 MB` parts committed under `src/main/model-parts/`, tracked by `src/main/model-parts/manifest.json`, and reassembled into `src/main/assets/` automatically at build time. This is model-agnostic — nothing is hard-coded to a specific file.

Gradle tasks (module `:ml-models`):

*   **`splitLargeModels`** — developer helper. Detects any asset in `src/main/assets/` over the limit, splits it into `<name>.partNNN` parts under `model-parts/`, records the part list + SHA-256 + size in `manifest.json`, deletes the monolith from `assets/`, and updates the managed block in the repo-root `.gitignore`.
*   **`assembleModels`** — runs automatically before packaging (APK asset merge and the AAB asset pack). Concatenates the parts back into `src/main/assets/<name>` and verifies the SHA-256.
*   **`checkModelSizes`** — build/CI guard. Fails the build if an **unmanaged** asset exceeds 100 MB, pointing you to `splitLargeModels`.

### Adding a new large model
1. Drop the model file into `src/main/assets/`.
2. Run `./gradlew :ml-models:splitLargeModels`.
3. Commit the generated `src/main/model-parts/` parts + `manifest.json` and the `.gitignore` change. **Do not** commit the reassembled file in `assets/` (it is git-ignored).

Config lives at the top of `ml-models/build.gradle.kts` (`mlSourceDirs`, `mlPartsDir`, `mlMaxBytes`, `mlPartBytes`). Manual reassembly for inspection: `cat src/main/model-parts/arcface.onnx.part* > src/main/assets/arcface.onnx`.

---

## 4. Alternative/Evaluated Models (Reference Only)

*   **Qualcomm FastSAM-X:**
    *   **Model Files:** `fastsam_x.onnx` (~137 KB) + `fastsam_x.data` (~288 MB)
    *   **Source:** Qualcomm AI Hub models (Snapdragon optimized single-pass instance segmentation).
    *   **LFS URL:** `https://qaihub-public-assets.s3.us-west-2.amazonaws.com/qai-hub-models/models/fastsam_x/releases/v0.56.0/fastsam_x-onnx-float.zip`
    *   **Notes:** Evaluated during research. Excels at class-agnostic single-pass segmentation but does not support interactive point prompts and is too large for general mobile distribution.
