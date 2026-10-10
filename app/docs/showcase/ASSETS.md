# Docs preview photo fixtures

Bundled demo photographs used by the docs scene registry
(`commonMain/composeResources/drawable/`). These are real photographs derived
from the licensed sources already cached in `testing/generated/sources/` — no
random web images, no personal photos, nothing fetched at preview runtime.

| Resource | Derived from | License | Processing |
| --- | --- | --- | --- |
| `docs_lake.jpg` | `moraine-lake.jpg` (Wikimedia, Moraine_Lake_17092005.jpg) | Public domain | Downscaled to 1280px max edge, JPEG q88 |
| `docs_earth.jpg` | `apollo17.jpg` (NASA, The Earth seen from Apollo 17) | Public domain | Downscaled to 1280px max edge, JPEG q88 |
| `docs_flower.jpg` | `flower.mp4` (MDN shared-assets) | CC0 | Frame extracted at 0.7s, 720px wide JPEG |
| `docs_flower_detail.jpg` | `flower.mp4` (MDN shared-assets) | CC0 | Frame extracted at 1.2s, 720px wide, centre-cropped 70% for a "detail" shot |

Exact source URLs and SHA256 hashes live in `testing/generate-fixtures.sh`
and `testing/generated/sources.tsv`. The same credits are published on the
docs site as `public/preview-credits.txt`.

| `docs_venice_panorama.jpg` (androidMain `files/`) | `venice_sunset_1k.hdr` (Poly Haven, https://polyhaven.com/a/venice_sunset) | CC0 | Tone-mapped (ffmpeg `tonemap=gamma:param=2.2`) to a 2048×1024 2:1 JPEG for the equirectangular panorama scene |

`docs_flower.mp4` (androidMain `files/`) is the CC0 MDN flower clip itself —
the same source `docs_flower*.jpg` frames are extracted from.

| `docs_motion_photo.jpg` (androidMain `files/`) | `golden-gate.jpg` (Wikimedia, Diliff) still + `flower.mp4` (MDN) clip, built by `testing/generate-fixtures.sh` | CC BY-SA 4.0 / CC0 | Verbatim copy of `testing/generated/cloud/Camera/motion-photo.jpg` — JPEG still with GCamera MicroVideo XMP and the appended 2s MP4 |

| `docs_astronaut.png` (androidMain `files/`) | NASA portrait of Eileen Collins by Robert Markowitz, distributed as `skimage.data.astronaut()` in scikit-image v0.18.3 (https://raw.githubusercontent.com/scikit-image/scikit-image/v0.18.3/skimage/data/astronaut.png; https://scikit-image.org/docs/stable/api/skimage.data.html) | Public domain | Verbatim 512×512 PNG; the people scenes crop the face region at runtime for the sample thumbnail |
