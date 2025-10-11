# Chunked Decrypt V2 Proposal

## Motivation
Current portable vault encryption produces a single monolithic ciphertext per media asset. For thumbnails and metadata we still decrypt the entire payload once (even though we spill large files to disk). Very large videos or images (e.g. >200MB) pay an upfront cost when only header information or a keyframe is needed.

## Goals
1. Enable partial header decrypt to obtain: mime, dimensions, duration (video), orientation.
2. Enable targeted decrypt of first keyframe (video) for thumbnail without full payload.
3. Maintain AES/GCM authenticity per segment.
4. Keep backwards compatibility / allow migration from V1.

## Proposed Format (V2)
```
MAGIC(4) = 'VLT2'
VERSION(1) = 0x02
HEADER_LEN(2)  // length of serialized header block (plaintext after auth)
SEGMENT_SIZE(4) // nominal plaintext segment size (e.g. 256KB) for random access
META_AUTH_TAG(16)
[ENCRYPTED HEADER BLOCK]
[SEGMENT 0: IV(12) CIPHERTEXT ... TAG(16)]
[SEGMENT 1: IV(12) CIPHERTEXT ... TAG(16)]
...
```

### Header Block
Plaintext JSON (or concise CBOR) containing:
- originalSize
- mimeType
- width, height (if image) or video track summary (codec, duration, timescale, keyframe index offset)
- segmentCount
- optional: smallThumbnail (<= 32KB) precomputed, base64

Header is encrypted+auth separately so we can decrypt only the header to answer queries.

## Random Access
Given SEGMENT_SIZE we can compute which segment holds byte range of interest. For video, we optionally store a small index of keyframe -> segment mapping (delta compressed) inside header.

## Crypto
- Each segment uses unique IV (12 bytes). Derive via HMAC(key, segmentIndex) truncated to 12 bytes or random IV stored inline.
- AES/GCM per segment ensures tamper detection; no dependency chain.

## Migration Strategy
1. On first access of V1 asset requested for partial decode, perform full decrypt and re-encrypt as V2 if size > threshold (e.g. 8MB) and user idle + charging.
2. Maintain flag in sidecar metadata indicating V2 availability to avoid re-migrating.

## API Additions
- DecryptManagerV2 with: decryptHeader(file), decryptSegment(file, index).
- StreamingThumbnailProvider attempts: if V2 -> decryptHeader + first keyframe's segment(s); else fallback full decrypt.

## Open Questions
- Segment size: tradeoff between overhead (IV+TAG) and random access granularity.
- Include optional per-segment checksum beyond GCM? (Probably redundant.)
- Precompute small thumbnail vs. always computing first keyframe lazily.

## Next Steps
1. Prototype header/segment writer for test assets.
2. Benchmark decrypt time for header only vs full file across sizes.
3. Decide segment sizing heuristics (maybe dynamic based on file size).
4. Integrate into migration worker.
