import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias (libs.plugins.kotlin.serializer)
}

// Load version properties
val versionPropsFile = file("../version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use { stream ->
        versionProps.load(stream)
    }
}

// Version configuration with fallbacks
val versionMajor = project.findProperty("VERSION_MAJOR")?.toString()?.toIntOrNull()
    ?: versionProps.getProperty("VERSION_MAJOR")?.toIntOrNull() ?: 1
val versionMinor = project.findProperty("VERSION_MINOR")?.toString()?.toIntOrNull()
    ?: versionProps.getProperty("VERSION_MINOR")?.toIntOrNull() ?: 0
val versionPatch = project.findProperty("VERSION_PATCH")?.toString()?.toIntOrNull()
    ?: versionProps.getProperty("VERSION_PATCH")?.toIntOrNull() ?: 0
val versionBuild = project.findProperty("VERSION_BUILD")?.toString()?.toIntOrNull()
    ?: System.getenv("BUILD_NUMBER")?.toIntOrNull()
    ?: versionProps.getProperty("VERSION_BUILD")?.toIntOrNull() ?: 1

val appVersionName = "$versionMajor.$versionMinor.$versionPatch"
val appVersionCode = versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild

android {
    namespace = "com.zebra.ai.palletchecker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zebra.ai.palletchecker"
        minSdk = 33
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Make version info available to the app
        buildConfigField("String", "VERSION_NAME", "\"$appVersionName\"")
        buildConfigField("int", "VERSION_CODE", "$appVersionCode")
        buildConfigField("String", "APP_NAME", "\"AI Pallet Checker(Beta)\"")
        buildConfigField(
            "String",
            "AI_VISION_SDK_VERSION",
            "\"${libs.versions.zebraAIVisionSdk.get()}\""
        )
        buildConfigField(
            "String",
            "BARCODE_DECODER_MODEL_VERSION",
            "\"${libs.versions.barcodeDecoder.get()}\""
        )

        buildConfigField(
            "String",
            "PALLET_AND_BOX_LOCALIZER_MODEL_VERSION",
            "\"${libs.versions.palletAndBoxLocalizer.get()}\""
        )
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.foundation)
    implementation(libs.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.uiautomator)

    // Test rules and transitive dependencies:
    androidTestImplementation(libs.androidx.ui.test.junit4)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // Below dependency is to get AI Data Capture SDK

    implementation(libs.zebra.ai.vision.sdk){ artifact { type = "aar" } }
    implementation(libs.barcode.decoder) { artifact { type = "aar" } }
    implementation(libs.pallet.and.box.localizer) { artifact { type = "aar" } }

    implementation(libs.androidx.navigation)
    implementation(libs.coil.compose)

    // Jetpack Compose BOM and dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.kotlinx.json.serialization)
    implementation(libs.gson)
}