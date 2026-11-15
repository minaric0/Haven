#!/usr/bin/env bash
set -euo pipefail
if [[ $# -ne 4 ]]; then
  echo 'Usage: bash sign-personal-apk.sh APKSIGNER_JAR UNSIGNED_APK EXTRACTED_PRIVATE_BACKUP OUTPUT_APK' >&2
  exit 2
fi
signer_jar=$1
unsigned_apk=$2
backup_dir=$3
output_apk=$4
java -jar "$signer_jar" sign \
  --ks "$backup_dir/haven-personal-release.jks" \
  --ks-key-alias haven-personal \
  --ks-pass "file:$backup_dir/keystore-password.txt" \
  --key-pass "file:$backup_dir/keystore-password.txt" \
  --v4-signing-enabled false \
  --out "$output_apk" "$unsigned_apk"
java -jar "$signer_jar" verify --verbose --print-certs "$output_apk"
