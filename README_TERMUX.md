# ME Tekstil AI — Termux

## 1) Prepare
```bash
cd ME-Tekstil-AI
bash scripts/termux_prepare.sh
```

## 2) Build
```bash
bash scripts/build_debug.sh
```

The APK is expected at:
`android-app/app/build/outputs/apk/debug/app-debug.apk`

If Android SDK/Gradle dependencies are unavailable in the current Termux environment,
the script prints the exact missing prerequisite instead of silently claiming success.
