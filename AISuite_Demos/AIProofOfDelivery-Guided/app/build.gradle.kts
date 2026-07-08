import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.zebra.ai.ppodguided"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.zebra.ai.ppodguided"
        minSdk = 33
        targetSdk = 36
        versionCode = 45
        versionName = "5.1.6"

        val pendoApiKey = System.getenv("aippodguided_pendo_api_key") ?: ""
        buildConfigField(type = "String", name = "PendoApiKey", value = "\"$pendoApiKey\"")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }

    kotlin {
        compilerOptions {
            jvmTarget  = JvmTarget.JVM_11
        }
    }

    applicationVariants.configureEach {
        // rename the output APK file
        outputs.configureEach {
            (this as? ApkVariantOutputImpl)?.outputFileName =
                "${rootProject.name}_${versionName}.apk"
        }
    }

    androidResources {
        noCompress.add("tar")
        noCompress.add("tar.crypt")
        noCompress.add("tflite")
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.exifinterface)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.zebra.ai.data.capture.sdk) { artifact { type = "aar" } }
    implementation(libs.proof.of.delivery) { artifact { type = "aar" } }
    implementation(libs.barcode.decoder) { artifact { type = "aar" } }
    implementation(libs.ocr.recognition) { artifact { type = "aar" } }
    implementation(libs.fcn.resnet.segmentation) { artifact { type = "aar" } }

    // Pendo SDK
    implementation(libs.pendo.io) { isChanging = true }
}
