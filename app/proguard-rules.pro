# AlbionDataPro Enterprise R8 / ProGuard Obfuscation & Security Rules

# Repackage obfuscated classes to hide package layout
-repackageclasses ''
-allowaccessmodification

# Obfuscate local variables & line numbers
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,Annotation

# Keep Android Entry Points
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep Serialized Model Classes for JSON Parsing
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep Jetpack Compose & Material3
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Security & Anti-Cheat Classes
-keep class com.example.albionmarketv2.AntiCheatManager { *; }
-keep class com.example.albionmarketv2.CryptoSecurityUtils { *; }
-keep class com.example.albionmarketv2.DeviceHardwareManager { *; }
-keep class com.example.albionmarketv2.LicenseManager { *; }
