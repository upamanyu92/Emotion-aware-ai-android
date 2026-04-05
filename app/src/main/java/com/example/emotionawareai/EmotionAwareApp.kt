package com.example.emotionawareai

import android.app.Application
import android.util.Log
import com.example.emotionawareai.domain.model.LlmOption
import com.example.emotionawareai.engine.ModelDownloader
import com.example.emotionawareai.manager.MemoryManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class EmotionAwareApp : Application() {

    /**
     * [ModelDownloader] is a Hilt singleton. Injecting it here lets us kick
     * off the selected model download the moment the app process is created —
     * well before any Activity or ViewModel is alive.
     */
    @Inject lateinit var modelDownloader: ModelDownloader
    @Inject lateinit var memoryManager: MemoryManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "EmotionAwareAI application starting")

        appScope.launch {
            // Use the saved selection (for returning users) or the pre-configured model
            // (GEMMA_2B) for new installs. Never fall back to device-specific detection.
            val selectedOption = LlmOption.fromId(memoryManager.getSelectedLlmId())
                ?: LlmOption.CONFIGURED_MODEL
            modelDownloader.startDownloadIfAbsent(selectedOption)
        }
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
            TRIM_LEVEL_RUNNING_CRITICAL,
            TRIM_LEVEL_COMPLETE -> Log.w(TAG, "Critical memory trim level=$level")
            else -> Log.d(TAG, "Memory trim level=$level")
        }
    }

    companion object {
        private const val TAG = "EmotionAwareApp"
        // ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL / TRIM_MEMORY_COMPLETE were
        // deprecated in API 35 without a direct replacement; use the raw integer values.
        private const val TRIM_LEVEL_RUNNING_CRITICAL = 15
        private const val TRIM_LEVEL_COMPLETE = 80
    }
}
