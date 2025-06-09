-ignorewarnings

-keepclassmembers class **$Companion {
    public kotlinx.serialization.KSerializer serializer(...);
}

-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class io.ktor.** { *; }

-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
