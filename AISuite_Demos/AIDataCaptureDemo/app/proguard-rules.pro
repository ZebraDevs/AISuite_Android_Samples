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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Gson + R8 fix ---
# We persist settings/filter data (BarcodeFilterData, OcrFilterData, FieldFilterConfig, etc.)
# to disk as JSON via Gson, which uses reflection on our data classes at runtime. Without these
# rules, R8 is free to rename fields or merge structurally-similar classes (e.g. our sealed
# FieldFilterConfig's two near-identical subclasses), which breaks Gson deserialization —
# reproducible only in release builds (debug builds skip R8 entirely), matching the
# ClassCastException crash traced to this on 2026-07-13.
#
# Gson bundles its own consumer rules protecting the library's internals, so we don't need (and
# lint correctly flags) a blanket "-keep class com.google.gson.** { *; }" — only -dontwarn plus
# keeping our own model classes, per Gson's own documented R8 guidance.
-dontwarn sun.misc.**

# Keep every field/class shape our own Gson-persisted model classes rely on, so R8 can't rename
# or merge them.
-keep class com.zebra.aidatacapturedemo.data.** { *; }