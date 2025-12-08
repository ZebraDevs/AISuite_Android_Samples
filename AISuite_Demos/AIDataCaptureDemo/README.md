## AI Data Capture Demo Application

This application demonstrates the features available in the Zebra AI Data Capture SDK - https://techdocs.zebra.com/ai-datacapture .  The application demonstrates technology features, including **Barcode Recognizer**, **Text/OCR Recognizer**, and **Product & Shelf Localizer**, and usecase feature including **Product & Shelf Recognizer**, and **OCR Barcode Finder**. Each  feature is illustrated through a live preview that provides real-time, on-screen feedback, displaying bounding boxes around detected objects and additionally showing recognition results for Product and Shelf Recognition and OCR Text Find.

## Project Purpose
Use this project as a sample for:
- Initializing and configuring Zebra's AI Data Capture SDK
- Processing camera frames with different foundational AI models
- Displaying real-time results in a Compose UI
- Managing state and business logic with MVVM

## How It Works
1. **([CameraX](https://developer.android.com/media/camera/camerax)) Integration**: The app uses CameraX for camera lifecycle and frame analysis.
2. **EntityTrackerAnalyzer**: Camera frames are analyzed in real-time using Zebra's EntityTrackerAnalyzer, which detects and tracks barcodes.
3. **MVVM Architecture**: All SDK interactions are isolated in the "Model" layer/folder. ViewModels manage state and expose it to the UI via Kotlin Flows.
4. **Jetpack Compose UI**: UI layer/folder handles UI Screens. The UI observes ViewModel state and displays overlays which draw bounding boxes and results and handles screen transitions.

## Useful References
- [SDK Documentation](https://techdocs.zebra.com/ai-datacapture/latest/about/)
- [Models](https://techdocs.zebra.com/ai-datacapture/latest/setup/#featuresmodels)
- [Developer Experience Videos](https://www.youtube.com/zebratechnologies)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [CameraX Documentation](https://developer.android.com/training/camerax)
- [Android Developer Documentation](https://developer.android.com/docs)

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 34 (Android 14)
- Zebra AI Data Capture SDK ([Documentation](https://techdocs.zebra.com/enterprise-ai/vision-sdk/))

### Setup & Installation
1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd AIDataCaptureDemo
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
### Usecase Demos
**OCR Find + Barcode** - Optical Character and Barcode Recognition:  
- Displays text recognition and barcode results on the live viewfinder.  
- The filter icon enables the user to locate text by filtering for numeric, alphabetic, or alphanumeric content, including options for specifying size ranges or exact string matches
**Product & Shelf Recognizer** - Product Recognition:  
- Enables creation of a product index  
- Multi-step process required to create index followed displaying results on live viewfinder  
- Recognizes products with results displayed as text within each product’s bounding box.  
- Highlights enrolled products in green during enrollment; non-enrolled products do not display text results.

#### Product Recognition Settings
Product Recognition is initialized to use products.db file in internal storage folder (filesDir). User has no direct access to this folder and file.
**Import Database**
- Allows user to select a database file using the file picker feature.
- This functionality is specifically intended for those managing a list of previously enrolled product database files.
- The feature allows users to load a product database file (*.db) from local storage.
- Once a file is selected, it is copied to the products.db file within the filesDir directory,
  and Product Recognition is initialized to use the newly selected database file.

**Clear Database**
- Clear’s database from file local storage.
- Shows “Deleted Product Database” toast

**Export Database**
- Copies products.db file from filesDir, that is used currently for product recognition into **Downloads** folder.

***Supporting Information***
By default, this application does not include a product recognition database. However, users can manually enroll products using the _Product_ model setting sequence and save the associated database for future use. The application also allows users to load databases from memory. Product databases are located in the _/Download/_ directory, while manually labeled product images are stored in the _/Pictures/_ directory.

Once manual labeling is complete, users can run real-time product recognition in _Product_ mode and review the results. If the product recognition results do not meet the desired accuracy levels, additional captures of the same product set may be needed. The sequence of screens required to perform product enrollment is shown below.

### Technology Demos
**Text/OCR Recognizer** - Optical Character Recognition:  
- Displays text recognition results on the live viewfinder.  
- The settings menu offers controls to customize detection, recognition, and grouping features.

#### Advanced OCR Settings
[Advanced OCR Settings](https://techdocs.zebra.com/ai-datacapture/latest/textocr/) allows developer to fine-tune performance for diverse use cases, including document scanning, real-time recognition, and automated data entry.

**Barcode Recognizer** - Barcode Detection and Decode  
- Displays a live camera preview.  
- Highlights 1D and 2D barcodes with bounding boxes in various colors.
- Displays decoded barcode value below the bounding boxes.
**Product & Shelf Localizer** - Retail Shelf Localizer  
- Provides a live camera preview with bounding boxes over detected features like shelves, labels, pegs labels and products.  
- Uses specific colors for each feature: Red for shelves, Blue for labels, Magenta for Peg and Green for products.

### Generic Settings - Model Processing Configurations
**CPU / GPU / DSP** - Configures processor type for running the selected model
- CPU – runs model on CPU, this is typically the least performant for inference time
- GPU – runs model on GPU, typically higher performance than CPU
- DSP – runs model on DSP, fastest performance

**640 / 1280 / 1600 / 2560** - Configures the AI Models to run at a specific input sizes
- 640 -> set’s the model inference dimensions to 640x640
- 1280 -> set’s the model inference dimensions to 1280x1280
- 1600 -> set’s the model inference dimensions to 1600x1600
- 2560 -> set’s the model inference dimensions to 2560x2560

**1MP / 2MP / 4MP / 8MP** - Configures the AI Models to run at a specific input image resolution
- 1MP -> set’s the analyzer input image resolution to 1280x720
- 2MP -> set’s the analyzer input image resolution to 1920x1080
- 4MP -> set’s the analyzer input image resolution to 2688x1512
- 8MP -> set’s the analyzer input image resolution to 3840x2160

Typically lower resolutions should be used when capturing images close-up while higher resolutions allow detection of smaller features or features that are far away.

### Build Dependencies:
This application requires specific dependencies made available by Zebra through a maven repository.  Access to this repository is necessary in order for the application to include all the required libraries required. 
## Support
If you encounter any issues or have questions about using the AI Suite, feel free to contact Zebra Technologies support through the official support page.

## Thank You
Lastly, thank you for being a part of our community. If you have any quesitons, please reach out to our DevRel team at developer@zebra.com

This README.md is designed to provide clarity and a user-friendly onboarding experience for developers. If you have specific details about the project that you would like to include, feel free to let us know!

## License
All content under this repository's root folder is subject to the [Development Tool License Agreement](../../Zebra%20Development%20Tool%20License.pdf). By accessing, using, or distributing any part of this content, you agree to comply with the terms of the Development Tool License Agreement.
