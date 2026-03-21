# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Shizuku API
-keep class dev.rikka.shizuku.** { *; }

# Keep Room entities
-keep class dev.perfoverlay.data.** { *; }

# Keep data classes used by Room
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
