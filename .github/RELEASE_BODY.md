## What's new in 5.0.1

ReFra 5.0.1 is a polish release that follows up 5.0 with a wave of bug fixes across the viewer, timeline, and panorama playback. It also broadens image format support — PSD, JPEG 2000, TIFF, and SVG now open directly, HEIC/AVIF decode on hardware, and there's a new system-font display option plus high-quality JPEG XL zoom.

### New Features

- **More image formats** — Added full decoding for PSD (8BPS), JPEG 2000, TIFF, and SVG across both the timeline grid (Glide) and the media viewer (Sketch), with magic-byte sniffing since MIME types are unreliable for some of these. Each format also gets a generic region decoder so pinch-to-zoom stays crisp in the viewer.
- **Hardware-accelerated HEIC/AVIF** — HEIC and AVIF now decode hardware-first via `ImageDecoder` (HEVC/AV1 codec) with a HeifCoder software fallback, and AVIF center-crop color artifacts are fixed by forcing RGBA_8888 in the Glide HEIF decoders.
- **Use system font** — A new display option switches the app over to your device's system font instead of the bundled typeface (#931).
- **High-quality JXL zoom** — JPEG XL images now zoom sharply in the viewer through a dedicated JxlCoder region decoder.

### Improvements

- **Media-volume video playback** — Video now plays only through the media volume stream. 'Request audio focus' option is removed.
- **Dismissible What's New card** — The What's New card on the timeline can now be dismissed without opening it (#935).
- Removed the pause-image-loading-while-flinging behaviour for smoother, more consistent scrolling.
- Updated Simplified Chinese translations (Vault renamed to 保险箱, Private Folder strings translated).

### Bug Fixes

- Improved editor markup text gestures (top-right handle now rotates, text stays within canvas bounds, correct two-finger centroid when zoomed) and crop box interactions (move small boxes, two-finger pinch resize, auto zoom-to-fit, clearer Crop action button) (#936)
- Fixed timeline scroll position not being preserved when returning from the media viewer
- Fixed settings categories losing their scroll position when navigating to a detail page
- Fixed album cover not matching the first item shown inside the album
- Fixed broken viewer exit animations by restricting the shared-element transition to the current page
- Fixed a black screen in the standalone viewer after deleting the current photo
- Fixed a crash on launch when MediaStore returned null string columns
- Fixed video zoom on forced rotation and made pinch-to-zoom feel natural
- Fixed the back gesture being ignored on videos when controls were hidden (#918)
- Fixed the video info section closing when auto-hide fired (#930)
- Fixed the panorama compass not updating on portrait/landscape rotation
- Fixed panorama gyro axes not being remapped for display rotation
- Fixed text disappearing and the cursor jumping in date format fields
- Fixed the scrubber value resetting to its default near zero (#934)
- Fixed home screen widgets by self-healing when the cached bitmap is missing
- Fixed Facebook export photos showing future dates in the timeline
- Fixed the search bar placeholder overflowing to a second line
- Fixed the erroneous "Copied to clipboard" toast on Android 13+ (#928)
