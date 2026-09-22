#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/android-app"

[ -x "./gradlew" ] || { echo "Missing android-app/gradlew"; exit 1; }

./gradlew --no-daemon clean testDebugUnitTest assembleDebug

APK="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK" ]; then
  echo "BUILD SUCCESS"
  echo "APK: $ROOT/android-app/$APK"
else
  echo "Build command finished but APK was not found: $APK"
  exit 1
fi
