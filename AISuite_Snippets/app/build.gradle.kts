plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.zebra.example"
    compileSdk = 35

    androidResources {
        noCompress.add("tar")
        noCompress.add("tar.crypt")
    }

    defaultConfig {
        applicationId = "com.zebra.example"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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

            // Format: AISuite_Snippets-v1.8-release.apk or AISuite_Snippets-v1.8-debug.apk
            output.outputFileName = "AISuite_Snippets-v${versionName}-${buildType}.apk"
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


    //Below dependency is to get AI Suite SDK
    implementation(libs.zebra.ai.vision.sdk) { artifact { type = "aar" } }

    //Below dependency is to get Barcode Localizer model for AI Suite SDK
    implementation(libs.barcode.localizer) { artifact { type = "aar" } }

    //Below dependency is to get OCR model for AI Suite SDK
    implementation(libs.text.ocr.recognizer) { artifact { type = "aar" } }

    //Below dependency is to get product Recognition model for AI Suite SDK
    implementation(libs.product.and.shelf.recognizer) { artifact { type = "aar" } }
}