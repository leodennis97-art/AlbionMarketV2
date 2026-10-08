# R8 / ProGuard Rules for AlbionMarketV2

# Keep all app classes to prevent stripping/renaming issues with Compose, Navigation & Reflection
-keep class com.example.albionmarketv2.** { *; }

# Keep Android Entry Points
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep Jetpack Compose & Material3
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Play Billing
-keep class com.android.billingclient.** { *; }

# Attributes required for reflection and serialization
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable
