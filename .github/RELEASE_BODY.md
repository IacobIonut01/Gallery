## What's new in 5.1.3

ReFra 5.1.3 makes media navigation and organization faster and safer, while improving editor reliability, location indexing, offline AI models, and thumbnail accuracy.

### New Features

- **Instant tap navigation** — Optionally tap the left or right edge of the media viewer to move immediately between photos and videos, with an opt-in prompt, visual preview, RTL support, and a searchable setting (#1151)
- **Safe moves from restricted folders** — Copy or move media from folders such as WhatsApp's `Android/media` storage through a verified transfer and Android deletion request, with original timestamps preserved and automatic rollback when the request is declined or fails (#1144)

### Improvements

- **Safer offline AI models** — Offline builds protect bundled models from deletion and unavailable network actions, while model installation validates staged assets before replacing existing files atomically (#1118)

### Bug Fixes

- **Reliable editor markup and previews** — Blur, mosaic, highlighter, text, and borders are preserved across save and reopen operations, while live adjustment previews no longer flash black or lose pending edits (#1078)
- **Preserved GPS metadata during scans** — Bulk metadata indexing keeps valid coordinates when reverse geocoding fails and requests unredacted location metadata when Android permits it (#1141)
- **Correct front-camera thumbnails** — Mirrored embedded JPEG thumbnails are bypassed so timeline previews match the full photo (#1152)
- **Safer media transfers** — Copied and moved media retain their original dates, partial transfers are cleaned up, and failed deletion flows do not leave unannounced duplicate files (#1144)
- **Reliable F-Droid ARM builds** — The native AVIF and HEIF encoder build now uses the pinned NDK Clang toolchain for AOM assembly instead of relying on an unavailable host assembler (#1068)
