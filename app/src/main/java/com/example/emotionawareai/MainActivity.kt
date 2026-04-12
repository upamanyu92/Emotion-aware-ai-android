package com.example.emotionawareai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.emotionawareai.ui.ChatViewModel
import com.example.emotionawareai.ui.navigation.MainNavigation
import com.example.emotionawareai.ui.screen.GetStartedScreen
import com.example.emotionawareai.ui.screen.LlmSetupScreen
import com.example.emotionawareai.ui.screen.LoginScreen
import com.example.emotionawareai.ui.screen.SplashScreen
import com.example.emotionawareai.ui.theme.EmotionAwareAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    /** Tracks whether the boot splash has finished its animation. */
    private var splashFinished by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permissions are no longer requested at startup.
        // They are requested on-demand from ChatScreen when the user taps
        // the mic (audio) or camera toggle button.

        setContent {
            val isProThemeEnabled by viewModel.isProThemeEnabled.collectAsStateWithLifecycle()
            // null = not yet determined (loading); false = no profile; true = has profile
            val hasProfile by viewModel.hasUserProfile.collectAsStateWithLifecycle()
            // null = not yet determined; false = not done; true = done
            val isLlmSetup by viewModel.isLlmSetupComplete.collectAsStateWithLifecycle()
            val isGetStartedShown by viewModel.isGetStartedShown.collectAsStateWithLifecycle()
            val llmSetupPhase by viewModel.llmSetupPhase.collectAsStateWithLifecycle()
            val llmSetupError by viewModel.llmSetupError.collectAsStateWithLifecycle()
            val modelDownloadProgress by viewModel.modelDownloadProgress.collectAsStateWithLifecycle()
            val selectedLlmId by viewModel.selectedLlmId.collectAsStateWithLifecycle()

            EmotionAwareAITheme(proThemeEnabled = isProThemeEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        // 1. Boot splash (3 s animation)
                        !splashFinished -> {
                            SplashScreen(onFinished = { splashFinished = true })
                        }

                        // 2. Still loading prefs from Room
                        isLlmSetup == null || hasProfile == null || isGetStartedShown == null -> {
                            /* Loading state — Surface stays dark */
                        }

                        // 3. Get Started carousel not yet shown
                        isGetStartedShown == false -> {
                            GetStartedScreen(
                                onComplete = { viewModel.completeGetStarted() },
                                onSkip = { viewModel.completeGetStarted() }
                            )
                        }

                        // 4. LLM selection not yet done → show setup screen
                        isLlmSetup == false -> {
                            LlmSetupScreen(
                                setupPhase = llmSetupPhase,
                                downloadProgress = modelDownloadProgress,
                                setupError = llmSetupError,
                                availableModels = viewModel.getAllLlmOptionsWithCompatibility(),
                                deviceRamMb = viewModel.deviceTotalRamMb,
                                deviceModel = viewModel.deviceModelName,
                                deviceChipset = viewModel.deviceChipsetName,
                                selectedModelForDownload = com.example.emotionawareai.domain.model.LlmOption.fromId(selectedLlmId)
                                    ?: com.example.emotionawareai.domain.model.LlmOption.CONFIGURED_MODEL,
                                onModelSelected = { viewModel.selectModelForSetup(it) },
                                onSkipSetup = { viewModel.skipLlmSetup() },
                                onRetrySetup = { viewModel.retryLlmSetup() }
                            )
                        }

                        // 5. No user profile → onboarding / login
                        hasProfile == false -> {
                            LoginScreen(
                                onProfileCreated = { name, avatar ->
                                    viewModel.saveUserProfile(name, avatar)
                                },
                                onOnboardingComplete = { areas, frequency ->
                                    viewModel.saveOnboardingPreferences(areas, frequency)
                                }
                            )
                        }

                        // 6. Everything ready → main app
                        else -> MainNavigation(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
