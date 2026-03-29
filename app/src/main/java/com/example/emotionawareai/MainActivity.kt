package com.example.emotionawareai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.emotionawareai.ui.ChatViewModel
import com.example.emotionawareai.ui.navigation.MainNavigation
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

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        viewModel.onPermissionsResult(cameraGranted = cameraGranted, audioGranted = audioGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            val isProThemeEnabled by viewModel.isProThemeEnabled.collectAsStateWithLifecycle()
            // null = not yet determined (loading); false = no profile; true = has profile
            val hasProfile by viewModel.hasUserProfile.collectAsStateWithLifecycle()
            // null = not yet determined; false = not done; true = done
            val isLlmSetup by viewModel.isLlmSetupComplete.collectAsStateWithLifecycle()
            val llmSetupPhase by viewModel.llmSetupPhase.collectAsStateWithLifecycle()
            val llmSetupError by viewModel.llmSetupError.collectAsStateWithLifecycle()
            val modelDownloadProgress by viewModel.modelDownloadProgress.collectAsStateWithLifecycle()

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
                        isLlmSetup == null || hasProfile == null -> {
                            /* Loading state — Surface stays dark */
                        }

                        // 3. LLM selection not yet done → show setup screen
                        isLlmSetup == false -> {
                            LlmSetupScreen(
                                detector = viewModel.deviceCapabilityDetector,
                                setupPhase = llmSetupPhase,
                                downloadProgress = modelDownloadProgress,
                                setupError = llmSetupError,
                                onStartSetup = { option -> viewModel.startLlmSetup(option) },
                                onSkipSetup = { viewModel.skipLlmSetup() },
                                onRetrySetup = { viewModel.retryLlmSetup() }
                            )
                        }

                        // 4. No user profile → onboarding / login
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

                        // 5. Everything ready → main app
                        else -> MainNavigation(viewModel = viewModel)
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            viewModel.onPermissionsResult(cameraGranted = true, audioGranted = true)
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}
