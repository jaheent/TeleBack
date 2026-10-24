# TeleBack Production ProGuard / R8 Rules

# Preserve Kotlin Annotations, Line Numbers, and Attributes for diagnostics
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions
-renamesourcefileattribute SourceFile
-keepattributes SourceFile, LineNumberTable

# Room Database
-keepclassmembers class * {
    @androidx.room.* *;
}
-keep class * extends androidx.room.RoomDatabase
-keep class com.teleback.data.local.** { *; }
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
    @kotlinx.serialization.SerialName *;
}
-keepclassmembers class * extends kotlinx.serialization.KSerializer {
    public static *** INSTANCE;
}
-keepclassmembers class * {
    *** Companion;
}
-keep class com.teleback.data.model.** { *; }
-keep class com.teleback.core.network.** { *; }
-dontwarn kotlinx.serialization.**

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*

# WorkManager
-keep class androidx.work.** { *; }
-keep class com.teleback.core.service.TeleBackSyncWorker { *; }

# Biometrics
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# Cryptography & Core Security Engine
-keep class com.teleback.core.crypto.** { *; }
-keepclassmembers class com.teleback.core.crypto.CryptoEngine {
    public *;
}

# Coroutines
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
