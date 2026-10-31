# Debug builds do not use R8. Keep rules minimal for a future release build.
-keepclassmembers class * { @kotlinx.serialization.Serializable *; }
