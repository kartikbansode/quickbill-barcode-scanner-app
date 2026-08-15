<p align="center">
  <a href="https://github.com/kartikbansode/quickbill-customer-display-app">
    <img width="80" height="80" alt="logo" src="https://github.com/user-attachments/assets/eca0e1aa-8784-4a37-90d1-c6d864a53941" />
  </a>
</p>

<h1 align="center">
  QuickBill Barcode Scanner
</h1>

<p align="center">
  <strong>Android Barcode Scanning Companion for QuickBill</strong>
</p>

<p align="center">
  Fast barcode scanning · Product lookup · QuickBill integration
</p>

<p align="center">
  <a href="https://github.com/kartikbansode/quickbill">
    QuickBill Desktop
  </a>
  &bull;
  <a href="https://github.com/kartikbansode/quickbill-barcode-scanner/releases">
    Releases
  </a>
  &bull;
  <a href="https://github.com/kartikbansode/quickbill-barcode-scanner-app">
    Repository
  </a>
</p>

<p align="center">
  <a href="https://github.com/kartikbansode/quickbill-barcode-scanner-app/releases/download/v1.1.0/QuickBill-Barcode-Scanner-v1.1.0.apk">
    <strong>Download Android App v1.1.0</strong>
  </a>
  <br>
  <em>First Stable Release</em>
</p>

<p align="center">
  <a href="https://github.com/kartikbansode/quickbill-barcode-scanner/releases">
    <img
      src="https://img.shields.io/badge/Release-v1.1.0-blue"
      alt="Release v1.1.0"
    />
  </a>

  <a href="https://developer.android.com/about/versions">
    <img
      src="https://img.shields.io/badge/Android-8.0%2B-green"
      alt="Android 8.0+"
    />
  </a>

  <a href="https://kotlinlang.org/">
    <img
      src="https://img.shields.io/badge/Kotlin-Android-purple"
      alt="Kotlin"
    />
  </a>

  <a href="https://developer.android.com/develop/ui/compose">
    <img
      src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-orange"
      alt="Jetpack Compose"
    />
  </a>
  <a href="https://github.com/kartikbansode/quickbill-barcode-scanner-app/blob/main/LICENSE">
  <img
    src="https://img.shields.io/badge/LICENSE-green"
    alt="License"
  />
  </a>
</p>

<p align="center">
  QuickBill Barcode Scanner is a companion Android application designed
  to work with the QuickBill Desktop billing system. It provides a
  dedicated mobile camera streaming interface for scanning
  products and streaming camera frames directly to the desktop application
  for high-performance barcode detection in billing workflows.
</p>

<p align="center">
  <strong>Part of the QuickBill Ecosystem</strong>
  <br>
  QuickBill Desktop remains the core billing system and authoritative
  source for products, inventory, bills, payments, and barcode detection.
</p>

<p align="center">
  <a href="https://github.com/kartikbansode/quickbill">
    QuickBill Desktop
  </a>
  &nbsp;·&nbsp;
  Core desktop billing and inventory management application
  <br>
  <a href="https://github.com/kartikbansode/quickbill-customer-display-app">
    QuickBill Customer Display
  </a>
  &nbsp;·&nbsp;
  Companion Android application for real-time customer-facing billing,
  payment, QR and transaction display
  <br>
  <a href="https://quickbill.kartikbansode.dev/documentation">
    Official Documentation
  </a>
</p>

## What's New in v1.1.0

- **Professional UI/UX Redesign:** Clean, restrained enterprise companion interface optimized for barcode scanning utility.
- **Robust Settings & Back Navigation:** Back navigation from Settings smoothly returns to the camera view without closing the app or interrupting the stream.
- **Optimized CameraX ImageAnalysis Pipeline:** Low-overhead background frame processing powering high-efficiency MJPEG HTTP streaming to QuickBill Desktop.
- **Configurable Stream Quality & FPS:** Customize JPEG quality (50%, 72%, 90%) and frame rates (8, 12, 24, 30 FPS).
- **Intelligent Local IP Detection:** Prioritizes local Wi-Fi interfaces with auto-recovery and live connection status indicators.
- **Theme & Accent Customization:** Full support for System, Light, and Dark themes with customizable accent colors.
- **Camera Selection & Torch Controls:** Quick toggling between Back and Front cameras with built-in torch assistance.
- **Keep Screen Awake:** Optional display awake management while scanning.
- **One-Tap Stream URL Copy:** Fast clipboard copy for effortless pairing with QuickBill Desktop.
- **Official QuickBill Brand Identity:** Fully integrated official branding and links to Documentation, Privacy, and Terms.

---

## Features

- Android Camera Barcode Scanner
- Low-latency Network MJPEG Camera Streaming
- QuickBill Desktop Integration (HTTP MJPEG on port 8080)
- Automatic Wi-Fi IP Address Detection
- Configurable Stream FPS & Image Quality
- Camera Selection (Front / Back)
- Integrated Camera Torch Support
- Keep Screen Awake Option
- Custom Theme & Accent Color Customization
- Instant Settings Persistence & Reset Option
- Low CPU & Battery Usage
- Professional Commercial POS UI

---

## Requirements

- Android 8.0 (API 26) or higher
- Android device with camera
- QuickBill Desktop
- Android device and desktop connected to the same local network

---

## Installation

Download and install the latest APK on your Android device.

Open the app and configure the scanner settings. The displayed device/network address can be configured in QuickBill Desktop under Scanner Settings.

---

## QuickBill Desktop

This application is designed to work with:

**QuickBill Desktop Billing System**

The Android device acts as a wireless barcode scanner for the desktop billing application.

---

## Official Links

- **Documentation:** https://quickbill.kartikbansode.dev/documentation
- **Privacy Policy:** https://quickbill.kartikbansode.dev/privacy
- **Terms of Service:** https://quickbill.kartikbansode.dev/terms

---

## Building

Clone the repository:

```bash
git clone https://github.com/kartikbansode/quickbill-barcode-scanner-app.git
```

---

## License

This project is proprietary software.

Copyright © 2026 Kartik Bansode. All Rights Reserved.

The source code is publicly available for viewing, educational, portfolio, and evaluation purposes only.

No permission is granted to copy, reproduce, modify, redistribute, republish, commercially use, sublicense, sell, or create derivative works from this software or its source code without prior written permission from the copyright holder.

For complete terms and restrictions, see the [LICENSE](https://github.com/kartikbansode/quickbill-barcode-scanner/blob/main/LICENSE) file.

---

## Contact

**LinkedIn**  
https://www.linkedin.com/in/kartikbansode

**GitHub**  
https://github.com/kartikbansode
