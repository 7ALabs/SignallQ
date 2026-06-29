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
# OkHttp e Okio incluem consumer proguard rules nos proprios JARs.
# -keep class okhttp3.** e -keep interface okhttp3.** eram redundantes
# e mantinham todas as classes desnecessariamente, aumentando o APK.
-dontwarn okhttp3.**
-dontwarn okio.**
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
# Compose
# ============================================================
# O Compose Compiler e Runtime incluem consumer proguard rules nos proprios JARs.
# -keep class androidx.compose.runtime.** era redundante e mantinha todo o runtime
# sem necessidade, impedindo o R8 de otimizar. Removido.

# ============================================================
# General Android rules
# ============================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
