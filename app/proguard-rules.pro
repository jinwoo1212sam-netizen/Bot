# ULTRON ProGuard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class com.ultron.assistant.ai.** { *; }
-keepclassmembers class com.ultron.assistant.command.** { *; }
-keepclassmembers class com.ultron.assistant.data.** { *; }
