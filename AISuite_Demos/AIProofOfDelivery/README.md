# AI Picture Proof of Delivery — Android Sample Application

A fully functional Android reference implementation of Zebra's **Picture Proof of Delivery (PPoD) Frontline AI Blueprint**, demonstrating how the [Zebra AI Data Capture SDK](https://techdocs.zebra.com/ai-datacapture) can automate and validate last-mile delivery photo capture — entirely on-device with no cloud dependency.

---

## Background

### Zebra AI Suite — AI Enablers

Zebra's AI Suite is a collection of on-device AI capabilities purpose-built for enterprise frontline workflows. Rather than generic computer vision models, it provides:

- **Pre-trained, AI models** optimized for real-world frontline conditions (variable lighting, handheld angles, industrial environments).
- **An SDK abstraction layer** ([AI Data Capture SDK](https://techdocs.zebra.com/ai-datacapture)) that wraps model inference, hardware acceleration (DSP), and result interpretation behind clean, composable APIs.
- **Benchmarked hardware** — the SDK and models are validated on Zebra mobile computers, ensuring inference performance is predictable in production.

### Zebra Frontline AI Blueprints

Blueprints are validated, reference solutions built on top of the AI Suite that target specific high-value frontline workflows. Each Blueprint packages proven AI models, integration patterns, and configurable business logic into a ready-made framework. For more on Zebra's Blueprint strategy and the full catalog:

> [zebra.com/us/en/software/ai-software/zebra-frontline-ai-blueprints.html](https://www.zebra.com/us/en/software/ai-software/zebra-frontline-ai-blueprints.html)

### The Picture Proof of Delivery (PPoD) Challenge

The traditional driver workflow requires three separate manual steps:

1. Scan the package barcode
2. Step back, avoid people/pets, and capture a photo
3. Manually select or confirm the delivery location in-app

This is slow, error-prone, and relies entirely on driver compliance. The PPoD Blueprint collapses this into **a single camera action** — one photo triggers on-device AI that automatically validates the scene, redacts PII, classifies the delivery context, and packages all metadata for backend sync. The result: **55% faster PPoD processing** and 1–2 additional deliveries per driver per day.

---

## This Repository

This repo is a **complete, runnable Android demonstration** of the PPoD Blueprint. It is intended to:

1. **Show SDK integration patterns** — how to initialize `ImageAttributesDetector` and `ImageTransformDetector`, wire them to preferences, and process `Bitmap` frames from CameraX.
2. **Demonstrate the full compliance + privacy pipeline** — validation checks running in parallel with PII blurring, all on-device.
3. **Serve as a starting point** for teams integrating the PPoD Blueprint into their own delivery applications.

The app can run as a **standalone launcher app** or be invoked as an **`android.media.action.IMAGE_CAPTURE` intent target**, making it a drop-in AI-powered camera replacement for existing delivery apps.

---

## Features Demonstrated

### Image Compliance Checks (Pass/Fail validation before saving)

| Check | SDK Metric | What It Validates |
|---|---|---|
| Image Quality | `ImageQualityClear` | Photo is in focus / not motion-blurred |
| Package Visible | `ImageTagPackageVisible` | A parcel or package is present in frame |
| Surroundings Visible | `ImageTagSurroundVisible` | Enough surrounding context (doorstep, address, etc.) |
| No People | `ImageTagPeopleVisible` | No identifiable persons in the scene (compliance) |

Each check has an independently configurable confidence threshold (0.0–1.0). A failed check produces a visual rejection (red border glow) with a specific reason, giving the driver immediate corrective feedback.

### Image Privacy Transformations (Applied to the saved image)

| Transformation | SDK Action | Purpose |
|---|---|---|
| Blur People | `LocalizeAndBlurPeople` | GDPR/PII — remove identifiable faces/bodies |
| Blur Pets | `LocalizeAndBlurPets` | Reduce identifiable information |
| Blur Barcodes | `LocalizeAndBlurBarcode` | Protect shipment barcodes in stored photos |
| Blur Text | `LocalizeAndBlurText` | Protect address labels, signage |

Blur radius is configurable: Low / Medium / High.

### Application Output and Metadata

- Processed images are saved as JPEG (90% quality) at a configurable save resolution (320×240 → 2560×1920).
- **EXIF metadata** is embedded in every saved image: timestamp, device make/model, app version, and a compliance result comment.
- When invoked via `IMAGE_CAPTURE` intent, the result image is returned to the calling app via `ContentProvider` URI.

---

## Architecture Overview

```
MainActivity
    │
    ├── EulaScreen (first launch only)
    │
    └── MainScreen
            │
            ├── CameraPreview (CameraX)
            ├── BorderGlow  ← IDLE / GOOD / BAD state
            ├── Report      ← compliance issues list
            ├── CountDownTimer ← auto-close on success
            └── Settings Overlay
                    │
                    └── ZPreferences (XML-driven, MDM-aware)

AppViewModel (MVVM hub)
    ├── processImage()     ← orchestrates the pipeline
    ├── saveImage()        ← EXIF + ContentProvider output
    └── ImageProcessor
            ├── ImageAttributesDetector  ← compliance checks
            └── ImageTransformDetector   ← PII blurring
```

**Key design decisions worth understanding:**

- `ImageProcessor` uses a **single-threaded executor** for all processing — this prevents race conditions.
- A **second single-threaded executor** handles detector initialization so that preference changes (which trigger full re-initialization) are queued and do not interrupt in-flight processing.
- `ZPreferences` listens for Android **App Restriction** broadcasts, enabling MDM-controlled policy enforcement of all settings at the device management layer.
- Device rotation is tracked via the **accelerometer** (not `WindowManager`) so that image orientation is correct even when the activity is locked to portrait.

---

## SDK Integration Quick Reference

The following is the minimal pattern this app uses to integrate the AI Data Capture SDK. See `ImageProcessor.kt` for the complete implementation.

### 1. Initialize the SDK

```kotlin
AIVisionSDK.getInstance(application).init()
```

Call once, typically in your `ViewModel` or `Application` class. License validation occurs when the `ImageAttributesDetector` is initialized — catch `AIVisionSDKLicenseException` there to detect licensing failures.

### 2. Configure and Create an `ImageAttributesDetector`

```kotlin
val metrics = listOf(
    ImageAttributeMetricValue.Builder(ImageQualityClear)
        .setValue(0.5f).setEnable(true).setCondition(GT_THAN).build(),
    ImageAttributeMetricValue.Builder(ImageTagPackageVisible)
        .setValue(0.5f).setEnable(true).setCondition(GT_THAN).build(),
    ImageAttributeMetricValue.Builder(ImageTagPeopleVisible)
        .setValue(0.3f).setEnable(true).setCondition(LS_THAN).build(),
    ImageAttributeMetricValue.Builder(ImageTagSurroundVisible)
        .setValue(0.5f).setEnable(true).setCondition(GT_THAN).build()
)

val settings = ImageAttributesDetector.Settings()
settings.configureImageAttributeMetrics(metrics)

// License is validated here — catch AIVisionSDKLicenseException to detect licensing failures
try {
    val detector = ImageAttributesDetector
        .getImageAttributesDetector(settings, executor).get()
} catch (e: Exception) {
    if (hasCause(e, AIVisionSDKLicenseException::class.java)) {
        // handle license failure
    }
}
```

### 3. Configure and Create an `ImageTransformDetector`

```kotlin
val descriptor = TransformActionDescriptor.Builder()
    .setActions(listOf(
        TransformationAction.LocalizeAndBlurPeople,
        TransformationAction.LocalizeAndBlurBarcode
    ))
    .setBlurRadius(BlurRadius.Medium)
    .build()

val settings = ImageTransformDetector.Settings()
val options = InferencerOptions()
options.runtimeProcessorOrder = arrayOf(InferencerOptions.DSP)
settings.configureInferencerOptions(options)
settings.configureTransformationActions(descriptor)

val transformer = ImageTransformDetector
    .getImageTransformDetector(settings, executor).get()
```

### 4. Process a Frame

```kotlin
val imageData = ImageData.fromBitmap(bitmap, 0)

// Compliance check
val results = detector.process(imageData, executor).get()
for (result in results) {
    val passed = result.isCompliant
    val confidence = result.value as Float
}

// Privacy transformation
val transformed = transformer.process(imageData).get()
val outputBitmap = transformed.bitmapImage
```

### 5. Always Dispose

```kotlin
detector.dispose()
transformer.dispose()
```

---

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- Zebra mobile computer running Android 13+ (API 33), **or** an Android 13+ device for initial development (note: DSP acceleration requires Zebra hardware)
- Artifactory credentials for the Zebra model repository

### Repository Access — Zebra Artifactory

The repository URL is already configured in `settings.gradle.kts`:
```
https://zebratech.jfrog.io/artifactory/emc-mvn-ext
```

Contact your Zebra representative or refer to the [SDK setup documentation](https://techdocs.zebra.com/ai-datacapture/latest/setup/#requirements) for access.

### Build and Run

```bash
# Clone and open in Android Studio, then:

# Build debug APK
./gradlew assembleDebug

# Install directly to a connected device
./gradlew installDebug

# Build release APK (minification is disabled for easier inspection)
./gradlew assembleRelease
```

The output APK is named `AI Proof of Delivery_{versionName}.apk` and placed in `app/build/`.

### First Launch

On first launch the app presents a EULA screen. Accept to proceed to the main camera screen. The EULA acceptance state is persisted in `SharedPreferences` and can be reset via MDM (app restrictions key: `eula_accepted`).

---

## Licensing

The Image Attributes Proof of Delivery AI model requires a software license to be procured and deployed to all target devices before the SDK will process images. In the app, `AIVisionSDKLicenseException` (caught in [ImageProcessor.kt](app/src/main/java/com/zebra/ai/ppod/ai/ImageProcessor.kt)) is the runtime signal that licensing is not in place.

Full step-by-step instructions are on the [AI Data Capture SDK licensing page](https://techdocs.zebra.com/ai-datacapture/3-2/license/). The four required stages are:

**1. Procure a license** — Submit a license request to Zebra specifying the use case ("Picture Proof of Delivery AI Blueprint") and quantity. Contact your Zebra representative to initiate this.

**2. Deploy & activate on devices** — Licenses are activated on target devices via the **Zebra License Manager app** (v15.0.4+, pre-installed on most Zebra devices). Deployment uses an EMM tool such as Zebra StageNow or SOTI MobiControl with the Badge ID and Product Name from the procurement confirmation email.

**3. Generate an app signature certificate** — Your signed APK must be identified to Zebra's Access Manager. Use the Zebra App Signature Tool (`SigTools.jar`) to extract the certificate:

```bash
java -jar SigTools.jar GETCERT -INFORM APK -OUTFORM DER -IN <your-app.apk> -OUTFILE <your-app-cert.pem>
```

> The APK must be signed with your production keystore — the certificate is bound to the signing identity, not the package name.

**4. Allowlist your app via StageNow** — Create an **AccessMgr** profile (MX 14.2+, Xpert Mode) with the values below and deploy to all target devices:

| Field | Value |
|---|---|
| Server Access Action | Allow Caller to Call Service |
| Service Identifier | `delegation-zebra-zsl-api-access-query` |
| Caller Package Name | Your app's package name (e.g. `com.zebra.ai.ppod`) |
| Caller Signature | The `.pem` certificate from step 3 |

The completed profile deploys via StageNow barcode scan or as exported XML for EMM distribution.

**Additional licensing resources:**
- [License Manager User Guide](https://techdocs.zebra.com/licensing)
- [Zebra Technical Support](https://www.zebra.com/us/en/support-downloads.html)

---

## Configuration

All settings are accessible in-app via the settings overlay (gear icon) and can also be remotely managed via Android Enterprise MDM using the app restriction keys defined in `app/src/main/res/xml/app_restrictions.xml`.

| Category | Key | Type | Default | Description |
|---|---|---|---|---|
| Compliance | `compliance_blur` | bool | `true` | Enable image quality/blur check |
| Compliance | `compliance_package` | bool | `true` | Enable package visibility check |
| Compliance | `compliance_surroundings` | bool | `true` | Enable surroundings check |
| Compliance | `compliance_contains_people` | bool | `true` | Enable people-in-frame check |
| Privacy | `blur_people` | bool | `true` | Blur people in saved image |
| Privacy | `blur_pets` | bool | `true` | Blur pets in saved image |
| Privacy | `blur_text` | bool | `true` | Blur text in saved image |
| Privacy | `blur_barcodes` | bool | `true` | Blur barcodes in saved image |
| Blur Radius | `blur_ratio` | choice | Medium | Low / Medium / High |
| Resolution | `capture_resolution` | choice | 320×240 | Inference resolution |
| Resolution | `save_resolution` | choice | 640×480 | Saved image resolution |
| Thresholds | `threshold_image_quality` | float | model default | 0.0–1.0 |
| Thresholds | `threshold_package` | float | model default | 0.0–1.0 |
| Thresholds | `threshold_surroundings` | float | model default | 0.0–1.0 |
| Thresholds | `threshold_contains_people` | float | model default | 0.0–1.0 |

Threshold defaults are read from the model itself when not explicitly set. Changes to any setting trigger automatic re-initialization of the SDK detectors.

---

## Intent Integration

The app registers as a handler for `android.media.action.IMAGE_CAPTURE`, enabling any app to use it as an AI-powered camera replacement:

```kotlin
// In your delivery app:
val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
intent.putExtra(MediaStore.EXTRA_OUTPUT, yourContentUri)
startActivityForResult(intent, REQUEST_CODE)
```

The PPoD app captures, validates, transforms, and writes the result directly to the provided `ContentProvider` URI, then returns to the calling app.


---

## Further Reading

| Resource | URL |
|---|---|
| AI Data Capture SDK documentation | https://techdocs.zebra.com/ai-datacapture |
| SDK setup & requirements | https://techdocs.zebra.com/ai-datacapture/latest/setup/#requirements |
| Available AI models | https://techdocs.zebra.com/ai-datacapture/latest/models/ |
| Zebra Frontline AI Blueprints overview | https://www.zebra.com/us/en/software/ai-software/zebra-frontline-ai-blueprints.html |

---

## License

This sample application is provided by Zebra Technologies for evaluation and reference purposes. Use is subject to the Zebra End User License Agreement presented on first launch. See `app/src/main/assets/` for the full EULA text.