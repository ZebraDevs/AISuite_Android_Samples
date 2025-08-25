# Zebra Mobile Computing AISuite
## AI Data Capture SDK Sample & Demo Applications

Welcome to the [Mobile Computing AISuite's](https://www.zebra.com/ap/en/software/mobile-computer-software/zebra-mobile-computing-ai-suite.html) Sample and Demo application's repository! This repository serves as an archive for numerous applications that demonstrate the capabilities and functionalities of the AI Data Capture SDK.

## Overview
The [AI Data Capture SDK](https://techdocs.zebra.com/ai-datacapture/latest/about/) offers a range of tools and resources for developers looking to integrate and utilize the [AISuite Models](https://techdocs.zebra.com/ai-datacapture/latest/setup/#featuresmodels) within their applications. The sample and demo applications contained in this repository provide practical examples and guidance on how to leverage the SDK effectively.

AI Data Capture SDK offers constructs that are compatible with [CameraX](https://developer.android.com/media/camera/camerax) , which allows developers to build their apps quickly and easily. This also allows the applications to use different detectors or analyzers beyond whats offered in AISuite.

Most of the samples here are built based on CameraX framework. 

## Useful References
- [SDK Documentation](https://techdocs.zebra.com/ai-datacapture/latest/about/)
- [Model Information](https://techdocs.zebra.com/ai-datacapture/latest/setup/#featuresmodels)
- [Developer Experience Videos](https://www.youtube.com/zebratechnologies)

## Contents
The repository includes:

### Code Snippets: 
Minimal code snippets for using AI DataCapture SDK (For eg, BarcodeDecode, ShelfLocalize)

### Sample Applications : 
Code examples showcasing various features of the AI Datacapture SDK.

### Demo Applications: 
Code examples showcasing how developers can build complex usecases using the SDK capabilities.

### Documentation
References to the technical documentation on the AI DataCapture SDK and the models used.

### Resources:
Additional resources, such as youtube videos to help you build your applications faster and better.

## Getting Started
To get started with the sample applications:

- Clone the Repository: Download the repository to your local machine using git clone.
- Explore the Samples: Browse through the sample applications to understand their structure and functionality.
- Read the Documentation: Refer to the included documentation for detailed information on each sample.


## Directory Structure

### AISuite_Snippets
Sample code snippets to easily integrate into applications.

- [Analyzers](AISuite_Snippets/app/src/main/java/com/zebra/example/java/analyzers) - Code snippets for using [EntityTrackerAnalyzer](https://techdocs.zebra.com/ai-datacapture/latest/camerax/#entitytrackeranalyzer).
- [Detectors](AISuite_Snippets/app/src/main/java/com/zebra/example/java/detectors) - Code snippets for using detectors (process() APIs) for [BarcodeDecoder](https://techdocs.zebra.com/ai-datacapture/latest/barcodedecoder/#processimagedataimagedata), [TextOCR](https://techdocs.zebra.com/ai-datacapture/latest/textocr/#processimagedataimagedataexecutorexecutor).
- [LowLevel](AISuite_Snippets/app/src/main/java/com/zebra/example/java/lowlevel) - Code snippets for using foundational apis for detecting, decoding barcodes, OCR and product recognition.
  
### AISuite_QuickStart
Sample application to aide developers build applications using AI DataCapture SDK

- [Analyzers](AISuite_QuickStart/app/src/main/java/com/zebra/aisuite_quickstart/java/analyzers/barcodetracker) - Samples using AI DataCapture SDK's in-built (CameraX) EntityTrackerAnalyzer to detect/decode/track Entities.
- [Detectors](AISuite_QuickStart/app/src/main/java/com/zebra/aisuite_quickstart/java/detectors) - Samples using detectors (process() API), for Localizers, BarcodeDecoder, textOCR components.
- [LowLevel](AISuite_QuickStart/app/src/main/java/com/zebra/aisuite_quickstart/java/lowlevel) - Samples using foundational APIs for detecting, decoding barcodes,OCR and for product recognition. 
  
### AISuite_Demo
Usecase demos built using the capabilities of AI DataCapture SDK, ready to be adopted into customer apps.
 - [AI Data Capture Demo](AISuite_Demos/AIDataCaptureDemo) - Usecase and technology demos showcasing main features and configurations of AISuite
 - [AI Barcode Finder](AISuite_Demos/AI_Barcode_Finder) - Usecase Demo that uses [EntityTrackerAnalyzer](https://techdocs.zebra.com/ai-datacapture/latest/camerax/#entitytrackeranalyzer) to build a multi-barcode finder application easily to interact with actionable barcodes.

## Support
If you encounter any issues or have questions about using the AI Suite, feel free to contact Zebra Technologies support through the official support page.

## Thank You
Lastly, thank you for being a part of our community. If you have any quesitons, please reach out to our DevRel team at developer@zebra.com

This README.md is designed to provide clarity and a user-friendly onboarding experience for developers. If you have specific details about the project that you would like to include, feel free to let us know!

## License
All content under this repository's root folder is subject to the [Development Tool License Agreement](Zebra%20Development%20Tool%20License.pdf). By accessing, using, or distributing any part of this content, you agree to comply with the terms of the Development Tool License Agreement.
