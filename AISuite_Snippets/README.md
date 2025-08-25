# AISuite Snippets
Welcome to the AISuite Snippets repository! This repository contains code snippets **(Java and Kotlin)** to help developers quickly get started with Zebra Technologies [Mobile Computing AI Suite](https://www.zebra.com/ap/en/software/mobile-computer-software/zebra-mobile-computing-ai-suite.html). 

The snippets cannot run standlone as an application on the device. This is only for reference purpose.

## Introduction
The AISuite Snippets repository provides sample code snippets for the various capabilities and interfaces offerred in the AI Data Capture SDK.

* Quickly understand the core features offerred by Zebra’s Mobile Computing AI Suite - The AI Models and the AI Data Capture SDK to use them.
* Reuse the code snippets in your applications

## Useful References
- [SDK Documentation](https://techdocs.zebra.com/ai-datacapture/latest/about/)
- [Model Information](https://techdocs.zebra.com/ai-datacapture/latest/textocr/)
- [Developer Experience Videos](https://www.youtube.com/zebratechnologies)

## Key Features
* Isolated Snippets: Ready-to-use code snippets to demonstrate key functionalities of the AI Data Capture SDK.
* Modular Design: Easily adaptable components to fit various use cases.
* Documentation: Step-by-step instructions for seamless integration.
* Extensibility: Designed to help developers expand and customize as needed.

## Requirements
Refer to the requirements outlined at [TechDocs](https://techdocs.zebra.com/ai-datacapture/latest/setup/#requirements)

## Developer Tools:
A code editor (e.g., Android Studio)
Git for cloning the repository.

## Repository Details:
git clone https://github.com/zebradevs/AISuite_Android_Samples

## Directory Structure
Here’s an overview of the AISuite_Snippets folder:
 
### AISuite_Snippets

#### app/src/main/java/com/zebra/example
##### Java
##### analyzers
- [barcodetracker](app/src/main/java/com/zebra/example/java/analyzers/BarcodeTracker.java) - Java Snippet using [EntityTrackerAnalyzer](https://techdocs.zebra.com/ai-datacapture/about/CameraX#EntityTrackerAnalyzer) to detect/decode/track barcodes.
##### detectors
- [barcodedecodersample](app/src/main/java/com/zebra/example/java/detectors/BarcodeSample.java) - Java Snippet showing how to use [BarcodeDecoder as a detector](https://techdocs.zebra.com/ai-datacapture/barcodedecoder/#processimagedataimagedata) in your CameraX Analyzer.
- [textocrsample](app/src/main/java/com/zebra/example/java/detectors/OCRSample.java) - Java Snippet showing how to use [TextOCR as a detector](https://techdocs.zebra.com/ai-datacapture/textocr/#processimagedataimagedata) in your CameraX Analyzer.
##### lowlevel
- [productrecognitionsample](app/src/main/java/com/zebra/example/java/lowlevel/ProductRecognitionSample.java) - Java Snippet to build a [shelf localization and product recognition](https://techdocs.zebra.com/ai-datacapture/productrecognition/) application.
- [simplebarcodesample](app/src/main/java/com/zebra/example/java/lowlevel/BarcodeLegacySample.java) - Java Snippet to use [detect/decode APIs to localize and decode barcodes](https://techdocs.zebra.com/ai-datacapture/barcodedecoder/#decodebitmapbmpbboxdetectionsexecutorexecutor) from BitMap images.
- [simpleocrsample](app/src/main/java/com/zebra/example/java/lowlevel/OCRLegacySample.java) - Java Snippet to use [detect APIs to recognize text](https://techdocs.zebra.com/ai-datacapture/textocr/#detectbitmapsrcimgexecutorexecutor) from BitMap images.
    
##### Kotlin
##### analyzers
- [barcodetracker](app/src/main/java/com/zebra/example/kotlin/analyzers/BarcodeTracker.kt) - Kotlin Sample using [EntityTrackerAnalyzer](https://techdocs.zebra.com/ai-datacapture/about/CameraX#EntityTrackerAnalyzer) to detect/decode/track barcodes.
##### detectors
- [barcodedecodersample](app/src/main/java/com/zebra/example/kotlin/detectors/BarcodeSample.kt) - Kotlin Sample showing how to use [BarcodeDecoder as a detector](https://techdocs.zebra.com/ai-datacapture/barcodedecoder/#processimagedataimagedata) in your CameraX Analyzer.
- [textocrsample](app/src/main/java/com/zebra/example/kotlin/detectors/OCRSample.kt) - Kotlin Sample showing how to use [TextOCR as a detector](https://techdocs.zebra.com/ai-datacapture/textocr/#processimagedataimagedata) in your CameraX Analyzer.
##### lowlevel
- [productrecognitionsample](app/src/main/java/com/zebra/example/kotlin/lowlevel/ProductRecognitionSample.kt) - Kotlin Sample to build a [shelf localization and product recognition](https://techdocs.zebra.com/ai-datacapture/productrecognition/) application.
- [simplebarcodesample](app/src/main/java/com/zebra/example/kotlin/lowlevel/BarcodeLegacySample.kt) - Kotlin Sample to use [detect/decode APIs to localize and decode barcodes](https://techdocs.zebra.com/ai-datacapture/barcodedecoder/#decodebitmapbmpbboxdetectionsexecutorexecutor) from BitMap images.
- [simpleocrsample](app/src/main/java/com/zebra/example/kotlin/lowlevel/OCRLegacySample.kt) - Kotlin Sample to use [detect APIs to recognize text](https://techdocs.zebra.com/ai-datacapture/textocr/#detectbitmapsrcimgexecutorexecutor) from BitMap images.
    

## Support
If you encounter any issues or have questions about using the AI Suite Snippets, feel free to contact Zebra Technologies support through the official support page.

## Thank You
Lastly, thank you for being a part of our community. If you have any quesitons, please reach out to our DevRel team at developer@zebra.com

This README.md is designed to provide clarity and a user-friendly onboarding experience for developers. If you have specific details about the project that you would like to include, feel free to let us know!

## License
All content under this repository's root folder is subject to the [End User License Agreement (EULA)](../Zebra%20Development%20Tool%20License.pdf). By accessing, using, or distributing any part of this content, you agree to comply with the terms of the EULA.

This README.md is designed to provide clarity and a user-friendly onboarding experience for developers. If you have specific details about the project that you would like to include, feel free to let us know!
