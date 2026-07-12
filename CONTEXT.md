# ReFra Gallery Context

This context defines the ubiquitous language for the gallery application's on-device media experiences and AI features.

## Language

**Subject Cutout**:
The process of isolating one or more distinct subjects (objects, people, or pets) from the background of an image using on-device machine learning.
_Avoid_: Object cropping, image cropping, subject isolation

**Active Cutout Mode**:
The user interface state entered when a subject has been successfully segmented and is selected, dimming the background and presenting action controls to the user.
_Avoid_: Selection state, editing state

**Target Media**:
The media item currently loaded in the image viewer that is being segmented.
_Avoid_: Selected image, active photo

**Cutout Engine**:
The MobileSAM ONNX session that performs both initial segmentation and interactive refinement via point prompts.
_Avoid_: First-Pass Segmenter, Attention Selector

**Mask Refinement**:
The post-processing pipeline (BFS hole-fill, island removal, box blur, sigmoid LUT) applied to the raw SAM mask to produce smooth, feathered edges.
_Avoid_: Second-Pass Refiner, Matting Engine

**Additive Point Mode**:
A configuration (plus icon) where user clicks/taps are translated into foreground (positive) prompt points for the First-Pass Segmenter.
_Avoid_: Plus brush, green brush, paint mode

**Subtractive Point Mode**:
A configuration (minus icon) where user clicks/taps are translated into background (negative) prompt points for the First-Pass Segmenter.
_Avoid_: Minus brush, red brush, erase mode

**Cached Cutout**:
The pre-computed transparent bitmap returned instantly by `finalizeCutout()`.
_Avoid_: Final Cutout, result, cutout image, output png

