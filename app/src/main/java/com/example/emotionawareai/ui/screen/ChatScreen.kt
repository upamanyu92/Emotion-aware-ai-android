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
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.emotionawareai.ui.ChatViewModel
import com.example.emotionawareai.ui.component.ActivityCaptionOverlay
import com.example.emotionawareai.ui.component.EmotionIndicator
import com.example.emotionawareai.ui.component.MessageBubble
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
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val currentEmotion by viewModel.currentEmotion.collectAsStateWithLifecycle()
    val audioToneEmotion by viewModel.audioToneEmotion.collectAsStateWithLifecycle()
    val effectiveEmotion by viewModel.effectiveEmotion.collectAsStateWithLifecycle()
    val activityCaptions by viewModel.activityCaptions.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val isModelLoaded by viewModel.isModelLoaded.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val cameraGranted by viewModel.cameraPermissionGranted.collectAsStateWithLifecycle()
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()
    val isContinuousConversationEnabled by viewModel.isContinuousConversationEnabled.collectAsStateWithLifecycle()
    val isAiAgentActive by viewModel.isAiAgentActive.collectAsStateWithLifecycle()
    val toneInsight by viewModel.toneInsight.collectAsStateWithLifecycle()
    val isProThemeEnabled by viewModel.isProThemeEnabled.collectAsStateWithLifecycle()
    val isExportWithInsights by viewModel.isExportWithInsights.collectAsStateWithLifecycle()
    val exportPayload by viewModel.exportPayload.collectAsStateWithLifecycle()
    val premiumFeaturesEnabled by viewModel.premiumFeaturesGloballyEnabled.collectAsStateWithLifecycle()

    val activity = LocalContext.current as? Activity
    val haptics = LocalHapticFeedback.current

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Scroll to the latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Show error snackbar
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

    // ── Multi-stop animated background gradient ───────────────────────────────
    val gradientTransition = rememberInfiniteTransition(label = "bgGradient")
    val gradientShift by gradientTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientShift"
    )
    // Accent orb pulse (drives the radial glow blobs)
    val orbPulse by gradientTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulse"
    )

    // Animated gradient colors interpolating between deep-navy anchor stops
    val gradColor0 = lerp(GradStart, GradMid1, gradientShift)
    val gradColor1 = lerp(GradMid1, GradMid2, gradientShift)
    val gradColor2 = lerp(GradMid2, GradEnd, 1f - gradientShift)

    // Send-button scale micro-animation
    val sendScale by animateFloatAsState(
        targetValue = if (inputText.isNotBlank() && !isGenerating) 1f else 0.85f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "sendScale"
    )

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(id = com.example.emotionawareai.R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        if (!isModelLoaded) {
                            Text(
                                text = "• stub mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleContinuousConversation()
                    }) {
                        Icon(
                            imageVector = if (isContinuousConversationEnabled) Icons.Filled.MicOff
                                          else Icons.Filled.Mic,
                            contentDescription = if (isContinuousConversationEnabled)
                                "Disable continuous conversation"
                            else
                                "Enable continuous conversation",
                            tint = if (isContinuousConversationEnabled) NeonCyan else Color.White
                        )
                    }
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleTts()
                    }) {
                        Icon(
                            imageVector = if (isTtsEnabled) Icons.AutoMirrored.Filled.VolumeUp
                                          else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (isTtsEnabled) "Disable TTS" else "Enable TTS",
                            tint = if (isTtsEnabled) NeonCyan else Color.White
                        )
                    }
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.startNewConversation()
                    }) {
                        Icon(
                            Icons.Filled.Add,
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(gradColor0, gradColor1, gradColor2)
                    )
                )
                .padding(paddingValues)
        ) {
            // ── Accent glow orbs (NeoPOP depth effect) ────────────────────────
            Box(
                modifier = Modifier
                    .size((280 * orbPulse).dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-60).dp, y = (-40).dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonPurple.copy(alpha = 0.18f * orbPulse),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
                    .blur(40.dp)
            )
            Box(
                modifier = Modifier
                    .size((200 * (1f - orbPulse * 0.2f)).dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 40.dp, y = 60.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonCyan.copy(alpha = 0.12f * orbPulse),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
                    .blur(50.dp)
            )

            Column(modifier = Modifier.fillMaxSize()) {

                // ── Status chips row ─────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NeoChip(
                        label = if (isAiAgentActive) "AI active" else "AI inactive",
                        icon = { Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        active = isAiAgentActive
                    )
                    NeoChip(
                        label = "Face: ${currentEmotion.displayName}",
                        active = true
                    )
                    NeoChip(
                        label = "Voice: ${audioToneEmotion.displayName}",
                        active = true
                    )
                    toneInsight?.let { insight ->
                        NeoChip(
                            label = "Tone: ${insight.label} ${(insight.confidence * 100).toInt()}%",
                            active = true
                        )
                    }
                }

                // ── Fixed camera preview (embedded, not floating) ─────────────
                AnimatedVisibility(
                    visible = cameraGranted,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        NeonPurple.copy(alpha = 0.5f),
                                        NeonCyan.copy(alpha = 0.4f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        CameraPreviewOverlay(
                            modifier = Modifier.fillMaxSize(),
                            onBitmapFrame = viewModel::onCameraFrame
                        )
                        // Glassmorphism header label
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassCard)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(NeonCyan, CircleShape)
                            )
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                        EmotionIndicator(
                            emotion = effectiveEmotion,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        )
                    }
                }

                // ── Message list ─────────────────────────────────────────────
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        MessageBubble(message = message)
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                // ── Activity caption strip ───────────────────────────────────
                AnimatedVisibility(
                    visible = cameraGranted && activityCaptions.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ActivityCaptionOverlay(captions = activityCaptions)
                }

                // ── Action chips row ─────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NeoChip(
                        label = if (isContinuousConversationEnabled) "Live on" else "Live off",
                        icon = {
                            Icon(
                                imageVector = if (isContinuousConversationEnabled) Icons.Filled.Mic
                                              else Icons.Filled.MicOff,
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
                        label = if (cameraGranted) "Camera on" else "Camera off",
                        active = cameraGranted
                    )

                    NeoChip(
                        label = if (isProThemeEnabled) "Pro Theme" else "Default",
                        active = isProThemeEnabled,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleProTheme()
                        }
                    )

                    NeoChip(
                        label = if (isExportWithInsights) "Insights" else "Transcript",
                        active = isExportWithInsights,
                        onClick = { viewModel.toggleExportInsights() }
                    )

                    NeoChip(
                        label = "Export",
                        active = true,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.prepareExportPayload()
                        }
                    )

                    // Remote kill-switch toggle (admin/debug feature)
                    NeoChip(
                        label = if (premiumFeaturesEnabled) "Features ✓" else "Features ✗",
                        active = premiumFeaturesEnabled,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setPremiumFeaturesEnabled(!premiumFeaturesEnabled)
                        }
                    )
                }

                // ── Input bar ────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, GradStart.copy(alpha = 0.9f))
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = GlassBorder,
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .imePadding()
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Message or keep talking…",
                                color = Color.White.copy(alpha = 0.35f)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple.copy(alpha = 0.7f),
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White.copy(alpha = 0.85f),
                            cursorColor = NeonCyan,
                            focusedContainerColor = GlassCard,
                            unfocusedContainerColor = Color(0x0DFFFFFF)
                        )
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
                            .scale(sendScale)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isGenerating)
                                    Brush.radialGradient(
                                        listOf(NeonPurple, NeonCyan.copy(alpha = 0.6f))
                                    )
                                else
                                    Brush.radialGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                        )
                                    )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
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
            }
        }
    }
}

// ── NeoPOP-style assist chip ───────────────────────────────────────────────────
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
                color = if (active) NeonCyan else Color.White.copy(alpha = 0.6f)
            )
        },
        leadingIcon = icon,
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (active) NeonPurple.copy(alpha = 0.18f) else GlassCard,
            labelColor = if (active) NeonCyan else Color.White.copy(alpha = 0.6f),
            leadingIconContentColor = if (active) NeonCyan else Color.White.copy(alpha = 0.5f)
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = if (active) NeonPurple.copy(alpha = 0.45f) else GlassBorder
        )
    )
}

// ── Colour lerp is provided by androidx.compose.ui.graphics.lerp (imported above)

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
                previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
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
