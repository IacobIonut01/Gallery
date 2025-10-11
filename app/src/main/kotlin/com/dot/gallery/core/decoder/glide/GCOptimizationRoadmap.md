# GC & Memory Optimization Roadmap

This document summarizes current optimizations and proposed next steps to minimize GC pressure in the encrypted media pipeline.

## Implemented
- Threshold-based decrypt (<=2MB stays in-memory, larger assets spill to temp file once).
- Streaming model (`EncryptedMediaSource`) so Glide retries do not trigger re-decrypt.
- Unified `DecryptManager` single-flight + small-result LRU to avoid duplicate decrypt work.
- Reuse of existing temp spill files for video frame extraction; avoid second spill.
- On-demand video frame selection (time micros / percent) without additional allocations.
- Sidecar metadata cache prevents redundant decrypt just to probe mime/dimensions/duration.
- Cleanup worker prunes stale temp + sidecar entries to keep cache footprint bounded.

## Next Candidates
1. HEIF/JXL True Streaming
   - Requires upstream library support for incremental or stream-based decode.
   - Investigate contributing PRs or wrapping native decoders able to operate on file descriptors.
2. Chunked Video Decrypt
   - For very large videos, decrypt only header + keyframe region for thumbnail extraction.
   - Needs container parsing (MP4/MOV boxes) to locate moov + keyframe tables.
3. Metadata Pre-Index Job
   - Batch pass that decrypts headers only and populates sidecar upfront (Warm start advantage).
   - Rate limit + run during device idle/charging.
4. Adaptive Threshold
   - Dynamically lower in-memory threshold under memory pressure (listen to ComponentCallbacks2).
5. Unified Temp File Manager
   - Track open handles, provide reference counting, coordinate cleanup worker with active usage.
6. Decoder Pool Reuse
   - Maintain reusable buffers for bounds decode and sampling to avoid transient byte[] churn.
7. Memory Diagnostics Hooks
   - Periodic logging of LRU hit ratio, decrypt dedupe rate, average spill size.

## Avoided Patterns
- Full byte[] materialization for large media beyond initial single decrypt.
- Duplicate frame extraction spills.

## Metrics Suggestions
- Record counters: decrypt_invocations, decrypt_coalesced, lru_hits, lru_misses, sidecar_reads, sidecar_writes.
- Tie into a lightweight in-app debug panel.

---
Maintainer note: Keep future changes additive; avoid destructive schema changes for sidecar storage. Consider migration to protobuf or Room if metadata fields expand substantially.
