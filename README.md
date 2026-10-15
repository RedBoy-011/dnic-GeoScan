# denic GeoScan (denic Visualizer 3D)

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.0-purple.svg)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-blue.svg)](https://developer.android.com/jetpack/compose)
[![Visualizer](https://img.shields.io/badge/3D%20Engine-Apache%20ECharts%203D-red.svg)](https://echarts.apache.org)
[![Build](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()

> **denic GeoScan** is a professional Android geophysical data logger and 3D visualizer app designed to replace legacy Windows software for magnetometer surface scanning.

---

## 🧭 Brand Concept: "denic" (Digital Electronic)
* **Aesthetic**: Swiss Minimalist & Glassmorphism with deep space dark theme (`#070A12`) and responsive multi-pane layout for phones and fieldwork tablets.
* **Visual Palette**: Magnetic spectrum neon accents:
  * 🟢 **Neon Green (`#00FF9D`)**: Nominal baseline magnetic field & sensor ground reference (Ground Zero).
  * 🔵 **Neon Cyan (`#00E5FF`)**: Negative anomalies / cavities / tunnels / low magnetic flux.
  * 🔴 **Neon Red (`#FF2A55`)**: Positive anomalies / ferromagnetic targets / peak magnetic flux.
* **Icon Concept**: Glassmorphic shield badge fusing an undulating 3D spatial terrain mesh with digital PCB circuit traces and contact sensor vias.

---

## 🛠 Tech Stack & Architecture
* **Language & Architecture**: Kotlin 2.0, Single-Activity Jetpack Compose with Material 3.
* **Coroutines & Flow**: Kotlin `SharedFlow` & `StateFlow` for zero-loss real-time telemetry streaming from hardware.
* **Hardware Connectivity**: Bluetooth Classic (SPP - Serial Port Profile, UUID `00001101-0000-1000-8000-00805F9B34FB`) interfacing with HC-05 microcontrollers.
* **Data Protocol**: Real-time delimited string packet: `$X,Y,Z#\n`.
* **Storage Access**: Android Storage Access Framework (SAF) for offline dataset inspection (`.txt`, `.csv`) and MediaStore export to `Documents/denic-GeoScan`.
* **3D Surface & Heatmap Engine**: Embedded, 100% offline Apache ECharts 5.5 + ECharts-GL 2.0.9 via hardware-accelerated WebView (`LAYER_TYPE_HARDWARE`).
* **Mathematical Filtering**: Mathematical Bicubic Convolution Interpolation (Catmull-Rom spline, $a = -0.5$) for converting discrete sensor steps into smooth, continuous topographical 3D surfaces.

---

## 📁 Project Architecture
```
denic-GeoScan/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Bluetooth SPP & SAF permissions configured
│   │   ├── assets/
│   │   │   └── echarts/
│   │   │       ├── echarts.min.js       # Offline Apache ECharts 5.5.0
│   │   │       ├── echarts-gl.min.js    # Offline ECharts-GL 2.0.9
│   │   │       └── index.html           # 3D Surface & 2D Heatmap renderer + JS Bridge
│   │   ├── java/com/denic/geoscan/
│   │   │   ├── MainActivity.kt          # Root edge-to-edge Compose activity
│   │   │   ├── data/
│   │   │   │   ├── analysis/
│   │   │   │   │   ├── BicubicInterpolator.kt   # Bicubic mathematical smoothing filter
│   │   │   │   │   └── DatasetNormalizer.kt     # Median Ground Zero baseline normalizer
│   │   │   │   ├── bluetooth/
│   │   │   │   │   ├── BluetoothConnectionState.kt
│   │   │   │   │   ├── BluetoothDeviceModel.kt
│   │   │   │   │   └── BluetoothService.kt      # SPP Coroutine & SharedFlow engine
│   │   │   │   ├── model/
│   │   │   │   │   ├── ScanPoint.kt
│   │   │   │   │   └── NormalizedScanPoint.kt
│   │   │   │   ├── parser/
│   │   │   │   │   └── ScanDataParser.kt        # Robust $X,Y,Z# packet reassembler
│   │   │   │   └── storage/
│   │   │   │       ├── LocalFileParser.kt       # Triplet and 2D matrix CSV/TXT parser
│   │   │   │       └── SessionExporter.kt       # MediaStore CSV session exporter
│   │   │   └── ui/
│   │   │       ├── components/
│   │   │       │   └── GlassmorphicCard.kt
│   │   │       ├── navigation/
│   │   │       │   ├── Screen.kt
│   │   │       │   └── AppNavigation.kt
│   │   │       ├── screens/
│   │   │       │   ├── remote/
│   │   │       │   │   ├── RemoteScanScreen.kt
│   │   │       │   │   └── RemoteScanViewModel.kt
│   │   │       │   └── local/
│   │   │       │       ├── LocalAnalysisScreen.kt
│   │   │       │       └── LocalAnalysisViewModel.kt
│   │   │       ├── theme/
│   │   │       │   ├── Color.kt
│   │   │       │   ├── Type.kt
│   │   │       │   └── Theme.kt
│   │   │       └── visualizer/
│   │   │           ├── EChartsVisualizer.kt
│   │   │           └── VisualizerViewMode.kt
│   │   └── res/
│   │       ├── drawable/                # 3D grid + PCB traces vector assets
│   │       ├── mipmap-anydpi-v26/       # Adaptive icons
│   │       └── values/                  # Strings, colors, base styles
│   └── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml               # Gradle version catalog
│   └── wrapper/
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 Phases Roadmap
- [x] **Phase 1: Project Initialization & UI Architecture**
- [x] **Phase 2: Bluetooth SPP Engine & `$X,Y,Z#\n` Parser**
- [x] **Phase 3: Local CSV/TXT File Parser (SAF) & Baseline Normalization**
- [x] **Phase 4: Apache ECharts 3D Engine & Heatmap**
- [x] **Phase 5: Bicubic Interpolation & CSV Session Export**
