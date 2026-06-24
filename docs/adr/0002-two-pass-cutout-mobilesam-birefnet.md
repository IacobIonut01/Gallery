# 0002-two-pass-cutout-mobilesam-birefnet

We decided to implement a two-pass cutout pipeline combining **MobileSAM** (Segment Anything Model) and **BiRefNet Lite** (Bilateral Reference Network) to resolve the trade-off between user-guided selection and high-resolution edge quality.

## Context

*   Single-pass background removal models (like RMBG-1.4 or BiRefNet) output exceptionally high-quality edge maps (ideal for hair, fur, and soft borders) but are class-agnostic and segment the entire foreground. They cannot select *which* object to cut out when multiple objects are present.
*   Interactive segmentation models (like MobileSAM) accept point prompts, letting users select and refine specific subjects. However, because their internal mask decoders operate at low resolutions (e.g. $256 \times 256$), the resulting upscaled boundaries are often jagged, simplified, or pixelated on high-resolution camera photos.
*   Combining both models in a single-pass is too slow and computationally expensive for mobile devices.
*   Manual brush refinement (painting pixels) was rejected as tedious and undesirable.

## Decision

We will implement a **Two-Pass Cutout Pipeline**:
1.  **First Pass (Interactive Attention Selector):** Tapping and holding an object triggers MobileSAM to generate a class-agnostic attention mask. The user can refine the selection dynamically by adding positive (include) or negative (exclude) point prompts. Only one object is processed per run.
2.  **Attention Crop:** Once the user is satisfied and triggers the refinement action, the app crops the original high-resolution photo to the bounding box of the MobileSAM mask, adding a **10% padding** (Refinement Margin) on all sides to provide background context.
3.  **Second Pass (Matting/Refinement Engine):** The Attention Crop is resized to $1024 \times 1024$ and fed to the lightweight **BiRefNet Lite FP16 ONNX model** (`birefnet_lite_fp16.onnx`, ~109.2 MB) to generate a high-precision alpha mask.
4.  **Final Cutout:** The high-resolution mask is combined with the Attention Crop, smoothed using a fast box blur, remapped, and cropped tightly to the non-transparent pixels to create the final transparent PNG cutout.

### Refinement & UX Gestures
*   **Gesture Separation:** We will use natural gesture separation rather than an explicit "navigation" tool state. Pinch-zoom and drag-pan remain handled by the underlying `ZoomImage` container, and the `onTap` listener is used exclusively to capture coordinates for positive/negative prompt points.
*   **Resource Lifecycle:** To avoid memory warnings or OOM errors on edge devices, the ONNX InferenceSessions for the SAM Encoder and SAM Decoder are initialized on-demand when entering **Active Cutout Mode** and closed/disposed immediately upon exit (navigating away, closing the selection, or finishing a Copy/Share/Save action). Cached embeddings are also cleared at this point.
*   **Model Downloader:** We will follow the app's existing background download worker and settings UI structure to download the three required model files.

## Consequences

*   **Exceptional Quality:** Cuts are extremely clean, snapping perfectly to fine details (clothing, hair, outlines) thanks to BiRefNet, while keeping interactive selection possible via MobileSAM.
*   **Optimal Performance (UX):** Step 2 runs *on-demand* (only when saving/copying/sharing) rather than real-time on every tap. The user interacts with the fast ~30ms SAM decoder first, and only incurs the ~150ms BiRefNet pass once at the end.
*   **Download Size:** Requires downloading the MobileSAM models (~44 MB) and the BiRefNet Lite model (~109 MB), totaling **~153.8 MB**. The model downloader under Settings > Smart Features is updated to manage all three files on-demand.
*   **Memory Overhead:** Caching and resource loading are strictly bounded to the active selection session, minimizing long-term RAM usage.
