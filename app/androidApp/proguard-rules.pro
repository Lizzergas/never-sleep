# ProGuard / R8 rules for Never Sleep release builds
# https://developer.android.com/studio/build/shrink-code

# Keep Compose runtime and UI (reflection + lambdas used heavily)
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# Keep Kotlin stdlib and metadata
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Keep your own app code that may be accessed by name (e.g. via reflection, deep links, etc.)
# Add specific -keep rules here as your app grows.
-keep class com.lizz.neversleep.** { *; }

# Google Mobile Ads SDK
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.**

# WorkManager / Room (transitive AdMob dependency — required for release minify)
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keep class androidx.work.** { *; }
-keep class androidx.work.impl.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.work.**

# OkHttp / networking (in case you add real ads or server calls)
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep AndroidX Lifecycle / SavedState (common with Compose)
-keep class androidx.lifecycle.** { *; }

# Prevent R8 from stripping important annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# If using any custom views or widgets that are inflated by name
-keep public class * extends android.view.View { *; }

# For the WRITE_SETTINGS and system interactions (keep controller classes)
-keep class com.lizz.neversleep.NeverSleepController { *; }
-keep class com.lizz.neversleep.NeverSleepTileService { *; }
-keep class com.lizz.neversleep.NeverSleepWidgetProvider { *; }
-keep class com.lizz.neversleep.NeverSleepWidgetReceiver { *; }
-keep class com.lizz.neversleep.NeverSleepApplication { *; }
-keep class com.lizz.neversleep.AdBanner { *; }
-keep class com.lizz.neversleep.NeverSleepToggleButton { *; }
