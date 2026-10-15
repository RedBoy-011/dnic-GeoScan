# dnic GeoScan (dnic Visualizer 3D)

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-blue.svg)](https://developer.android.com/jetpack/compose)
[![Visualizer](https://img.shields.io/badge/3D%20Engine-Apache%20ECharts%203D-red.svg)](https://echarts.apache.org)

> **dnic GeoScan** is a professional Android geophysical data logger and 3D visualizer app designed to replace legacy Windows software for magnetometer surface scanning.

---

## 🧭 Brand Concept: "dnic" (Digital Electronic)
* **Aesthetic**: Swiss Minimalist & Glassmorphism with deep space dark theme (`#070A12`).
* **Visual Palette**: Magnetic spectrum neon accents:
  * 🟢 **Neon Green (`#00FF9D`)**: Nominal baseline magnetic field & sensor ground reference.
  * 🔵 **Neon Cyan (`#00E5FF`)**: Negative anomalies / cavities / low magnetic flux.
  * 🔴 **Neon Red (`#FF2A55`)**: Positive anomalies / ferrous metals / peak magnetic flux.
* **Icon Concept**: Glassmorphic shield badge fusing an undulating 3D spatial terrain mesh with digital PCB circuit traces and contact vias.

---

## 🛠 Tech Stack
* **Language & Architecture**: Kotlin, Single-Activity Jetpack Compose with Material 3.
* **Coroutines & Flow**: Kotlin `SharedFlow` & `StateFlow` for zero-loss real-time telemetry streaming.
* **Connectivity**: Bluetooth Classic (SPP - Serial Port Profile) interfacing with HC-05 microcontrollers.
* **Data Protocol**: Real-time delimited string packet: `$X,Y,Z#\n`.
* **Storage**: Android Storage Access Framework (SAF) for offline dataset inspection (`.txt`, `.csv`).
* **3D Surface & Heatmap Engine**: Embedded Apache ECharts 3D via hardware-accelerated WebView.

---

## 📁 Project Architecture
```
dnic-GeoScan/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml       # Bluetooth & SAF permissions configured
│   │   ├── java/com/dnic/geoscan/
│   │   │   ├── MainActivity.kt       # Root edge-to-edge Compose activity
│   │   │   └── ui/
│   │   │       ├── components/
│   │   │       │   └── GlassmorphicCard.kt
│   │   │       ├── navigation/
│   │   │       │   ├── Screen.kt
│   │   │       │   └── AppNavigation.kt
│   │   │       ├── screens/
│   │   │       │   ├── remote/RemoteScanScreen.kt
│   │   │       │   └── local/LocalAnalysisScreen.kt
│   │   │       └── theme/
│   │   │           ├── Color.kt
│   │   │           ├── Type.kt
│   │   │           └── Theme.kt
│   │   └── res/
│   │       ├── drawable/             # Vector icons (3D grid + PCB traces)
│   │       ├── mipmap-anydpi-v26/    # Adaptive icon configuration
│   │       └── values/               # Strings, colors, base styles
│   └── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml            # Version catalog
│   └── wrapper/
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 Phases Roadmap
- [x] **Phase 1: Project Initialization & UI Architecture**
- [ ] **Phase 2: Bluetooth SPP Engine & `$X,Y,Z#\n` Parser**
- [ ] **Phase 3: Apache ECharts 3D WebView Integration & Real-time Surface Plotting**
- [ ] **Phase 4: Local File Analysis (SAF) & Interpolation Filters**
- [ ] **Phase 5: Exporting, Calibration & Production Polishing**
