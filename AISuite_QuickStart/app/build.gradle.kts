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
    }
    defaultConfig {
        applicationId = "com.zebra.aisuite_quickstart"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "3.0"

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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val versionName = defaultConfig.versionName
            val versionCode = defaultConfig.versionCode
            val buildType = buildType.name
            val sdkVersion = libs.versions.zebraAIVisionSdk.get()

            // Format: AISuite_Quickstart-v1.8-release-SDK_3.1.4.apk or AISuite_Quickstart-v1.8-debug-SDK_3.1.4.apk
            output.outputFileName = "AISuite_Quickstart-v${versionName}-${buildType}-SDK_${sdkVersion}.apk"

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
    implementation(libs.barcode.localizer) { artifact { type = "aar" } }

    //Below dependency is to get OCR model for AI Suite SDK
    implementation(libs.text.ocr.recognizer) { artifact { type = "aar" } }

    //Below dependency is to get product Recognition model for AI Suite SDK
    implementation(libs.product.and.shelf.recognizer) { artifact { type = "aar" } }
}