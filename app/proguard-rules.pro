# Add project specific ProGuard rules here.
-keep class com.arbeitszeit.tracker.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    *;
}
