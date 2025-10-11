# HEIF / JXL Streaming Limitations

## Current State
- HEIF decoding uses `HeifCoder` which expects full byte array input for size probe and sampled decode.
- JXL decoding via `JxlCoder` also operates on full in-memory byte arrays (`getSize`, `decodeSampled`).
- Our bridge decoders (`HeifEncryptedSourceDecoder`, `JxlEncryptedSourceDecoder`) adapt `EncryptedMediaSource` by materializing bytes when necessary.

## Limitations
1. No InputStream or file descriptor API exposed by the current libraries; prevents incremental decode.
2. Necessitates full decrypt for large HEIF/JXL media even if only a downsampled preview is needed (though we spill to temp file to reduce heap impact).
3. Prevents partial header decrypt optimization proposed for V2 encryption format until underlying codec supports stream fragments or random access.

## Potential Approaches
| Approach | Effort | Risk | Notes |
|----------|-------|------|------|
| Contribute stream API to HeifCoder | Medium | Medium | Requires native layer changes; must keep compatibility. |
| Wrap native decoder accepting file path | Low | Low | If underlying native lib can open files directly; investigate. |
| Use platform `ImageDecoder` for HEIF (API 28+) | Low | Medium | Might lose some performance or features; test quality/perf. |
| External JXL native integration (CJXL / DJXL) with streaming | High | High | Larger maintenance burden. |

## Near-Term Mitigations
- Keep adaptive decrypt threshold to avoid holding huge HEIF/JXL in memory; rely on spill file.
- Consider precomputing a small JPEG thumbnail during encryption/migration to bypass heavy first decode.

## Metrics to Add Later
- Count of HEIF/JXL full decrypts vs. total views.
- Average decode time for HEIF/JXL vs JPEG/PNG to justify stream investment.

## Decision Log
No immediate refactor until measurable pain (OOM frequency or decode latency) justifies native stream work.
