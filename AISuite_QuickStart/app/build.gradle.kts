plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.zebra.aisuite_quickstart"
    compileSdk = 35
    androidResources {
        noCompress.add("tar")
        noCompress.add("tar.crypt")
        noCompress.add("tflite")
        noCompress.add("onnx")
    }
    defaultConfig {
        applicationId = "com.zebra.aisuite_quickstart"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        val appVersion: String = libs.versions.appVersion.get()
        versionName = appVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this
            val fileName = "AIDCQuickStart-${variant.buildType.name}-v${variant.versionName}.apk"
            (output as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = fileName
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        jniLibs {
            // ONNX Runtime ships libc++_shared.so; keep the first one found.
            pickFirsts += "**/libc++_shared.so"
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.androidx.camera.extensions)

    implementation(libs.json)
    implementation(libs.gson)

    //Below dependency is to get AI Suite SDK
    implementation(libs.zebra.ai.vision.sdk) { artifact { type = "aar" } }

    //Below dependency is to get Barcode Localizer model for AI Suite SDK
    implementation(libs.barcode.decoder) { artifact { type = "aar" } }

    //Below dependency is to get OCR model for AI Suite SDK
    implementation(libs.text.ocr.recognizer) { artifact { type = "aar" } }

    //Below dependency is to get product Recognition model for AI Suite SDK
    implementation(libs.product.and.shelf.recognizer) { artifact { type = "aar" } }

    //Below dependency is to get warehouse localizer beta model for AI Suite SDK
    implementation(libs.pallet.and.box.localizer) { artifact { type = "aar" } }

    // Custom Detector dependencies
    implementation(libs.mlkit.text.recognition)
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support) {
        exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
    }
    implementation(libs.onnxruntime.android)
}
