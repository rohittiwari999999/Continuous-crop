# SnapCrop — Google Play Store Listing & Metadata

## 1. App Title & Descriptions
- **App Title (Max 30 characters)**:
  `SnapCrop: Fast Photo Cropper`

- **Short Description (Max 80 characters)**:
  `Continuous high-speed photo crop workstation with instant background saves.`

- **Full Description (Max 4,000 characters)**:
  ```markdown
  SnapCrop is the ultimate high-speed continuous image cropping and extraction workstation designed for creators, editors, researchers, and professionals who need to cut multiple sections from images rapidly.

  ⚡ WORKFLOW WITHOUT INTERRUPTION
  Forget tedious cropping workflows where every single crop forces you through save dialogs, full-screen transitions, and resets. With SnapCrop:
  1. Open any photo from your Gallery or take a shot with the Camera.
  2. Drag the intuitive crop frame to your desired region.
  3. Tap "Crop & Save" — the crop is instantly processed and saved to your device in the background.
  4. Immediately drag the frame to the next area and tap "Crop & Save" again!

  📸 TOP SAVED ITEMS RIBBON (LIVE MINI-MAP)
  A sticky thumbnail ribbon at the top of your screen dynamically appends each saved crop in real-time as background processing completes. Tap any thumbnail to view full details, share, or delete.

  ✨ PRO TOOLS & ASPECT RATIOS
  • Free unconstrained crop box
  • Preset aspect ratios: 1:1 Square, 4:3, 3:4, 16:9, 9:16
  • 90° Clockwise Rotation & Horizontal Mirror Flip
  • Resolution resizing: 100% Original, 75%, 50%, 25%
  • Rule-of-Thirds composition grid with live alignment guides
  • Real-time pixel resolution badge

  🔒 ZERO EXCESS PERMISSIONS & PRIVACY FIRST
  • Uses Android's Zero-Permission Photo Picker
  • Saves directly to your device storage (Pictures/SnapCrop) using the official Android MediaStore API
  • Works completely offline with zero tracking, zero accounts, and zero cloud uploads
  • Full support for Android 15 & Target SDK 36
  ```

---

## 2. Store Categorization & Tags
- **Application Type**: App
- **Primary Category**: Photography
- **Secondary Category**: Tools / Productivity
- **Tags**: Photo Editor, Image Cropper, Fast Crop, Batch Crop, Photo Resizer, Grid Crop

---

## 3. Store Visual Asset Specifications (Play Console)
- **App Icon**: 512 × 512 px, 32-bit PNG with alpha, max 1MB
- **Feature Graphic**: 1024 × 500 px, JPG or 24-bit PNG (no alpha), max 15MB
- **Phone Screenshots**:
  - Minimum: 2 screenshots
  - Recommended: 4 to 8 screenshots (1080 × 2400 px or 1080 × 1920 px)
  - Key screens:
    1. Initial Screen: One-tap Gallery and Camera selection
    2. Continuous Crop Workstation: Interactive bounding box & live pixel badge
    3. Top Saved Crops Ribbon: Multi-crop thumbnails mini-map
    4. Pro Settings Screen: Formats (JPEG, PNG, WebP), haptics, storage folder
- **7-inch & 10-inch Tablet Screenshots**: Supported via adaptive Compose layout

---

## 4. Privacy & Permissions Declarations
- **Data Safety**:
  - Personal Data Collected: None
  - Data Shared with Third Parties: None
  - Location / Financial / Health Data: None
- **Android Permissions Declared**:
  - `android.permission.VIBRATE`: Normal install-time permission for tactile haptic feedback on saves.
  - Storage: Zero broad storage permissions needed. Uses Android Scoped Storage (`MediaStore.Images.Media`).
