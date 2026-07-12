# 0003-single-pass-cutout-mobilesam-refinement

We decided to pivot the image cutout pipeline from the two-pass architecture (MobileSAM + BiRefNet) to a single-pass architecture (MobileSAM only) with fast real-time mask refinement.

## Context

*   The two-pass cutout pipeline (MobileSAM + BiRefNet) provided high-quality edge maps, but the second pass (BiRefNet) was heavy, took significant time to process (refinement phase), and occasionally crashed on low-memory or thermal-throttled mobile devices.
*   The download footprint was large (~153.8 MB total), mostly due to the BiRefNet Lite ONNX model (~109 MB).
*   We needed a solution that provides instant cutout responses (0ms refinement delay on save/copy/share) and is highly stable across all Android devices, without sacrificing boundary cleanliness.

## Decision

We will pivot to a **Single-Pass Cutout Pipeline with Real-Time Refinement**:
1.  **Single-Pass Inference:** We will use only **MobileSAM** for both the interactive prompt parsing and the final mask generation. `birefnet_lite.onnx` is completely removed from the assets, download list, and build pipeline.
2.  **Real-Time Edge Smoothing:** To resolve the jagged/pixelated edges of the low-resolution SAM mask decoder, we will apply a real-time smoothing filter on the cropped bounding box:
    *   Compute a dynamic blur radius proportional to the cropped box dimensions (e.g., `0.5%` of the max dimension, clamped between 1 and 8).
    *   Apply a fast `boxBlur` to the alpha channel of the crop.
    *   Apply a sigmoid contrast lookup table (LUT) remapping to sharpen the blurred alpha transition, producing a smooth, feathered, high-quality edge.
3.  **Instant Finalization:** The refinement is done during the interactive decoding step. The `finalizeCutout` step becomes a 0ms pass-through returning the pre-calculated, smoothed cutout bitmap.
4.  **UI & UX Redesign:**
    *   **Combined Cutout Controls Pill:** A single glassmorphic `CombinedCutoutControlsPill` at bottom-center houses refinement tools (Include/Exclude, Undo/Redo, Reset) in the top row and action buttons (Copy/Share/Save) in a bottom row, separated by a divider. The combined layout keeps all controls in one reachable spot.
    *   **Floating Close Button:** A circular Close (`X`) button floats at the top-right corner to exit the cutout mode.
    *   **Glowing Point Markers:** Selected prompt points render with a glowing outer halo and a crisp white border.
    *   **Auto-Refine Gesture:** Refinement mode automatically defaults to `+` (Include) after the initial long press.
    *   **No Dismiss on Background Tap:** Tapping the dimmed background outside the image content area does not dismiss the cutout session to prevent accidental loss of prompt points.

## Consequences

*   **Improved Performance:** Copy, Share, and Save operations complete instantaneously (0ms latency), since the refined cutout is already cached from the interactive step.
*   **Reduced Footprint:** Lowered minimum required storage space by ~110 MB (down to ~44 MB total), resulting in a much faster and more reliable model download process.
*   **Enhanced Stability:** Eliminates memory overhead and crashes associated with loading/running the second model.
*   **Premium Visuals:** Clean glassmorphic layouts, distinct point styling with glowing halos, and smooth feathered cutout boundaries create a premium, modern user experience.
