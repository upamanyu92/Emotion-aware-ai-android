package com.example.emotionawareai.ui.screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.emotionawareai.R
import com.example.emotionawareai.ui.ChatViewModel
import com.example.emotionawareai.ui.component.AgentPresenceAnimation
import com.example.emotionawareai.ui.component.DailyCheckInSheet
import com.example.emotionawareai.ui.component.EmotionIndicator
import com.example.emotionawareai.ui.component.PrivacyNoticeDialog
import com.example.emotionawareai.ui.component.VoiceInputButton
import com.example.emotionawareai.ui.theme.GlassBorder
import com.example.emotionawareai.ui.theme.GlassCard
import com.example.emotionawareai.ui.theme.GradEnd
import com.example.emotionawareai.ui.theme.GradMid1
import com.example.emotionawareai.ui.theme.GradMid2
import com.example.emotionawareai.ui.theme.GradStart
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonPurple
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val effectiveEmotion by viewModel.effectiveEmotion.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isModelLoaded by viewModel.isModelLoaded.collectAsStateWithLifecycle()
    val isModelAvailable by viewModel.isModelAvailable.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val cameraGranted by viewModel.cameraPermissionGranted.collectAsStateWithLifecycle()
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()
    val isContinuousConversationEnabled by viewModel.isContinuousConversationEnabled.collectAsStateWithLifecycle()
    val isCameraEnabled by viewModel.isCameraEnabled.collectAsStateWithLifecycle()
    val isCameraPreviewVisible by viewModel.isCameraPreviewVisible.collectAsStateWithLifecycle()
    val isCaptionsEnabled by viewModel.isCaptionsEnabled.collectAsStateWithLifecycle()
    val isVoiceModeActive by viewModel.isVoiceModeActive.collectAsStateWithLifecycle()
    val exportPayload by viewModel.exportPayload.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userAvatar by viewModel.userAvatar.collectAsStateWithLifecycle()
    val speechCaption by viewModel.speechCaption.collectAsStateWithLifecycle()
    val showDailyCheckIn by viewModel.showDailyCheckIn.collectAsStateWithLifecycle()
    val showPrivacyNotice by viewModel.showPrivacyNotice.collectAsStateWithLifecycle()

    val activity = LocalContext.current as? Activity
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearError()
            }
        }
    }

    LaunchedEffect(exportPayload) {
        val payload = exportPayload ?: return@LaunchedEffect
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, payload)
        }
        activity?.startActivity(Intent.createChooser(shareIntent, "Share conversation"))
        viewModel.clearExportPayload()
    }

    val cameraWallpaperVisible = cameraGranted && isCameraEnabled && isCameraPreviewVisible

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(NeonPurple.copy(alpha = 0.25f))
                                .border(1.dp, NeonPurple.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(userAvatar.ifBlank { "😊" }, style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (userName.isNotBlank()) "Hi, $userName" else stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = when {
                                    isSpeaking -> "Tara is speaking"
                                    isGenerating -> "Tara is thinking"
                                    isListening -> "Listening to you"
                                    else -> "Immersive conversation view"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.68f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleVoiceMode()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Hearing,
                            contentDescription = if (isVoiceModeActive) "Exit voice mode" else "Enter voice mode",
                            tint = if (isVoiceModeActive) NeonCyan else Color.White
                        )
                    }
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleContinuousConversation()
                    }) {
                        Icon(
                            imageVector = if (isContinuousConversationEnabled) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = if (isContinuousConversationEnabled) {
                                "Disable continuous conversation"
                            } else {
                                "Enable continuous conversation"
                            },
                            tint = if (isContinuousConversationEnabled) NeonCyan else Color.White
                        )
                    }
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleTts()
                    }) {
                        Icon(
                            imageVector = if (isTtsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (isTtsEnabled) "Disable TTS" else "Enable TTS",
                            tint = if (isTtsEnabled) NeonCyan else Color.White
                        )
                    }
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.startNewConversation()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "New conversation",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = GradMid1.copy(alpha = 0.95f),
                    contentColor = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(GradStart, GradMid1, GradMid2, GradEnd)))
                .padding(paddingValues)
        ) {
            if (cameraWallpaperVisible) {
                CameraPreviewOverlay(
                    modifier = Modifier.fillMaxSize(),
                    onBitmapFrame = viewModel::onCameraFrame
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                GradStart.copy(alpha = 0.28f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.76f)
                            )
                        )
                    )
            )

            if (!cameraWallpaperVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    NeonPurple.copy(alpha = 0.16f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        label = if (isModelLoaded) "BitNet live" else if (isModelAvailable) "Model ready" else "Stub mode",
                        active = isModelLoaded || isModelAvailable,
                        icon = Icons.Filled.Bolt
                    )
                    StatusPill(
                        label = if (cameraWallpaperVisible) "Camera wallpaper" else if (isCameraEnabled) "Camera hidden" else "Camera off",
                        active = cameraWallpaperVisible,
                        icon = if (cameraWallpaperVisible) Icons.Filled.CameraAlt else Icons.Filled.VideocamOff
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    EmotionIndicator(emotion = effectiveEmotion)
                }

                Spacer(modifier = Modifier.weight(1f))

                AgentPresenceAnimation(
                    isListening = isListening,
                    isGenerating = isGenerating,
                    isSpeaking = isSpeaking,
                    speechCaption = speechCaption,
                    captionsVisible = isCaptionsEnabled,
                    userName = userName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeoChip(
                        label = if (isContinuousConversationEnabled) "Live on" else "Live off",
                        icon = {
                            Icon(
                                imageVector = if (isContinuousConversationEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        active = isContinuousConversationEnabled,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleContinuousConversation()
                        }
                    )
                    NeoChip(
                        label = if (isCameraEnabled) "Cam on" else "Cam off",
                        icon = {
                            Icon(
                                imageVector = if (isCameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        active = isCameraEnabled && cameraGranted,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleCamera()
                        }
                    )
                    if (isCameraEnabled && cameraGranted) {
                        NeoChip(
                            label = if (isCameraPreviewVisible) "Wallpaper" else "Hidden",
                            icon = {
                                Icon(
                                    imageVector = if (isCameraPreviewVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            active = isCameraPreviewVisible,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.toggleCameraPreviewVisible()
                            }
                        )
                    }
                    NeoChip(
                        label = if (isCaptionsEnabled) "Captions" else "No captions",
                        icon = {
                            Icon(
                                imageVector = if (isCaptionsEnabled) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionDisabled,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        active = isCaptionsEnabled,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleCaptions()
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.28f))
                        .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Send a message or use your voice", color = Color.White.copy(alpha = 0.45f)) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                            focusedBorderColor = NeonCyan.copy(alpha = 0.45f),
                            unfocusedBorderColor = GlassBorder,
                            cursorColor = NeonCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.45f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.45f)
                        ),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank() && !isGenerating) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        })
                    )

                    VoiceInputButton(
                        isListening = isListening,
                        onStartListening = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.startVoiceInput()
                        },
                        onStopListening = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.stopVoiceInput()
                        }
                    )

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isGenerating) {
                                    Brush.radialGradient(listOf(NeonPurple, NeonCyan.copy(alpha = 0.68f)))
                                } else {
                                    Brush.radialGradient(listOf(Color.White.copy(alpha = 0.14f), Color.Transparent))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && !isGenerating) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank() && !isGenerating,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            if (showDailyCheckIn) {
                DailyCheckInSheet(
                    onSubmit = { score, note -> viewModel.submitMoodCheckIn(score, note) },
                    onDismiss = { viewModel.dismissDailyCheckIn() }
                )
            }

            if (showPrivacyNotice) {
                PrivacyNoticeDialog(onDismiss = { viewModel.dismissPrivacyNotice() })
            }
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.26f))
            .border(1.dp, if (active) NeonCyan.copy(alpha = 0.35f) else GlassBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) NeonCyan else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun NeoChip(
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) NeonCyan else Color.White.copy(alpha = 0.68f)
            )
        },
        leadingIcon = icon,
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (active) NeonPurple.copy(alpha = 0.16f) else GlassCard,
            labelColor = if (active) NeonCyan else Color.White.copy(alpha = 0.68f),
            leadingIconContentColor = if (active) NeonCyan else Color.White.copy(alpha = 0.58f)
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = if (active) NeonPurple.copy(alpha = 0.42f) else GlassBorder
        )
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@SuppressLint("MissingPermission")
@Composable
private fun CameraPreviewOverlay(
    modifier: Modifier = Modifier,
    onBitmapFrame: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                val bitmap = imageProxy.toBitmap()
                                imageProxy.close()
                                onBitmapFrame(bitmap)
                            }
                        }

                    runCatching {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    }
                }, context.mainExecutor)
            }
        },
        modifier = modifier
    )
}
