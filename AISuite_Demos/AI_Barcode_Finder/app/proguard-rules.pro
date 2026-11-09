# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line numbers in stack traces for release builds.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# =============================================================================
# Gson — field names are read by reflection at runtime.
# Keep the Gson library itself and any TypeAdapterFactory implementations.
# =============================================================================
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# =============================================================================
# Settings model — serialized/deserialized by Gson (SettingsJsonStorage).
# Keep field names, enum constants, and the no-arg constructor so JSON written
# by one build version still loads (with defaults for new fields) after update.
# =============================================================================
-keepclassmembers class com.zebra.ai.barcodefinder.sdkcoordinator.model.AppSettings {
    <init>();
    <fields>;
}
-keepclassmembers class com.zebra.ai.barcodefinder.sdkcoordinator.model.BarcodeSymbology {
    <init>();
    <fields>;
}
-keepclassmembers class com.zebra.ai.barcodefinder.sdkcoordinator.model.FeedbackType {
    <init>();
    <fields>;
}
-keepclassmembers enum com.zebra.ai.barcodefinder.sdkcoordinator.enums.DpmMode {
    <fields>;
}
-keepclassmembers enum com.zebra.ai.barcodefinder.sdkcoordinator.enums.PerformanceMode {
    <fields>;
}
-keepclassmembers enum com.zebra.ai.barcodefinder.sdkcoordinator.enums.ModelInput {
    <fields>;
}
-keepclassmembers enum com.zebra.ai.barcodefinder.sdkcoordinator.enums.Resolution {
    <fields>;
}
-keepclassmembers enum com.zebra.ai.barcodefinder.sdkcoordinator.enums.ProcessorType {
    <fields>;
}

# ActionableBarcode — serialized by Gson (ActionableBarcodeJsonStorage).
-keepclassmembers class com.zebra.ai.barcodefinder.application.domain.model.ActionableBarcode {
    <fields>;
}

# =============================================================================
# Zebra AI Vision SDK — init via reflection; SDK AAR ships its own consumer
# rules but we keep an explicit guard here so any future AAR change doesn't
# silently strip classes and bring back the gray Start Scan button.
# =============================================================================
-keep class com.zebra.ai.vision.** { *; }
-dontwarn com.zebra.ai.vision.**

# =============================================================================
# Pendo analytics SDK
# =============================================================================
-keep class sdk.pendo.io.** { *; }
-dontwarn sdk.pendo.io.**

# =============================================================================
# Kotlin coroutines
# =============================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**