# SnapCrop — APK Location & Build Distribution Guide

## 1. APK Locations (Dist paths)
When you build the app using Gradle or Android Studio, the compiled APK binaries are generated in the following directories:

### Debug APK (Test / Development)
- **Path**:
  `app/build/outputs/apk/debug/app-debug.apk`
- **Use Case**: Direct sideloading onto physical Android devices or test devices. Pre-signed with Android debug keystore.

### Release APK (Google Play / Production)
- **Path**:
  `app/build/outputs/apk/release/app-release-unsigned.apk`
- **Use Case**: Production distribution or Google Play Store (once signed with your upload keystore).

---

## 2. Generating the APK in this Environment
To trigger a fresh build of the APK, run:
```bash
gradle :app:assembleDebug
```
or for release:
```bash
gradle :app:assembleRelease
```

---

## 3. How to Export / Download from AI Studio
1. Open the project settings or header menu in the top right of Google AI Studio.
2. Select **"Export project (ZIP)"** to download the complete Android source code and assets.
3. You can also generate and download APKs directly from the AI Studio build menu.
4. Open the project in **Android Studio** anytime: simply open the root folder, and Gradle will automatically sync and build.

---

## 4. Play Console Target SDK & Compatibility
- **Compile SDK**: 36
- **Target SDK**: 36 (Fully compliant with Google Play target API level requirements)
- **Min SDK**: 24 (Android 7.0 Nougat and above — covers 96%+ of active Android devices worldwide)
