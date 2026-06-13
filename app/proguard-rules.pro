# ===================================
# RentEase ProGuard Configuration
# ===================================

# General
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose
-dontpreverify

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod

# =============================
# Android
# =============================
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# =============================
# Firebase
# =============================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep Firestore data classes (for toObject() deserialization)
-keep class com.example.rentease.Item { *; }
-keep class com.example.rentease.RentalRequest { *; }
-keep class com.example.rentease.SupportTicket { *; }
-keep class com.example.rentease.ReportItem { *; }

# =============================
# Glide
# =============================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$* { *; }
-dontwarn com.bumptech.glide.**

# =============================
# Material Design
# =============================
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
-keep class androidx.** { *; }
-dontwarn androidx.**

# =============================
# OkHttp / Networking (if any)
# =============================
-dontwarn okhttp3.**
-dontwarn okio.**
