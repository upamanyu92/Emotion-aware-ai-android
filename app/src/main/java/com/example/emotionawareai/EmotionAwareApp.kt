package com.example.emotionawareai

import android.app.Application
import android.util.Log
import com.example.emotionawareai.engine.ModelDownloader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EmotionAwareApp : Application() {

    /**
     * [ModelDownloader] is a Hilt singleton. Injecting it here lets us kick
     * off the BitNet model download the moment the app process is created —
     * well before any Activity or ViewModel is alive.
     */
    @Inject lateinit var modelDownloader: ModelDownloader

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "EmotionAwareAI application starting")

        // Start downloading the BitNet model immediately so it is ready
        // (or well on its way) by the time the user reaches the main screen.
        // startDownloadIfAbsent() is a no-op when the file is already present.
        modelDownloader.startDownloadIfAbsent()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "System low memory warning — releasing non-essential caches")
        // Hilt-injected singletons (LLMEngine, etc.) handle their own cleanup
        // via their own onLowMemory / clear() methods called from the ViewModel.
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_COMPLETE -> Log.w(TAG, "Critical memory trim level=$level")
            else -> Log.d(TAG, "Memory trim level=$level")
        }
    }

    companion object {
        private const val TAG = "EmotionAwareApp"
    }
}
