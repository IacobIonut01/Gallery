## What's new in 5.1.1

ReFra 5.1.1 adds precise frame extraction, secure Nextcloud browser sign-in, broader Smart Search controls, and extensive reliability improvements across browsing, cloud backup, Smart Features, and the media viewer.

### New Features

- **Motion Photo and video frame extraction** — Scrub to an exact moment, select one or more frames, and export up to 50 JPEG or PNG images while preserving capture metadata when available (#910)
- **Nextcloud two-factor sign-in** — Authorize through Nextcloud Login Flow v2 in your browser, including two-factor authentication, while ReFra securely stores the generated app password (#1073)
- **Search ignored albums** — Optionally include timeline-hidden albums in Smart Features and Smart Search without making them visible in the timeline; locked albums remain private (#1098)
- **Remote and keyboard settings navigation** — Move predictably through settings with a D-pad, keyboard, or remote, with clear focus across lists, controls, and actions

### Improvements

- **Resumable Smart Features scans** — Checkpointed, energy-aware stages preserve valid results, recover after interruption, skip unchanged work, and avoid repeatedly scanning photos with no detected faces (#1088)
- **Faster thumbnail grids** — Lightweight thumbnails keep scrolling smooth, then refine to full quality when movement stops; MediaStore thumbnails also use a direct fast path (#1076)
- **More efficient cloud backup** — Streamed uploads, bounded concurrency, batched checks, and preserved verification state reduce repeated hashing, I/O, and network work (#1016)
- **Clearer timeline organization** — Date separators and daily, monthly, or yearly grouping can be configured independently across Timeline, Albums, Favorites, Vault, and Cloud Archive
- **More discoverable settings** — Help & Tips search now learns additional rendered settings automatically
- **Security and permission transparency** — Security settings report the active encryption or fallback state, and setup correctly recognizes Android's selected-photo access as limited permission
- **Reproducible native builds** — Local, CI, and F-Droid native builds use the same pinned stable NDK toolchain (#1068)

### Bug Fixes

- **Large album navigation** — Opening a selected item from a large local album now lands on the photo that was tapped instead of the first item
- **Motion Photo playback** — Fixed cropping, surface recreation, wrong-page playback, zoom mismatches, and distorted filmstrip thumbnails (#1107)
- **Safe media actions** — Favorite, trash, restore, delete, and write operations now work safely for JXL, JPEG 2000, PSD/PSB, APNG, and other media indexed in Android's generic Files collection (#1113)
- **Cloud reliability** — Prevented stuck indexing notifications, preserved verified backups through re-indexing, and made original downloads account-safe, atomic, and resilient to interruption (#1045)
- **Category freshness** — Smart categories no longer retain deleted items, stale counts, or missing cover thumbnails
- **Trash and restore safety** — Actions wait for confirmed completion, preserve storage-volume identity, and no longer silently become permanent deletion (#1090)
- **Merged album browsing and filtering** — Merged subfolders display their media again, and timeline filters resolve all source folders without hiding the timeline (#1080)
- **Viewer and grid stability** — Fixed Fancy Blur contrast, viewer flashes, internal-cache reliability, rotation-lock handling, and invalid pinch-grid transitions (#1029, #1062, #1065, #1074)
- **Search and screen states** — Back clears active searches even with no results, and gallery screens prioritize real errors over misleading loading or empty states
- **Settings correctness** — System-following date formats remain system-controlled instead of appearing as custom overrides
- **Smart scan efficiency** — Successful face scans with no detections are no longer repeated unnecessarily
- **Viewer actions** — Removed the obsolete automatic Subject Cutout suggestion pill while retaining editor and configured long-press access
- **Android 17 networking** — ReFra requests the local-network permission needed by LAN cloud providers and casting (#1092)
- **Themed icon sizing** — The monochrome launcher icon now matches the optical size and padding of the color icon (#50)
