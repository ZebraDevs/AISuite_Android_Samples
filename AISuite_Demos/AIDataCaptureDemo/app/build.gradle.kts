plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.zebra.aidatacapturedemo"
    compileSdk = 36

    androidResources {
        noCompress.add("tar")
        noCompress.add("tar.crypt")
    }

    defaultConfig {
        applicationId = "com.zebra.aidatacapturedemo"
        minSdk = 33
        targetSdk = 36
        versionCode = 16
        val appVersion: String = libs.versions.appVersion.get().toString()
        versionName = appVersion

        buildConfigField("String", "AI_DataCaptureDemo_Version", "\"$appVersion\"")

        val zebraAIVisionSdk: String = libs.versions.zebraAIVisionSdk.get().toString()
        buildConfigField("String", "Zebra_AI_VisionSdk_Version", "\"$zebraAIVisionSdk\"")

        val barcodeLocalizer: String = libs.versions.barcodeLocalizer.get().toString()
        buildConfigField("String", "BarcodeLocalizer_Version", "\"$barcodeLocalizer\"")

        val textOcrRecognizer: String = libs.versions.textOcrRecognizer.get().toString()
        buildConfigField("String", "TextOcrRecognizer_Version", "\"$textOcrRecognizer\"")

        val productAndShelfRecognizer: String = libs.versions.productAndShelfRecognizer.get().toString()
        buildConfigField("String", "ProductAndShelfRecognizer_Version", "\"$productAndShelfRecognizer\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
		jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.constraintlayout)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.play.services.tasks)
    implementation(libs.androidx.navigation.compose.android)
    implementation(libs.androidx.documentfile)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.jsoup)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.androidx.camera.extensions)
    implementation(libs.runtime.permissions)

    // JSON serialization
    implementation(libs.gson)

    //Below dependency is to get AI Suite SDK
    implementation(libs.zebra.ai.vision.sdk) { artifact { type = "aar" } }

    //Below dependency is to get Barcode Localizer model for AI Suite SDK
    implementation(libs.barcode.localizer) { artifact { type = "aar" } }

    //Below dependency is to get OCR model for AI Suite SDK
    implementation(libs.text.ocr.recognizer) { artifact { type = "aar" } }

    //Below dependency is to get product Recognition model for AI Suite SDK
    implementation(libs.product.and.shelf.recognizer) { artifact { type = "aar" } }

    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}