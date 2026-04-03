# PerfOverlay ProGuard Rules

# Keep Shizuku API
-keep class dev.rikka.shizuku.** { *; }

# Keep Room entities and database classes
-keep class dev.perfoverlay.data.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Keep data classes used for serialization/persistence
-keepclassmembers class dev.perfoverlay.data.PerformanceStats { *; }
-keepclassmembers class dev.perfoverlay.data.OverlayConfig { *; }

# Kotlin specific
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Compose
-dontwarn androidx.compose.**

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
