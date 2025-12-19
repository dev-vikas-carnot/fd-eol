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

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn com.fasterxml.jackson.core.type.TypeReference
-dontwarn com.zebra.sdk.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
 -keep class com.carnot.fd.eol.data.** {*;}

-keep class com.carnot.fd.eol.features.*.data.** { *; }

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
 -keep,allowobfuscation,allowshrinking interface retrofit2.Call
 -keep,allowobfuscation,allowshrinking class retrofit2.Response

 # With R8 full mode generic signatures are stripped for classes that are not
 # kept. Suspend functions are wrapped in continuations where the type argument
 # is used.
 -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

 # Copied from missingrules.txt
 # Please add these rules to your existing keep rules in order to suppress warnings.
 # This is generated automatically by the Android Gradle plugin.
 -dontwarn javax.naming.Binding
 -dontwarn javax.naming.NamingEnumeration
 -dontwarn javax.naming.NamingException
 -dontwarn javax.naming.directory.Attribute
 -dontwarn javax.naming.directory.Attributes
 -dontwarn javax.naming.directory.DirContext
 -dontwarn javax.naming.directory.InitialDirContext
 -dontwarn javax.naming.directory.SearchControls
 -dontwarn javax.naming.directory.SearchResult
 -dontwarn org.bouncycastle.jsse.BCSSLParameters
 -dontwarn org.bouncycastle.jsse.BCSSLSocket
 -dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
 -dontwarn org.conscrypt.Conscrypt$Version
 -dontwarn org.conscrypt.Conscrypt
 -dontwarn org.conscrypt.ConscryptHostnameVerifier
 -dontwarn org.openjsse.javax.net.ssl.SSLParameters
 -dontwarn org.openjsse.javax.net.ssl.SSLSocket
 -dontwarn org.openjsse.net.ssl.OpenJSSE