# ProGuard rules for SnapCrop
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-dontwarn com.google.mlkit.**
-keep class com.google.mlkit.** { *; }
-dontwarn androidx.datastore.**
-keep class androidx.datastore.** { *; }
