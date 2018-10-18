# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/bshao/opt/android-sdk-linux/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-ignorewarnings
-dontwarn
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-keepattributes SourceFile,LineNumberTable,*Annotation*

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# Phantom 3.x Replace Gradle Plugin 会将插件中父类为 android.app.Fragment 的类的父类替换为 SysFragmentProxy ，
# 因此需要在宿主中 keep 该类
-keep class com.wlqq.phantom.library.proxy.SysFragmentProxy { *; }
-keep class com.wlqq.phantom.library.proxy.SysDialogFragmentProxy { *; }
-keep class com.wlqq.phantom.library.proxy.SysListFragmentProxy { *; }
-keep class com.wlqq.phantom.library.proxy.SysPreferenceFragmentProxy { *; }
-keep class com.wlqq.phantom.library.proxy.PhantomActivityAware { *; }

# Phantom Communication Lib
-keep class com.wlqq.phantom.communication.** { *; }

# Phantom Service method
-keepclassmembers class * {
    @com.wlqq.phantom.communication.RemoteMethod <methods>;
}

# ARTUtils
-keep class com.taobao.android.dex.interpret.ARTUtils { *; }
