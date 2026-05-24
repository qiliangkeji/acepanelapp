# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# Keep the R class since we're using reflection in some places
-keep class **.R$* {*;}

# Keep classes that are accessed via reflection
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Kotlin-specific rules
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keep class kotlin.jvm.functions.** { *; }
-keep class kotlin.jvm.internal.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Compose-specific rules
-keep class androidx.compose.** { *; }
-keep @androidx.compose.runtime.Composable class * { *; }
-keep @androidx.compose.ui.graphics.vector.WithVectoricons class * { *; }

# Ktor client rules
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Serialization
-keep @kotlinx.serialization.SerialName class * { *; }
-keep class * {
    <fields>;
    <init>(); 
}
-keep class **.*Companion {
    <fields>;
}
-keepclassmembers class **.*Companion {
    <fields>;
    <methods>;
}
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Multiplatform Settings
-keep class com.russhwolf.** { *; }
-keep interface com.russhwolf.** { *; }

# ViewModel
-keep class * extends androidx.lifecycle.ViewModel
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
    <fields>;
}

# Keep all public classes of your application
-keep public class com.acepanel.app.** {
    public protected <methods>;
    public protected <fields>;
}

# Keep application class
-keep public class * extends android.app.Application
-keep public class * extends androidx.multidex.MultiDexApplication

# Keep activities
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment

# Keep views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep custom Application class
-keep class com.acepanel.app.MainApplication { *; }

# Enum support for Moshi/Retrofit if used
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
