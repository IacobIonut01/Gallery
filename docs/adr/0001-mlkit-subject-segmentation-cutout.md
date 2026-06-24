# 0001-subject-segmentation-cutout-mobilesam

We decided to use an on-device, on-demand downloaded **MobileSAM ONNX** model (via ONNX Runtime) to power the "tap and hold to cutout an object" feature and the interactive point-prompt refinement tool.

## Context

*   We want a subject cutout feature that isolates objects on tap-and-hold.
*   The initial selection may be imperfect, so we need a refinement tool to add/remove areas.
*   To keep the refinement smart (working together with the AI model rather than manually painting pixels), the model must support point/coordinate prompts.
*   Google Play Services ML Kit's segmenter does not support interactive point prompts for manual refinement and has external GMS dependencies.
*   Background removal models (like RMBG-1.4) are fast but only output a single global foreground mask and cannot accept interactive point prompts.
*   **MobileSAM** (Segment Anything Model optimized for edge devices) supports point prompts. Its image encoder is ~28.1 MB, and its mask decoder is ~16.5 MB.
*   By downloading these models on demand, we avoid ballooning the initial APK size while maintaining a fully offline, private AI capability that runs entirely on ONNX Runtime (which is already integrated for CLIP search models).

## Decision

*   Implement the cutout feature using a two-stage MobileSAM ONNX model.
*   On long-press, run the MobileSAM Image Encoder once to get image embeddings.
*   Run the MobileSAM Mask Decoder (takes ~30ms) using the clicked coordinate as a positive prompt to generate the initial mask.
*   Provide Additive (+) and Subtractive (-) point modes directly in Active Cutout Mode. Single-finger tap gestures on the image record coordinates which are fed to the decoder as additional foreground/background prompt points. The AI model dynamically updates the cutout shape in real-time.
*   Pinch-to-zoom and drag-to-pan remain enabled by default at all times (using standard multi-touch pinch and drag gestures). Tap coordinates are recorded on quick clicks without conflicting with pan/zoom drags.

## Consequences

*   The first long-press on an image incurs a 1.5s–3s latency while the image encoder runs (this can be optimized by caching embeddings or using hardware acceleration where available). Subsequent refinement decoder calls are near-instant (~30ms).
*   Models are downloaded on-demand under Settings > Smart Features, keeping the base APK size low.
*   Users on offline-only variants without network permissions cannot download the models and will have the feature disabled.
