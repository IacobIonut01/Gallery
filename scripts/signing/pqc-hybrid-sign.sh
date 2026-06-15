#!/usr/bin/env bash
#
# pqc-hybrid-sign.sh  (Issue #901)
#
# Re-signs already-built release APKs with the rotated classical key + lineage,
# then verifies. Designed to run as a CI step AFTER `./gradlew assemble...Release`
# (see PQC_HYBRID_SIGNING_PLAN.md) or locally.
#
# Runnable today: classical key rotation + lineage + verification.
# Stubbed (pending public apksigner ML-DSA flags): the PQC signer leg.
#
# Required env:
#   APKSIGNER                path to Android 17 build-tools apksigner
#   NEW_KEYSTORE             rotated classical keystore (e.g. new_release.jks)
#   NEW_ALIAS                alias in NEW_KEYSTORE
#   NEW_STORE_PASSWORD       keystore password
#   NEW_KEY_PASSWORD         key password
#   LINEAGE                  rotation lineage file (refra.lineage)
# Optional:
#   APK_DIR                  dir to scan for *.apk (default: app/build/outputs/apk-renamed)
#
set -euo pipefail

APKSIGNER="${APKSIGNER:?set APKSIGNER to the Android 17 build-tools apksigner}"
NEW_KEYSTORE="${NEW_KEYSTORE:?set NEW_KEYSTORE}"
NEW_ALIAS="${NEW_ALIAS:?set NEW_ALIAS}"
NEW_STORE_PASSWORD="${NEW_STORE_PASSWORD:?set NEW_STORE_PASSWORD}"
NEW_KEY_PASSWORD="${NEW_KEY_PASSWORD:?set NEW_KEY_PASSWORD}"
LINEAGE="${LINEAGE:?set LINEAGE to the rotation lineage file}"
APK_DIR="${APK_DIR:-app/build/outputs/apk-renamed}"

# Marked TODO: append the ML-DSA signer once apksigner exposes the flag.
# Expected to look roughly like a second --next-signer with the ML-DSA key,
# or a dedicated hybrid flag. Keep empty until confirmed against the SDK.
PQC_SIGNER_ARGS=()

mapfile -t APKS < <(find "$APK_DIR" -name "*.apk" -type f | sort)
if [[ ${#APKS[@]} -eq 0 ]]; then
  echo "No APKs found under $APK_DIR" >&2
  exit 1
fi

for apk in "${APKS[@]}"; do
  echo "==> Signing $apk"
  "$APKSIGNER" sign \
    --ks "$NEW_KEYSTORE" \
    --ks-key-alias "$NEW_ALIAS" \
    --ks-pass "pass:$NEW_STORE_PASSWORD" \
    --key-pass "pass:$NEW_KEY_PASSWORD" \
    --lineage "$LINEAGE" \
    "${PQC_SIGNER_ARGS[@]}" \
    "$apk"

  echo "==> Verifying $apk"
  "$APKSIGNER" verify --verbose --print-certs "$apk"
done

echo "All APKs signed and verified."
if [[ ${#PQC_SIGNER_ARGS[@]} -eq 0 ]]; then
  echo "NOTE: classical+lineage only. ML-DSA leg not yet attached (see TODO)."
fi
