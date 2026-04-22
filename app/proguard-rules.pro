# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Room persistence library rules
-keepclassmembers class com.michaelmoros.debttracker.** {
    @androidx.room.Entity *;
    @androidx.room.Dao *;
    @androidx.room.Database *;
    @androidx.room.TypeConverter *;
}

# Keep Entity classes themselves from being renamed
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase

# General Room rules to prevent issues with generated code
-dontwarn androidx.room.paging.**

# Retain Coroutines and Flow attributes for Room's async operations
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# Kotlin serialization or reflection if used (helps with generic type signatures in Room)
-keepnames class com.michaelmoros.debttracker.** { *; }

# Uncomment this to preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
