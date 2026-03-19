# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools proguard/proguard-android.txt

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Hilt
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# MediaPipe
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# LLM Engine JNI
-keep class com.example.emotionawareai.engine.LLMEngine {
    native <methods>;
    public *;
}

# Keep data models
-keep class com.example.emotionawareai.data.model.** { *; }
-keep class com.example.emotionawareai.domain.model.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Annotation processor classes referenced from packaged metadata at shrink time.
-dontwarn javax.annotation.processing.**
-dontwarn javax.lang.model.**

