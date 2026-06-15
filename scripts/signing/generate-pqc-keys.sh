#!/usr/bin/env bash
#
# generate-pqc-keys.sh  (Issue #901)
#
# One-time, LOCAL key setup for hybrid (classical + ML-DSA) APK signing of
# GitHub releases. Run this on a secure machine; never commit the outputs.
#
# What it does today (runnable):
#   1. Generates a NEW classical RSA key (you cannot reuse the old one).
#   2. Builds a SigningCertificateLineage proving old-key -> new-key, so
#      existing installs accept updates signed with the new key.
#
# What is stubbed (pending public tooling):
#   3. ML-DSA (PQC) key generation + pairing. Android 17 exposes ML-DSA-65 /
#      ML-DSA-87 via KeyPairGenerator (Android Keystore), but the apksigner CLI
#      flags to attach an ML-DSA signer to an APK are not documented publicly
#      yet. The hook below is intentionally left as a TODO.
#
# Usage:
#   APKSIGNER=$ANDROID_HOME/build-tools/37.0.0/apksigner \
#   OLD_KEYSTORE=app/release_key.jks OLD_ALIAS=<old> \
#     scripts/signing/generate-pqc-keys.sh
#
set -euo pipefail

APKSIGNER="${APKSIGNER:-apksigner}"
OLD_KEYSTORE="${OLD_KEYSTORE:?set OLD_KEYSTORE to the current release_key.jks}"
OLD_ALIAS="${OLD_ALIAS:?set OLD_ALIAS to the current key alias}"

NEW_KEYSTORE="${NEW_KEYSTORE:-new_release.jks}"
NEW_ALIAS="${NEW_ALIAS:-refra-new}"
LINEAGE="${LINEAGE:-refra.lineage}"
VALIDITY_DAYS="${VALIDITY_DAYS:-10000}"

echo "==> 1/3 Generating new classical RSA-4096 key ($NEW_KEYSTORE / $NEW_ALIAS)"
if [[ -f "$NEW_KEYSTORE" ]]; then
  echo "    $NEW_KEYSTORE already exists, skipping keytool generation."
else
  keytool -genkeypair -v \
    -keystore "$NEW_KEYSTORE" \
    -alias "$NEW_ALIAS" \
    -keyalg RSA -keysize 4096 \
    -validity "$VALIDITY_DAYS"
fi

echo "==> 2/3 Building rotation lineage ($LINEAGE): $OLD_ALIAS -> $NEW_ALIAS"
"$APKSIGNER" rotate \
  --out "$LINEAGE" \
  --old-signer --ks "$OLD_KEYSTORE" --ks-key-alias "$OLD_ALIAS" \
  --new-signer --ks "$NEW_KEYSTORE" --ks-key-alias "$NEW_ALIAS"

echo "==> 3/3 ML-DSA (PQC) key generation: PENDING"
cat <<'EOF'
    The ML-DSA signing key cannot be generated/paired with the public CLI yet.
    Track Android 17 build-tools apksigner support for ML-DSA-65 / ML-DSA-87.
    Once available, generate the PQC key and update pqc-hybrid-sign.sh to pass it.
EOF

echo
echo "Done. Base64-encode and store as GitHub secrets (DO NOT COMMIT):"
echo "    base64 -i $NEW_KEYSTORE  -> secret SIGNING_KEY_NEW"
echo "    base64 -i $LINEAGE       -> secret SIGNING_LINEAGE"
echo "Also record fingerprints for release notes:"
echo "    keytool -list -v -keystore $NEW_KEYSTORE -alias $NEW_ALIAS"
