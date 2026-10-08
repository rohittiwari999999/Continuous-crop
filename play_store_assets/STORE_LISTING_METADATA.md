# Google Play Console Store Listing Metadata

## 1. App Details
- **App Name (Title - Max 30 chars):** `ContinuousCrop`
- **Short Description (Max 80 chars):** `High-speed continuous image cropping & resizing with instant background saves.`
- **Default Language:** English (United States) (en-US)
- **Category:** Photography / Tools
- **Tags / Keywords:** Image Crop, Batch Crop, Photo Resizer, Fast Workflow, MediaStore Scoped Storage, Photo Editor, Continuous Extraction

---

## 2. Full Description (Max 4000 characters)

```markdown
ContinuousCrop is an ultra-fast, professional image cropping and resizing workstation designed for creators, designers, researchers, and photographers who need to extract multiple cropped sections from high-resolution images rapidly — without the frustration of constant save dialogs or screen transitions.

⚡ ZERO-INTERRUPT CONTINUOUS WORKFLOW
Tired of apps that close the editor every time you save a crop? With ContinuousCrop, your cropping bounding box remains fully active and draggable on the canvas. Simply frame a region, tap "Crop & Save", and the app immediately saves your cropped section to device storage in the background while you slide the box to the next area.

🎞️ STICKY SAVED RIBBON (LIVE THUMBNAILS)
A sticky mini-map horizontal ribbon at the top of your screen automatically displays small, high-fidelity thumbnails of your saved crops in real-time as background I/O operations complete. Inspect, share, or delete any crop with a single tap.

📐 PRECISION HARDWARE-ACCELERATED WORKSTATION
• 8 Interactive Touch Handles: 4 corner anchors + 4 edge handles with generous touch targets.
• Center Panning: Move the crop box effortlessly anywhere across the image.
• Rule-of-Thirds Grid: Align compositions perfectly with grid modes (Always, Drag Only, Hidden).
• Live Dimension Badges: Real-time pixel width and height indicators right on the canvas.
• Aspect Ratio Options: Free (unconstrained), 1:1 Square, 4:3, 16:9, 3:4, 9:16, 3:2, 2:3.
• Instant Transforms: Rotate 90° clockwise, flip horizontally, or reset framing in one tap.

💾 CONFIGURABLE OUTPUT QUALITY & FORMATS
• Formats: JPEG (Standard), PNG (Lossless transparent), and WEBP (Modern high efficiency).
• Quality Tuning: Precision slider from 50% to 100% compression quality.
• Scale Downsampling: Export at Original 100%, 75%, 50%, Max 1080p, or Max 720p.

📁 PRIVACY-FIRST SCOPED STORAGE
• Saves directly to your device Gallery (`Pictures/ContinuousCrop`) using Android MediaStore Scoped Storage API.
• Zero broad storage permissions needed — completely safe, private, and offline.
• No cloud uploads, no third-party tracking, and no ads.

Elevate your photo extraction workflow with ContinuousCrop today!
```

---

## 3. Graphic Assets Checklist for Play Console

All graphic assets are generated and saved in `/play_store_assets/`:

| Asset | File Name | Required Dimensions | Format |
|---|---|---|---|
| **App Icon** | `app_icon_512x512.png` | 512 × 512 px | 32-bit PNG, max 1MB |
| **Feature Graphic** | `feature_graphic_1024x500.png` | 1024 × 500 px | 24-bit PNG/JPEG, max 15MB |
| **Phone Screenshot 1** | `screenshot_1_workstation_1080x1920.png` | 1080 × 1920 px (16:9 / 9:16) | PNG |
| **Phone Screenshot 2** | `screenshot_2_top_ribbon_1080x1920.png` | 1080 × 1920 px (16:9 / 9:16) | PNG |
| **Phone Screenshot 3** | `screenshot_3_settings_1080x1920.png` | 1080 × 1920 px (16:9 / 9:16) | PNG |
| **Phone Screenshot 4** | `screenshot_4_detail_share_1080x1920.png` | 1080 × 1920 px (16:9 / 9:16) | PNG |

---

## 4. Built Installables & Upload Bundles

All built binaries are ready in `/dist/`:
- **Android App Bundle (AAB for Play Console):** `dist/ContinuousCrop-v1.0.aab`
- **Standalone APK (Direct Sideload & Testing):** `dist/ContinuousCrop-v1.0.apk`

---

## 5. Content Rating & Policy Declarations
- **Violence, Sexuality, Crude Language:** None (Rated PEGI 3 / Everyone).
- **Data Safety:** App does NOT collect, share, or transmit user data. All processing occurs locally on device.
- **Permissions:** Standard Scoped Storage (`MediaStore.Images.Media`), Zero dangerous permissions.
