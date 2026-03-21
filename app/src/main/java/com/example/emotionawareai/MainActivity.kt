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
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.emotionawareai.ui.ChatViewModel
import com.example.emotionawareai.ui.screen.ChatScreen
import com.example.emotionawareai.ui.screen.LoginScreen
import com.example.emotionawareai.ui.theme.EmotionAwareAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

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

            EmotionAwareAITheme(proThemeEnabled = isProThemeEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (hasProfile) {
                        true -> ChatScreen(viewModel = viewModel)
                        false -> LoginScreen(
                            onProfileCreated = { name, avatar ->
                                viewModel.saveUserProfile(name, avatar)
                            }
                        )
                        // null = still loading from preferences — show nothing (splash-like)
                        null -> { /* Loading state — Surface stays dark */ }
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
