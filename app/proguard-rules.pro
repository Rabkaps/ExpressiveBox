# Proguard rules for ExpressiveBox
-keep class com.hambalapps.expressivebox.vpn.** { *; }
-keep class com.hambalapps.expressivebox.data.** { *; }
-keep class libbox.** { *; }
-keep class go.** { *; }

# Keep all native JNI callbacks
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep serialized names and classes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# ML Kit Barcode Scanning Keep Rules to prevent R8 obfuscation/stripping crashes
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.android.odml.** { *; }
-keep class com.google.android.gms.common.internal.safeparcel.** { *; }
