#!/usr/bin/env bash
# Creates the Play upload keystore and writes TL_SIGN_* into local.properties.
# Back up keystore + passwords somewhere safe — losing them blocks app updates.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KEYSTORE_DIR="$ROOT/keystore"
KEYSTORE="$KEYSTORE_DIR/today-list-upload.jks"
LOCAL="$ROOT/local.properties"

mkdir -p "$KEYSTORE_DIR"

if [[ -f "$KEYSTORE" ]]; then
  echo "Keystore already exists: $KEYSTORE"
  exit 0
fi

STORE_PASS="$(openssl rand -base64 24 | tr -d '/+=' | head -c 24)"
KEY_PASS="$STORE_PASS"

keytool -genkeypair \
  -keystore "$KEYSTORE" \
  -alias todaylist \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storetype JKS \
  -storepass "$STORE_PASS" \
  -keypass "$KEY_PASS" \
  -dname "CN=Today List, OU=Mobile, O=4CTech LLC, L=Florida, ST=FL, C=US"

touch "$LOCAL"
grep -v '^TL_SIGN_' "$LOCAL" > "$LOCAL.tmp" || true
mv "$LOCAL.tmp" "$LOCAL"
{
  echo ""
  echo "# Upload keystore (do not commit)"
  echo "TL_SIGN_STORE_FILE=keystore/today-list-upload.jks"
  echo "TL_SIGN_KEY_ALIAS=todaylist"
  echo "TL_SIGN_STORE_PASSWORD=$STORE_PASS"
  echo "TL_SIGN_KEY_PASSWORD=$KEY_PASS"
} >> "$LOCAL"

echo "Created $KEYSTORE"
echo "Signing props written to local.properties"
echo "IMPORTANT: back up the keystore file and local.properties passwords."
keytool -list -v -keystore "$KEYSTORE" -alias todaylist -storepass "$STORE_PASS" \
  | grep -E 'Alias name|SHA1:|SHA256:|Valid from'
