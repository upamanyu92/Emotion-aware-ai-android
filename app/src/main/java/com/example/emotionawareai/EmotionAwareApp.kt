package com.example.emotionawareai

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EmotionAwareApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "EmotionAwareAI application starting")
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
