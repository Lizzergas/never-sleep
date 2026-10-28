# Android components declared in the manifest are preserved by AGP. Compose,
# Google Mobile Ads, UMP, and AndroidX supply their own consumer keep rules.
# Package-wide keep rules prevent R8 from optimizing almost the entire app.
# Add a targeted rule here only for a demonstrated reflection or JNI entry point.

# Retain useful source locations for Play Console crash reports and retracing.
-keepattributes SourceFile,LineNumberTable

# AdMob's WorkManager dependency uses Room 2.2.5. Its consumer rules preserve
# database class names, but R8 full mode also needs the constructor that Room
# invokes through Class.newInstance() during WorkManager initialization.
-keepclassmembers class androidx.work.impl.WorkDatabase_Impl {
    public <init>();
}

# graphics-path 1.0.1 ships stripped native binaries, but this app never uses
# path iteration. Release packaging omits that unused library and ignores its
# JNI keep rule. Prevent inlining/merging, then require the entire package to
# disappear. Any new path-iteration use fails the build instead of shipping
# code that would load a missing native library. Revisit this guard and the
# packaging exclusion when upgrading graphics-path or using path iteration.
-keep,allowshrinking class androidx.graphics.path.** {
    *;
}
-checkdiscard class androidx.graphics.path.**
