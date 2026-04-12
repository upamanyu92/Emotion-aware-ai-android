package com.example.emotionawareai.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.emotionawareai.domain.model.LlmOption
import com.example.emotionawareai.domain.model.PiperVoice
import com.example.emotionawareai.domain.model.TtsBackend
import com.example.emotionawareai.domain.model.TtsVoiceProfile
import com.example.emotionawareai.ui.ChatViewModel
import com.example.emotionawareai.ui.ModelInstallState
import com.example.emotionawareai.ui.theme.GlassBorder
import com.example.emotionawareai.ui.theme.GlassCard
import com.example.emotionawareai.ui.theme.GradEnd
import com.example.emotionawareai.ui.theme.GradMid1
import com.example.emotionawareai.ui.theme.GradMid2
import com.example.emotionawareai.ui.theme.GradStart
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonPurple
import com.example.emotionawareai.ui.theme.NeonRose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    onNavigateToHfLogin: () -> Unit = {}
) {
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()
    val ttsVoiceProfile by viewModel.ttsVoiceProfile.collectAsStateWithLifecycle()
    val ttsBackend by viewModel.ttsBackend.collectAsStateWithLifecycle()
    val piperVoice by viewModel.piperVoice.collectAsStateWithLifecycle()
    val isSelectedPiperVoiceInstalled by viewModel.isSelectedPiperVoiceInstalled.collectAsStateWithLifecycle()
    val isPiperVoiceDownloading by viewModel.isPiperVoiceDownloading.collectAsStateWithLifecycle()
    val piperVoiceDownloadProgress by viewModel.piperVoiceDownloadProgress.collectAsStateWithLifecycle()
    val isCameraEnabled by viewModel.isCameraEnabled.collectAsStateWithLifecycle()
    val isCaptionsEnabled by viewModel.isCaptionsEnabled.collectAsStateWithLifecycle()
    val isContinuousConversationEnabled by viewModel.isContinuousConversationEnabled.collectAsStateWithLifecycle()
    val isProThemeEnabled by viewModel.isProThemeEnabled.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userAvatar by viewModel.userAvatar.collectAsStateWithLifecycle()
    val isPremiumUser by viewModel.isPremiumUser.collectAsStateWithLifecycle()
    val checkInFrequency by viewModel.checkInFrequency.collectAsStateWithLifecycle()
    val growthAreas by viewModel.growthAreas.collectAsStateWithLifecycle()
    val selectedLlmId by viewModel.selectedLlmId.collectAsStateWithLifecycle()
    val isModelAvailable by viewModel.isModelAvailable.collectAsStateWithLifecycle()
    val isModelLoaded by viewModel.isModelLoaded.collectAsStateWithLifecycle()
    val modelInstallState by viewModel.modelInstallState.collectAsStateWithLifecycle()
    val isModelDownloading by viewModel.isModelDownloading.collectAsStateWithLifecycle()
    val modelDownloadProgress by viewModel.modelDownloadProgress.collectAsStateWithLifecycle()
    val isModelDownloadFailed by viewModel.isModelDownloadFailed.collectAsStateWithLifecycle()
    val huggingFaceToken by viewModel.huggingFaceToken.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    val selectedModel = remember(selectedLlmId) {
        LlmOption.fromId(selectedLlmId) ?: LlmOption.CONFIGURED_MODEL
    }
    val llmOptionsWithCompatibility = remember {
        viewModel.getAllLlmOptionsWithCompatibility()
    }

    // File picker launcher: accepts any file type so users can select .gguf files
    val modelFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.installModelFromUri(context, it) }
    }

    // Auto-dismiss SUCCESS/ERROR after a short delay so the user sees confirmation
    LaunchedEffect(modelInstallState) {
        if (modelInstallState == ModelInstallState.SUCCESS ||
            modelInstallState == ModelInstallState.ERROR
        ) {
            kotlinx.coroutines.delay(3_000)
            viewModel.dismissModelInstallState()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile & Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(GradStart, GradMid1, GradMid2, GradEnd)))
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassCard)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(NeonPurple.copy(alpha = 0.3f))
                                .border(2.dp, NeonPurple.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(userAvatar, style = MaterialTheme.typography.titleLarge)
                        }
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text(
                                userName.ifBlank { "User" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                if (isPremiumUser) "✨ Premium" else "Free plan",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPremiumUser) NeonCyan else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                item {
                    SettingsSection("AI Model") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Psychology,
                                contentDescription = null,
                                tint = if (isModelLoaded) NeonCyan else NeonPurple,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                val modelName = selectedModel.name
                                Text(
                                    text = when {
                                        selectedModel.isBuiltIn -> "$modelName selected"
                                        isModelLoaded -> "$modelName loaded"
                                        isModelDownloading -> "Downloading $modelName…"
                                        isModelAvailable -> "$modelName installed (not loaded)"
                                        else -> "$modelName not installed"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isModelLoaded) NeonCyan else Color.White
                                )
                                Text(
                                    text = when {
                                        selectedModel.isBuiltIn ->
                                            "This model is built into compatible devices and does not require a download."
                                        isModelLoaded -> "On-device inference active (${selectedModel.description})"
                                        isModelDownloading -> {
                                            val pct = modelDownloadProgress
                                            if (pct != null && pct >= 0f) {
                                                "%.0f%% — downloading $modelName (${selectedModel.sizeLabel})".format(pct * 100)
                                            } else {
                                                "Downloading $modelName (${selectedModel.sizeLabel})…"
                                            }
                                        }
                                        isModelAvailable -> viewModel.getModelFilePath()
                                        else -> "$modelName will be downloaded automatically (${selectedModel.sizeLabel})"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                if (isModelDownloading) {
                                    Spacer(Modifier.height(6.dp))
                                    val progress = modelDownloadProgress
                                    if (progress != null && progress >= 0f) {
                                        androidx.compose.material3.LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = NeonCyan,
                                            trackColor = NeonPurple.copy(alpha = 0.2f)
                                        )
                                    } else {
                                        androidx.compose.material3.LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = NeonCyan,
                                            trackColor = NeonPurple.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                            if (isModelLoaded) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Loaded",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = GlassBorder)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Choose a model",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            llmOptionsWithCompatibility.forEach { (option, compatibility) ->
                                val tokenRequired = option.requiresHuggingFaceToken
                                val tokenMissing = tokenRequired && huggingFaceToken.isBlank()
                                val isSelected = option.id == selectedModel.id
                                val rowEnabled = !isModelDownloading && !tokenMissing
                                val subtitle = buildString {
                                    append("${option.parameterLabel.ifBlank { option.sizeLabel }} • ${compatibility.label}")
                                    if (compatibility.isRecommended) append(" • Recommended")
                                    if (tokenMissing) append(" • Add HuggingFace token")
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) NeonCyan.copy(alpha = 0.12f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) NeonCyan.copy(alpha = 0.6f) else GlassBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable(enabled = rowEnabled) { viewModel.changeLlmFromSettings(option) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (rowEnabled) Color.White else Color.White.copy(alpha = 0.45f),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (tokenMissing) NeonRose else Color.White.copy(alpha = 0.58f)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = GlassBorder)

                        // Failure banner: shown when download failed and agent is inactive
                        if (isModelDownloadFailed && !isModelLoaded) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = NeonRose,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "AI agent is not active",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = NeonRose
                                    )
                                    Text(
                                        "The model download failed. Check your internet connection and tap Retry.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonRose.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Button(
                                onClick = { viewModel.downloadModel() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonRose.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Psychology,
                                    contentDescription = null,
                                    tint = NeonRose
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("Retry Download", color = NeonRose)
                            }
                            HorizontalDivider(color = GlassBorder)
                        }

                        // Primary action: auto-download the selected model or cancel ongoing download
                        if (!selectedModel.isBuiltIn && (!isModelAvailable || isModelDownloading)) {
                            Button(
                                onClick = {
                                    if (isModelDownloading) viewModel.cancelModelDownload()
                                    else viewModel.downloadModel()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isModelDownloading)
                                        NeonRose.copy(alpha = 0.25f)
                                    else
                                        NeonCyan.copy(alpha = 0.20f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isModelDownloading) {
                                    CircularProgressIndicator(
                                        color = NeonRose,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Text("Cancel Download", color = NeonRose)
                                } else {
                                    Icon(
                                        Icons.Filled.Psychology,
                                        contentDescription = null,
                                        tint = NeonCyan
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Text("Download ${selectedModel.name}", color = NeonCyan)
                                }
                            }
                        }

                        // Secondary: manual file install (advanced users / custom models)
                        Button(
                            onClick = { modelFileLauncher.launch(arrayOf("*/*")) },
                            enabled = modelInstallState != ModelInstallState.INSTALLING && !isModelDownloading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonPurple.copy(alpha = 0.25f),
                                disabledContainerColor = NeonPurple.copy(alpha = 0.10f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            when (modelInstallState) {
                                ModelInstallState.INSTALLING -> {
                                    CircularProgressIndicator(
                                        color = NeonPurple,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Text("Installing…", color = Color.White)
                                }
                                ModelInstallState.SUCCESS -> {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = NeonCyan)
                                    Spacer(Modifier.size(8.dp))
                                    Text("Installed!", color = NeonCyan)
                                }
                                ModelInstallState.ERROR -> {
                                    Icon(Icons.Filled.Info, contentDescription = null, tint = NeonRose)
                                    Spacer(Modifier.size(8.dp))
                                    Text("Installation failed", color = NeonRose)
                                }
                                else -> {
                                    Icon(
                                        Icons.Filled.FolderOpen,
                                        contentDescription = null,
                                        tint = NeonPurple
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Text(
                                         if (isModelAvailable) "Replace model file" else "Load from file (.gguf)",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // ── HuggingFace Token ─────────────────────────────────────────────────
                item {
                    HuggingFaceTokenCard(
                        savedToken = huggingFaceToken,
                        onSave = { token -> viewModel.setHuggingFaceToken(token) },
                        onLoginWithHuggingFace = onNavigateToHfLogin
                    )
                }

                item {
                    SettingsSection("Conversation") {
                        SettingsToggleRow(
                            title = "Voice output (TTS)",
                            subtitle = "Tara speaks responses aloud",
                            icon = if (isTtsEnabled) Icons.AutoMirrored.Filled.VolumeUp
                                   else Icons.AutoMirrored.Filled.VolumeOff,
                            checked = isTtsEnabled,
                            onToggle = { viewModel.toggleTts() }
                        )
                        if (isTtsEnabled) {
                            HorizontalDivider(color = GlassBorder)
                            TtsBackendSelector(
                                selected = ttsBackend,
                                onSelect = { viewModel.setTtsBackend(it) }
                            )
                            HorizontalDivider(color = GlassBorder)
                            if (ttsBackend == TtsBackend.SYSTEM) {
                                TtsVoiceProfileSelector(
                                    selected = ttsVoiceProfile,
                                    onSelect = { viewModel.setTtsVoiceProfile(it) }
                                )
                            } else {
                                PiperVoiceSelector(
                                    selected = piperVoice,
                                    onSelect = { viewModel.setPiperVoice(it) }
                                )
                                HorizontalDivider(color = GlassBorder)
                                PiperVoiceStatusCard(
                                    selectedVoice = piperVoice,
                                    isInstalled = isSelectedPiperVoiceInstalled,
                                    isDownloading = isPiperVoiceDownloading,
                                    progress = piperVoiceDownloadProgress,
                                    onDownload = { viewModel.downloadSelectedPiperVoice() },
                                    onCancel = { viewModel.cancelPiperVoiceDownload() }
                                )
                            }
                        }
                        HorizontalDivider(color = GlassBorder)
                        SettingsToggleRow(
                            title = "Continuous voice mode",
                            subtitle = "Hands-free back-and-forth",
                            icon = if (isContinuousConversationEnabled) Icons.Filled.Mic
                                   else Icons.Filled.MicOff,
                            checked = isContinuousConversationEnabled,
                            onToggle = { viewModel.toggleContinuousConversation() }
                        )
                    }
                }

                item {
                    SettingsSection("Camera & Detection") {
                        SettingsToggleRow(
                            title = "Camera (emotion detection)",
                            subtitle = "Facial & posture signals power Tara's empathy",
                            icon = if (isCameraEnabled) Icons.Filled.Camera
                                   else Icons.Filled.VideocamOff,
                            checked = isCameraEnabled,
                            onToggle = { viewModel.toggleCamera() }
                        )
                        HorizontalDivider(color = GlassBorder)
                        SettingsToggleRow(
                            title = "Activity captions",
                            subtitle = "Show posture & activity overlay",
                            icon = if (isCaptionsEnabled) Icons.Filled.ClosedCaption
                                   else Icons.Filled.ClosedCaptionDisabled,
                            checked = isCaptionsEnabled,
                            onToggle = { viewModel.toggleCaptions() }
                        )
                    }
                }

                item {
                    SettingsSection("Check-in Frequency") {
                        listOf(
                            "daily" to "Daily",
                            "weekly" to "Weekly",
                            "as_needed" to "As needed"
                        ).forEach { (value, label) ->
                            val selected = checkInFrequency == value
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Switch(
                                    checked = selected,
                                    onCheckedChange = {
                                        if (it) viewModel.saveOnboardingPreferences(
                                            growthAreas, value
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeonCyan,
                                        checkedTrackColor = NeonPurple.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            if (value != "as_needed") HorizontalDivider(color = GlassBorder)
                        }
                    }
                }

                item {
                    SettingsSection("Data & Privacy") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint = NeonCyan.copy(alpha = 0.7f),
                                modifier = Modifier.padding(end = 10.dp, top = 2.dp)
                            )
                            Text(
                                "All data is stored locally on your device. Nothing is sent to external servers. Tara is for personal growth — not medical advice. In a crisis, call 988.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        HorizontalDivider(color = GlassBorder)
                        Button(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonRose.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.DeleteForever,
                                contentDescription = null,
                                tint = NeonRose
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("Clear all data", color = NeonRose)
                        }
                    }
                }

                if (!isPremiumUser) {
                    item {
                        Button(
                            onClick = { activity?.let { viewModel.startPremiumUpgrade(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonPurple.copy(alpha = 0.7f)
                            )
                        ) {
                            Text(
                                "✨ Upgrade to Premium",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color(0xFF1E2533),
            title = { Text("Clear all data?", color = Color.White) },
            text = {
                Text(
                    "This will delete all conversations and reset your profile. This cannot be undone.",
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.clearAllData()
                }) { Text("Clear", color = NeonRose) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = NeonCyan.copy(alpha = 0.8f),
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        content()
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = NeonCyan.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(end = 12.dp)
                .size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = NeonPurple.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

/** Horizontal chip-row that lets the user pick a [TtsVoiceProfile]. */
@Composable
private fun TtsVoiceProfileSelector(
    selected: TtsVoiceProfile,
    onSelect: (TtsVoiceProfile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            "Voice style",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TtsVoiceProfile.entries.forEach { profile ->
                val isSelected = profile == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NeonCyan else GlassBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .background(
                            color = if (isSelected) NeonCyan.copy(alpha = 0.15f)
                                    else Color.Transparent,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onSelect(profile) }
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.7f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun TtsBackendSelector(
    selected: TtsBackend,
    onSelect: (TtsBackend) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            "Speech engine",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TtsBackend.entries.forEach { backend ->
                val isSelected = backend == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NeonCyan else GlassBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .background(
                            color = if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onSelect(backend) }
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            backend.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.8f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            backend.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PiperVoiceSelector(
    selected: PiperVoice,
    onSelect: (PiperVoice) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            "Neural voice",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        PiperVoice.entries.forEachIndexed { index, voice ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(voice) }
                    .background(if (voice == selected) NeonCyan.copy(alpha = 0.12f) else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (voice == selected) NeonCyan.copy(alpha = 0.6f) else GlassBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(voice.displayName, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${voice.localeLabel} · ${voice.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                if (voice == selected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (index != PiperVoice.entries.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PiperVoiceStatusCard(
    selectedVoice: PiperVoice,
    isInstalled: Boolean,
    isDownloading: Boolean,
    progress: Float?,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = when {
                isInstalled -> "${selectedVoice.displayName} is ready for fully offline neural speech"
                isDownloading -> "Downloading ${selectedVoice.displayName} voice package…"
                else -> "Download ${selectedVoice.displayName} once to enable Sherpa-ONNX + Piper"
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        if (isDownloading) {
            Spacer(Modifier.height(10.dp))
            if (progress != null && progress >= 0f) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonCyan,
                    trackColor = NeonPurple.copy(alpha = 0.2f)
                )
            } else {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonCyan,
                    trackColor = NeonPurple.copy(alpha = 0.2f)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { if (isDownloading) onCancel() else onDownload() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDownloading) NeonRose.copy(alpha = 0.25f) else NeonPurple.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    color = NeonRose,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.size(8.dp))
                Text("Cancel voice download", color = NeonRose)
            } else {
                Icon(
                    if (isInstalled) Icons.Filled.CheckCircle else Icons.Filled.FolderOpen,
                    contentDescription = null,
                    tint = if (isInstalled) NeonCyan else Color.White
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    if (isInstalled) "Re-download voice package" else "Download voice package",
                    color = if (isInstalled) NeonCyan else Color.White
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Downloads model + shared espeak-ng data into app-private storage. System TTS remains the automatic fallback.",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

// ── HuggingFace Token Card ────────────────────────────────────────────────────

/**
 * A settings card that lets the user enter and save their HuggingFace access
 * token. Required for downloading gated models (e.g. Gemma 2B, Phi-3 Mini).
 *
 * How to get a token:
 *  1. Sign up at huggingface.co
 *  2. Go to huggingface.co/settings/tokens → New token (Read access is enough)
 *  3. Accept the model licence on the model's HuggingFace page
 *  4. Paste the token here and tap Save
 */
@Composable
private fun HuggingFaceTokenCard(
    savedToken: String,
    onSave: (String) -> Unit,
    onLoginWithHuggingFace: () -> Unit = {}
) {
    var draftToken by remember(savedToken) { mutableStateOf(savedToken) }
    var tokenVisible by remember { mutableStateOf(false) }
    val isDirty = draftToken.trim() != savedToken.trim()
    val isSaved = savedToken.isNotBlank() && !isDirty

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassCard)
            .border(
                width = 1.dp,
                color = if (isSaved) NeonCyan.copy(alpha = 0.4f) else GlassBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Key,
                contentDescription = null,
                tint = if (isSaved) NeonCyan else NeonPurple,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "HuggingFace Access Token",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            if (isSaved) {
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Token saved",
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            text = "Required for gated models (Gemma 2B, Phi-3 Mini, Mistral 7B). " +
                   "Get a free Read token at huggingface.co/settings/tokens, " +
                   "then accept the model licence on the model page.",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.55f),
            lineHeight = 16.sp
        )

        OutlinedTextField(
            value = draftToken,
            onValueChange = { draftToken = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "hf_xxxxxxxxxxxxxxxxxxxx",
                    color = Color.White.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            visualTransformation = if (tokenVisible) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            trailingIcon = {
                Icon(
                    imageVector = if (tokenVisible) Icons.Filled.VisibilityOff
                                  else Icons.Filled.Visibility,
                    contentDescription = if (tokenVisible) "Hide token" else "Show token",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { tokenVisible = !tokenVisible }
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = GlassBorder,
                cursorColor = NeonCyan
            ),
            shape = RoundedCornerShape(10.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )

        // ── Login with HuggingFace button ────────────────────────────────────────
        Button(
            onClick = onLoginWithHuggingFace,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonPurple.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Key,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "Login with HuggingFace",
                color = NeonCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (savedToken.isNotBlank()) {
                Button(
                    onClick = {
                        draftToken = ""
                        onSave("")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonRose.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Clear", color = NeonRose, fontSize = 13.sp)
                }
            }
            Button(
                onClick = { onSave(draftToken) },
                enabled = isDirty && draftToken.isNotBlank(),
                modifier = Modifier.weight(2f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan.copy(alpha = 0.25f),
                    disabledContainerColor = NeonCyan.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Save & Download",
                    color = if (isDirty && draftToken.isNotBlank()) NeonCyan
                            else Color.White.copy(alpha = 0.3f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

