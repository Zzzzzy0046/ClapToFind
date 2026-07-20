# Preserve app classes
-keep class com.claptofind.phone.** { *; }
# MediaPipe
-dontwarn com.google.mediapipe.**
-keep class com.google.mediapipe.** { *; }
# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }
# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
# WorkManager
-keep class * extends androidx.work.Worker
