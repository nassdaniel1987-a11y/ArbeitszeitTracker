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

# Google Drive API
-keep class com.google.api.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-dontwarn com.google.api.**

# Google Drive API - Missing optional dependencies
-dontwarn aQute.bnd.annotation.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.naming.**
-dontwarn org.osgi.framework.**
-dontwarn org.apache.logging.log4j.**
-dontwarn com.graphbuilder.**
-dontwarn org.apache.http.**

# Google HTTP Client
-keep class com.google.http-client.** { *; }
-dontwarn com.google.http-client.**

# Google OAuth Client
-keep class com.google.oauth-client.** { *; }
-dontwarn com.google.oauth-client.**

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

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.arbeitszeit.tracker.**$$serializer { *; }
-keepclassmembers class com.arbeitszeit.tracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.arbeitszeit.tracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Gson (falls verwendet)
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

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

# === WIDGETS ===
# AppWidget Provider Classes - Keep all widget classes
-keep class * extends android.appwidget.AppWidgetProvider { *; }
-keep class com.arbeitszeit.tracker.widget.** { *; }

# RemoteViews - Required for widgets
-keep class android.widget.RemoteViews { *; }
-keepclassmembers class android.widget.RemoteViews {
    *;
}

# Widget Resources - Prevent resource shrinking from removing widget resources
# Note: Resource shrinking is controlled separately, these rules help R8 understand usage
-keep class **.R$layout { int widget_*; }
-keep class **.R$drawable { int widget_*; }
-keep class **.R$xml { int widget_*; }
-keep class **.R$color { int widget_*; }
-keep class **.R$id { int widget_*; }
-keep class **.R$string { int widget_*; }
