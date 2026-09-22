# ME Tekstil AI — GitHub Actions

The repository-level workflow is:
`.github/workflows/android-build.yml`

It builds the Android project in `android-app/` on GitHub using:
- JDK 17
- Android SDK 36
- Gradle 9.3.1
- unit tests
- debug APK artifact upload

The workflow creates a temporary debug keystore for the debug build when needed.

No API keys are stored in GitHub. API credentials are intended to be entered inside the app Settings screen.

The workflow is deliberately debug-first. Release signing will be added only after the debug build is green.
