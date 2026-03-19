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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.emotionawareai.billing.PremiumPlanType
import com.example.emotionawareai.ui.component.ActivityCaptionOverlay
import com.example.emotionawareai.ui.component.EmotionIndicator
import com.example.emotionawareai.ui.component.MessageBubble
import com.example.emotionawareai.ui.component.VoiceInputButton
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
    val isPremiumUser by viewModel.isPremiumUser.collectAsStateWithLifecycle()
    val isBillingReady by viewModel.isBillingReady.collectAsStateWithLifecycle()
    val isAiAgentActive by viewModel.isAiAgentActive.collectAsStateWithLifecycle()
    val premiumOffers by viewModel.premiumOffers.collectAsStateWithLifecycle()
    val isPurchaseInProgress by viewModel.isPurchaseInProgress.collectAsStateWithLifecycle()
    val isRestoreInProgress by viewModel.isRestoreInProgress.collectAsStateWithLifecycle()
    val toneInsight by viewModel.toneInsight.collectAsStateWithLifecycle()
    val isProThemeEnabled by viewModel.isProThemeEnabled.collectAsStateWithLifecycle()
    val isExportWithInsights by viewModel.isExportWithInsights.collectAsStateWithLifecycle()
    val exportPayload by viewModel.exportPayload.collectAsStateWithLifecycle()

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

    val gradientTransition = rememberInfiniteTransition(label = "bgGradient")
    val gradientShift by gradientTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientShift"
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
                            style = MaterialTheme.typography.titleLarge
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
                    if (!isPremiumUser) {
                        IconButton(onClick = { activity?.let(viewModel::startPremiumUpgrade) }) {
                            Icon(Icons.Default.Diamond, contentDescription = "Upgrade premium")
                        }
                    }
                    IconButton(onClick = { viewModel.toggleContinuousConversation() }) {
                        Icon(
                            imageVector = if (isContinuousConversationEnabled) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = if (isContinuousConversationEnabled) {
                                "Disable continuous conversation"
                            } else {
                                "Enable continuous conversation"
                            }
                        )
                    }
                    IconButton(onClick = { viewModel.toggleTts() }) {
                        Icon(
                            imageVector = if (isTtsEnabled) Icons.AutoMirrored.Filled.VolumeUp
                                          else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (isTtsEnabled) "Disable TTS" else "Enable TTS"
                        )
                    }
                    IconButton(onClick = { viewModel.startNewConversation() }) {
                        Icon(Icons.Filled.Add, contentDescription = "New conversation")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A).copy(alpha = 0.9f + (0.1f * gradientShift)),
                            Color(0xFF1E1B4B),
                            Color(0xFF111827).copy(alpha = 0.9f + (0.1f * (1f - gradientShift)))
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = { Text(if (isAiAgentActive) "AI active" else "AI inactive") },
                        leadingIcon = {
                            Icon(Icons.Default.Bolt, contentDescription = null)
                        }
                    )
                    AssistChip(
                        onClick = { },
                        label = { Text("Face: ${currentEmotion.displayName}") }
                    )
                    AssistChip(
                        onClick = { },
                        label = { Text("Voice: ${audioToneEmotion.displayName}") }
                    )
                    toneInsight?.let { insight ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text("Tone: ${insight.label} ${(insight.confidence * 100).toInt()}%")
                            }
                        )
                    }
                }

                if (!isPremiumUser) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.24f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(id = com.example.emotionawareai.R.string.premium_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Unlock long memory, export, pro themes, and advanced tone insights.",
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            premiumOffers.forEach { offer ->
                                Button(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        activity?.let { viewModel.startPremiumUpgrade(it, offer.planType) }
                                    },
                                    enabled = isBillingReady && offer.available && !isPurchaseInProgress,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (offer.planType == PremiumPlanType.MONTHLY) {
                                            Color(0xFF7C3AED)
                                        } else {
                                            Color(0xFF2563EB)
                                        }
                                    )
                                ) {
                                    Text("${offer.title} • ${offer.priceText}")
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.restorePurchases()
                                    },
                                    enabled = !isRestoreInProgress
                                ) {
                                    Text(if (isRestoreInProgress) "Restoring..." else "Restore")
                                }
                                Button(onClick = { viewModel.retryBillingConnection() }) {
                                    Text("Retry")
                                }
                            }
                        }
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { viewModel.toggleContinuousConversation() },
                        label = {
                            Text(
                                if (isContinuousConversationEnabled) "Live conversation on"
                                else "Live conversation off"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isContinuousConversationEnabled) Icons.Filled.Mic
                                else Icons.Filled.MicOff,
                                contentDescription = null
                            )
                        }
                    )

                    AssistChip(
                        onClick = { },
                        label = {
                            Text(if (cameraGranted) "Camera context on" else "Camera context off")
                        }
                    )

                    if (!isPremiumUser) {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                activity?.let(viewModel::startPremiumUpgrade)
                            },
                            enabled = isBillingReady,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFC947),
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Upgrade")
                        }
                    } else {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.toggleProTheme()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isProThemeEnabled) Color(0xFF10B981) else Color(0xFF334155)
                            )
                        ) {
                            Text(if (isProThemeEnabled) "Pro Theme On" else "Pro Theme Off")
                        }

                        Button(onClick = { viewModel.toggleExportInsights() }) {
                            Text(if (isExportWithInsights) "Insights in Export" else "Transcript Export")
                        }

                        Button(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.prepareExportPayload()
                        }) {
                            Text("Export")
                        }
                    }
                }

                // ── Input bar ────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .imePadding()
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Message or keep talking in live mode…") },
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
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            }
                        )
                    )

                    VoiceInputButton(
                        isListening = isListening,
                        onStartListening = { viewModel.startVoiceInput() },
                        onStopListening = { viewModel.stopVoiceInput() }
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isGenerating)
                                    Color(0xFF7C3AED)
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = Color.White
                        )
                    }
                }
            }

            // ── Emotion indicator overlay (top-right) ─────────────────────
            EmotionIndicator(
                emotion = effectiveEmotion,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 52.dp, end = 8.dp)
            )

            // ── Floating camera preview (bottom-right) ────────────────────
            if (cameraGranted) {
                CameraPreviewOverlay(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 112.dp, end = 16.dp),
                    onBitmapFrame = viewModel::onCameraFrame
                )
            }
        }
    }
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
            .size(128.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                2.dp,
                Color.White.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
    )
}
