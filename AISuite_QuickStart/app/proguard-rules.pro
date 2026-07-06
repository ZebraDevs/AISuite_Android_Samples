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

# ── ONNX Runtime ──────────────────────────────────────────────────────────────────────────────
# OrtSession.run() is a JNI method that calls back into Java to construct TensorInfo, OnnxTensor,
# and related value classes. R8 cannot see these call sites and will strip the constructors,
# causing NoSuchMethodError at runtime. Keep the entire ai.onnxruntime package.
-keep class ai.onnxruntime.** { *; }

# ── TensorFlow Lite ───────────────────────────────────────────────────────────────────────────
# TFLite Interpreter loads delegate and op-resolver classes reflectively.
-keep class org.tensorflow.lite.** { *; }

# ── ML Kit ────────────────────────────────────────────────────────────────────────────────────
# ML Kit registers internal components reflectively.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }