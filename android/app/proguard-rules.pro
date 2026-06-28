# ============================================================
# Room
# ============================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.**

# ============================================================
# DataStore Preferences
# ============================================================
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn androidx.datastore.**

# ============================================================
# OkHttp
# ============================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ============================================================
# Kotlin coroutines
# ============================================================
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ============================================================
# Kotlin data classes / serialization via reflection
# ============================================================
-keepclassmembers class io.signallq.app.** {
    public <init>(...);
}

# ============================================================
# Compose — tooling only, stripped from release
# ============================================================
-keep class androidx.compose.runtime.** { *; }

# ============================================================
# General Android rules
# ============================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
