## What's new in 5.1.2

ReFra 5.1.2 focuses on safer media editing and metadata removal, trustworthy cloud backups, and more reliable browsing across large local, NAS, JPEG XL, video, and location libraries.

### Improvements

- **Reliable native codec packaging** — Clean, CI, release, and F-Droid builds now reproducibly build and include the full native image codecs instead of silently linking fallback stubs (#1068)
- **Dependency maintenance** — Updated project dependencies, Gradle tooling, and supporting build components

### Bug Fixes

- **Trustworthy cloud backup verification** — WebDAV, SMB, and NFS backups are verified with streamed content hashes before ReFra trusts them for duplicate detection, backup status, or safe local-file deletion
- **Reliable large NAS libraries** — Large SMB and NFS shares now use a complete reusable index for stable paging, albums, covers, thumbnails, video streaming, and account-aware routing (#1104)
- **Lossless metadata removal** — Sanitized media is created and fully verified separately before an original can be moved to system trash, preventing source loss and keeping retries safe (#1124)
- **Safer editor overwrites** — Existing originals remain published and visible during replacement, with rollback restoration after partial write failures (#1131)
- **JPEG XL metadata** — Exif and XMP metadata now loads from both direct and Brotli-compressed JPEG XL container boxes (#1125)
- **Album move destinations** — Existing folders on primary and removable storage can be selected as move destinations again (#1121)
- **Deep album position restoration** — Large albums preserve deep grid and viewer positions after process or screen restoration (#965)
- **Deterministic location navigation** — Equivalent local, cloud, and map locations are merged consistently while taps open the correct scoped timeline and media
- **External video launches** — Videos opened from other apps no longer flash viewer controls over the first visible frame (#1126)
