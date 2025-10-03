# AISuite QuickStart Application
Welcome to the AISuite QuickStart Sample application repository! This repository contains samples **(Java and Kotlin)** and resources to help you quickly get started with Zebra Technologies [Mobile Computing AI Suite](https://www.zebra.com/ap/en/software/mobile-computer-software/zebra-mobile-computing-ai-suite.html).

The AISuite Quick Start Sample Application provides a streamlined setup and examples to enable developers to integrate Zebra’s AI capabilities into their applications with ease.

## Introduction
The AI Suite Quick Start repository provides sample code to use various features of AI Data Capture SDK.

* Quickly understand the core features offerred by Zebra’s Mobile Computing AI Suite - The AI Models and the AI Data Capture SDK to use them.
* Set up and run AI-based solutions in a minimal timeframe.
* Access ready-to-use examples for integration into the projects.
* This guide focuses on the contents of the AISuite_QuickStart folder, which is specifically designed to simplify the onboarding process for developers.

## Useful References
- [SDK Documentation](https://techdocs.zebra.com/ai-datacapture/latest/about/)
- [Model Information](https://techdocs.zebra.com/ai-datacapture/latest/setup/#featuresmodels)
- [Developer Experience Videos](https://www.youtube.com/zebratechnologies)

## Key Features
* Pre-configured examples: Ready-to-run sample projects to demonstrate key functionalities of the AI Data Capture SDK.
* Modular Design: Easily adaptable components to fit various use cases.
* Documentation: Step-by-step instructions for seamless integration.
* Extensibility: Designed to help developers expand and customize as needed.

## Requirements
Refer to the requirements outlined at [TechDocs](https://techdocs.zebra.com/ai-datacapture/latest/setup/#requirements)

## Developer Tools:
A code editor (e.g., Android Studio)
Git for cloning the repository.

## Repository Details:
git clone https://github.com/zebradevs/AISuite_Android_Samples.git

## Directory Structure
Here’s an overview of the AISuite_QuickStart Sample Application folder:
 
### AISuite_QuickStart

#### app/src/main/java/com/zebra/aisuite_quickstart
##### Java
##### analyzers
- [barcodetracker](app/src/main/java/com/zebra/aisuite_quickstart/java/analyzers/barcodetracker) - Java Sample using [EntityTrackerAnalyzer](https://techdocs.zebra.com/ai-datacapture/latest/camerax/#entitytrackeranalyzer) to detect/decode/track barcodes.
##### detectors
- [barcodedecodersample](app/src/main/java/com/zebra/aisuite_quickstart/java/detectors/barcodedecodersample) - Java Sample showing how to use [BarcodeDecoder as a detector](https://techdocs.zebra.com/ai-datacapture/latest/barcodedecoder/#processimagedataimagedata) in your CameraX Analyzer.
- [textocrsample](app/src/main/java/com/zebra/aisuite_quickstart/java/detectors/textocrsample) - Java Sample showing how to use [TextOCR as a detector](https://techdocs.zebra.com/ai-datacapture/latest/textocr/#processimagedataimagedataexecutorexecutor) in your CameraX Analyzer.
##### lowlevel
- [productrecognitionsample](app/src/main/java/com/zebra/aisuite_quickstart/java/lowlevel/productrecognitionsample) - Java Sample to build a [shelf localization and product recognition](https://techdocs.zebra.com/ai-datacapture/latest/productrecognition/) application. <br> <br> **Note:** The Product Recognition feature requires enrollment of products to generate a product index for the products to be recognized. In [productrecognitionsample](app/src/main/java/com/zebra/aisuite_quickstart/java/lowlevel/productrecognitionsample), there is a [sample shelf image](app/src/main/assets/demo_shelf.jpg) and the corresponding [product.index](app/src/main/assets/product.index) are used. Developers can use the [sample shelf image](app/src/main/assets/demo_shelf.jpg) to check how the products are recognized. For the product enrollment process, please refer to [AIDataCaptureDemo](../AISuite_Demos/AIDataCaptureDemo).<br><br>
- [simplebarcodesample](app/src/main/java/com/zebra/aisuite_quickstart/java/lowlevel/simplebarcodesample) - Java Sample to use [detect/decode APIs to localize and decode barcodes](https://techdocs.zebra.com/ai-datacapture/latest/barcodedecoder/#decodebitmapbmpbboxdetectionsexecutorexecutor) from BitMap images.
- [simpleocrsample](app/src/main/java/com/zebra/aisuite_quickstart/java/lowlevel/simpleocrsample) - Java Sample to use [detect APIs to recognize text](https://techdocs.zebra.com/ai-datacapture/latest/textocr/#detectparagraphsbitmapsrcimgexecutorexecutor) from BitMap images.
##### viewfinder
- [viewfinder](app/src/main/java/com/zebra/aisuite_quickstart/java/viewfinder/EntityViewGraphic.java) - Java Sample showing how to use [AI Data Capture SDK's Viewfinder](https://techdocs.zebra.com/ai-datacapture/latest/camerax/#entityviewfinder) along with [EntityTrackerAnalyzer](https://techdocs.zebra.com/ai-datacapture/latest/camerax/#entitytrackeranalyzer) to display tracked entities
##### Kotlin
##### analyzers
- [barcodetracker](app/src/main/java/com/zebra/aisuite_quickstart/kotlin/analyzers/barcodetracker) - Kotlin Sample using [EntityTrackerAnalyzer](https://techdocs.zebra.com/ai-datacapture/latest/camerax/#entitytrackeranalyzer) to detect/decode/track barcodes.
##### detectors
- [barcodedecodersample](app/src/main/java/com/zebra/aisuite_quickstart/kotlin/detectors/barcodedecodersample) - Kotlin Sample showing how to use [BarcodeDecoder as a detector](https://techdocs.zebra.com/ai-datacapture/latest/barcodedecoder/#processimagedataimagedata) in your CameraX Analyzer.
- [textocrsample](app/src/main/java/com/zebra/aisuite_quickstart/kotlin/detectors/textocrsample) - Kotlin Sample showing how to use [TextOCR as a detector](https://techdocs.zebra.com/ai-datacapture/latest/textocr/#processimagedataimagedataexecutorexecutor) in your CameraX Analyzer.
##### lowlevel
- [productrecognitionsample](app/src/main/java/com/zebra/aisuite_quickstart/kotlin/lowlevel/productrecognitionsample) - Kotlin Sample to build a [shelf localization and product recognition](https://techdocs.zebra.com/ai-datacapture/latest/productrecognition/) application. <br><br> **Note:** The Product Recognition feature requires enrollment of products to generate a product index for the products to be recognized. In [productrecognitionsample](app/src/main/java/com/zebra/aisuite_quickstart/kotlin/lowlevel/productrecognitionsample), there is a [sample shelf image](app/src/main/assets/demo_shelf.jpg) and the corresponding [product.index](app/src/main/assets/product.index) are used. Developers can use the [sample shelf image](app/src/main/assets/demo_shelf.jpg) to check how the products are recognized. For the product enrollment process, please refer to [AIDataCaptureDemo](../AISuite_Demos/AIDataCaptureDemo).<br><br>
- [simplebarcodesample](app/src/main/java/com/zebra/aisuite_quickstart/kotlin/lowlevel/simplebarcodesample) - Kotlin Sample to use [detect/decode APIs to localize and decode barcodes](https://techdocs.zebra.com/ai-datacapture/latest/barcodedecoder/#decodebitmapbmpbboxdetectionsexecutorexecutor) from BitMap images.
- [simpleocrsample](app/src/main/java/com/zebra/aisuite_quickstart/kotlin/lowlevel/simplebarocrsample) - Kotlin Sample to use [detect APIs to recognize text](https://techdocs.zebra.com/ai-datacapture/latest/textocr/#detectparagraphsbitmapsrcimgexecutorexecutor) from BitMap images.
##### viewfinder
- [viewfinder](app/src/main/java/com/zebra/aisuite_quickstart/kotlin/viewfinder/EntityViewGraphic.java) - Kotlin Sample showing how to use [AI Data Capture SDK's Viewfinder](https://techdocs.zebra.com/ai-datacapture/latest/camerax/#entityviewfinder) along with [EntityTrackerAnalyzer](https://techdocs.zebra.com/ai-datacapture/latest/camerax/#entitytrackeranalyzer) to display tracked entities.

## Support
If you encounter any issues or have questions about using the AI Suite Quick Start, feel free to contact Zebra Technologies support through the official support page.

## Thank You
Lastly, thank you for being a part of our community. If you have any quesitons, please reach out to our DevRel team at developer@zebra.com

This README.md is designed to provide clarity and a user-friendly onboarding experience for developers. If you have specific details about the project that you would like to include, feel free to let us know!

## License
All content under this repository's root folder is subject to the [Development Tool License Agreement](../Zebra%20Development%20Tool%20License.pdf). By accessing, using, or distributing any part of this content, you agree to comply with the terms of the Development Tool License Agreement.

This README.md is designed to provide clarity and a user-friendly onboarding experience for developers. If you have specific details about the project that you would like to include, feel free to let us know!
