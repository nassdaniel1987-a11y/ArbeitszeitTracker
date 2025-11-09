# Add project specific ProGuard rules here.

# Keep application classes
-keep class com.arbeitszeit.tracker.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <fields>;
}
-keepclassmembers class * extends androidx.room.RoomDatabase {
    *;
}
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}

# Apache POI (Excel)
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.schemas.**
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.schemas.** { *; }

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# OSMDroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Compose (retain parameter names for better debugging)
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** {
    *;
}

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keep class androidx.work.impl.WorkManagerInitializer

# Preserve line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Preserve annotations
-keepattributes *Annotation*

# Preserve generic signatures
-keepattributes Signature

# Preserve Serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
