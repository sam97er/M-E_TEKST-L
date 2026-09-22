#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/android-app"

echo "[1/3] Checking Java..."
command -v java >/dev/null || { echo "Java is missing. Install a JDK first."; exit 1; }

echo "[2/3] Checking Android SDK variables..."
if [ -z "${ANDROID_HOME:-}" ] && [ -z "${ANDROID_SDK_ROOT:-}" ]; then
  echo "ANDROID_HOME/ANDROID_SDK_ROOT is not set."
  echo "Set it to your Android SDK path, then run this script again."
  exit 1
fi

echo "[3/3] Checking Gradle wrapper..."
if [ ! -x "./gradlew" ]; then
  echo "Gradle wrapper executable is missing from android-app."
  echo "Use Android Studio/Gradle to generate the wrapper, or install Gradle and run:"
  echo "  gradle wrapper"
  exit 1
fi

echo "Preparation checks passed."
