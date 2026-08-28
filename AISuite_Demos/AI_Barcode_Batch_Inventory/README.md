# AI Barcode Batch Inventory Demo

## Demo Overview

<p align="center">
  <img src="demo.gif" alt="AI Barcode Batch Inventory demo" width="300">
</p>

Review the current SDK configuration — model input size, camera resolution, and inference (processor) type — then tap **Start Scan** and point the camera at a shelf of barcodes. Detected barcodes are tracked with real-time overlays and collected into the inventory results.

## Project Purpose

**AI Barcode Batch Inventory Demo** is a sample Android application that demonstrates how to use Zebra's AI Data Capture SDK, specifically the `EntityTrackerAnalyzer` for real-time barcode detection in a batch inventory workflow. Point the camera at a shelf of barcodes, capture a scan session, and review the collected inventory results. This project shows how to integrate EntityTrackerAnalyzer (a CameraX `ImageAnalysis.Analyzer` exposed by Zebra's SDK) with Jetpack Compose and MVVM architecture for modern, enterprise-grade batch inventory scanning.

Use this project as a sample for:
- Initializing and configuring Zebra's AI Data Capture SDK
- Processing camera frames with EntityTrackerAnalyzer
- Displaying real-time barcode tracking results in a Compose UI
- Managing scan sessions, results, and state with MVVM

## How It Works

1. **([CameraX](https://developer.android.com/media/camera/camerax)) Integration**: The app uses CameraX for camera lifecycle and frame analysis.
2. **EntityTrackerAnalyzer**: Camera frames are analyzed in real-time using Zebra's EntityTrackerAnalyzer, which detects and tracks barcodes during a batch capture session.
3. **MVVM Architecture**: All SDK interactions are isolated in the data layer (repositories). ViewModels manage state and expose it to the UI via Kotlin Flows.
4. **Jetpack Compose UI**: The UI observes ViewModel state and displays real-time overlays for detected barcodes and the collected inventory results.

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android device running Android 13 (API 33) or later with a rear camera
- Zebra AI Data Capture SDK ([Documentation](https://techdocs.zebra.com/ai-datacapture/latest/about/))

### Setup & Installation
1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd AI_Barcode_Batch_Inventory
   ```
2. **Open in Android Studio:**
   - Select "Open an Existing Project" and choose the project directory.
3. **Build the project:**
   ```bash
   ./gradlew build
   ```
4. **Run on device:**
   - Connect an Android device with a camera and run the app from Android Studio.

## Usage Overview

- **Home Screen:** Entry point. Initializes the SDK and requests camera permission.
- **Finder Screen:** Live camera view. Press the capture button to start a scan session. Detected barcodes appear as overlays in real time, and results are saved automatically when the capture window completes.
- **Scan Results:** Review the list of captured barcodes with quantities, and resume scanning to add more.
- **Settings:** Configure SDK parameters — model input size, camera resolution, processor type (DSP/AUTO), and enabled barcode symbologies.

## Documentation & Support
- [Zebra AI Data Capture SDK Documentation](https://techdocs.zebra.com/ai-datacapture/latest/about/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [CameraX Documentation](https://developer.android.com/training/camerax)
- [Android Developer Documentation](https://developer.android.com/docs)

---

*This project is a sample implementation of Zebra's EntityTrackerAnalyzer. For questions or support, please refer to Zebra's developer resources or open an issue in this repository.*
