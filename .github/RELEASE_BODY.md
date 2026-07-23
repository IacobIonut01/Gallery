## What's new in 5.1.0

ReFra 5.1.0 is a major capability release: connect multiple cloud services, develop RAW photos, edit huge images at full resolution, lift subjects on-device, browse people and places, and enjoy sharper HDR and animated media throughout the viewer.

### New Features

- **Multi-account cloud library** — Connect Immich, ownCloud, Nextcloud, WebDAV, SMB and NFS accounts; merge remote media into the timeline; configure per-album destinations; monitor backup and sync; browse shared links; and keep selected media available offline
- **Guided setup** — A redesigned first-run flow walks through permissions, appearance, AI models, cloud accounts and useful tips
- **On-device subject cutout** — ReFra detects the main subject without uploading the photo, then lets you refine, copy, share or save it with a transparent background; the editor also includes background removal (#991)
- **People and privacy tools** — Scan and group faces on-device, browse people, and blur detected faces manually or automatically
- **RAW development** — Develop RAW photos in the editor with white balance, exposure, highlight, colour-space and demosaic controls, then export JPEG, PNG or 8/16-bit TIFF
- **Full-resolution tiled editing** — Large photos are edited through a memory-bounded tiled pipeline that preserves their original resolution and source format
- **HDR HEIC viewer** — Native tiled HEIC decoding, Ultra HDR gain-map rendering and HDR display support deliver sharp zoom without loading the whole image into memory
- **Interactive photo map** — Photo thumbnail clusters, deterministic zooming, map appearance controls and a unified location timeline replace the old heatmap experience (#1037, #1038)
- **Lossless metadata sanitization** — Remove sensitive metadata from supported images without recompressing their pixels
- **Album slideshows** — Start configurable, full-screen playback from an album (#1032)
- **Media-type albums** — Browse automatic albums grouped by media format and type (#1030)
- **Album covers** — Choose any local photo as an album's cover
- **Secure picker vaults** — Authenticate individual vaults, select across vaults and safely clean up temporary picker files

### Improvements

- **Help & Tips** — Unified fuzzy search, real-component previews, quick actions and markdown-powered release notes make guidance easier to find
- **System-aware dates** — Date and time formats now follow the device by default, while an information sheet explains custom format strings (#953)
- **Sharper animation** — GIF, animated WebP and APNG render at full viewer resolution (#1056)
- **Smoother media viewer** — Faster opening and swiping, stable hidden controls, visible rotation progress and seamless refresh after overwrite or copy
- **Pinned albums** — Choose tile or banner layouts, with locked albums consistently using privacy-safe thumbnails
- **Photo stacks** — Matching RAW/DNG files rank below JPEGs so the processed photo becomes the stack cover
- **Selection experience** — Reused thumbnail painters reduce flashes, drag-selection hit testing is accurate, and mosaic counters stay correct
- **Navigation** — Re-tapping the selected navigation tab scrolls its content back to the top
- **AI model management** — Search and Subject Cutout models are managed independently and can be downloaded again after deletion

### Bug Fixes

- **Format detection** — Standard images mislabeled by MediaStore as RAW or TIFF now decode normally (#1054), while JXL metadata and unclassified special-format images are recovered correctly
- **Locked albums** — Pinned locked albums no longer reveal their cover thumbnail (#1057)
- **Private folders** — Fixed SAF crashes during move-out and deletion, restored video playback and refresh, and respected the configured selection actions (#1015)
- **Editor reliability** — Save failures and permission errors are reported, edited images refresh immediately, and streamed overwrites preserve the source until encoding succeeds
- **Subject cutout** — Fixed gesture routing, swipe positioning, memory cleanup, pager conflicts and failed-session state
- **Grid stability** — Fixed crashes while pinch-zooming mosaic timelines or scrolling smart-category preview results (#1019)
- **SVG rendering** — Files without a `viewBox` no longer show a second shrunken image (#1020)
- **Viewer playback** — Fixed white flashes between videos, controls reappearing during swipes, Ultra HDR detection after restart and screen timeout during playback (#998, #1005)
- **Picker navigation** — Returning from Android's media picker no longer breaks gallery back navigation
- **Cloud actions** — Multi-account selection actions now resolve the correct provider instance
