# AI Barcode Finder Demo

## Project Purpose

**AI Barcode Finder Demo** is a sample Android application that demonstrates how to use Zebra's AI Data Capture SDK, specifically the `EntityTrackerAnalyzer` for barcode finding. This project shows how to integrate EntityTrackerAnalyzer (a CameraX `ImageAnalysis.Analyzer` exposed by Zebra's SDK) with Jetpack Compose and MVVM architecture for modern, enterprise-grade barcode scanning.

Use this project as a sample for:
- Initializing and configuring Zebra's AI Data Capture SDK
- Processing camera frames with EntityTrackerAnalyzer
- Displaying real-time barcode tracking results in a Compose UI
- Managing state and business logic with MVVM

## How It Works

1. **([CameraX](https://developer.android.com/media/camera/camerax)) Integration**: The app uses CameraX for camera lifecycle and frame analysis.
2. **EntityTrackerAnalyzer**: Camera frames are analyzed in real-time using Zebra's EntityTrackerAnalyzer, which detects and tracks barcodes.
3. **MVVM Architecture**: All SDK interactions are isolated in the data layer (repositories). ViewModels manage state and expose it to the UI via Kotlin Flows.
4. **Jetpack Compose UI**: The UI observes ViewModel state and displays overlays for detected barcodes. Touch interactions allow users to configure and manage barcodes.

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 34 (Android 14)
- Zebra AI Data Capture SDK ([Documentation](https://techdocs.zebra.com/enterprise-ai/vision-sdk/))

### Setup & Installation
1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd AI_Barcode_Finder
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

- **Home Screen:** Navigate between barcode finder, configuration, and settings.
- **Barcode Finder:** Point the camera at barcodes. Interactive overlays appear for detected barcodes. Touch overlays to configure actions.
- **Configure Demo:** Set up actionable barcodes and define action types (pickup, recall, quantity).
- **Scan Results:** View completed scan operations and manage barcode inventory.

## Documentation & Support
- [Zebra AI Data Capture SDK Documentation](https://techdocs.zebra.com/enterprise-ai/vision-sdk/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [CameraX Documentation](https://developer.android.com/training/camerax)
- [Android Developer Documentation](https://developer.android.com/docs)

---

*This project is a sample implementation of Zebra's EntityTrackerAnalyzer. For questions or support, please refer to Zebra's developer resources or open an issue in this repository.*
