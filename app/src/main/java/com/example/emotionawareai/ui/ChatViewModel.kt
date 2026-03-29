package com.example.emotionawareai.ui

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.emotionawareai.domain.model.ActivityCaption
import com.example.emotionawareai.domain.model.ChatMessage
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.MessageRole
import com.example.emotionawareai.domain.model.PiperVoice
import com.example.emotionawareai.billing.BillingManager
import com.example.emotionawareai.billing.PremiumOffer
import com.example.emotionawareai.billing.PremiumPlanType
import com.example.emotionawareai.domain.model.PremiumFeature
import com.example.emotionawareai.engine.ActivityAnalyzer
import com.example.emotionawareai.engine.DeviceCapabilityDetector
import com.example.emotionawareai.engine.EmotionDetector
import com.example.emotionawareai.engine.ModelDownloader
import com.example.emotionawareai.data.database.MoodCheckInDao
import com.example.emotionawareai.data.model.MoodCheckInEntity
import com.example.emotionawareai.domain.model.GrowthArea
import com.example.emotionawareai.domain.model.LlmOption
import com.example.emotionawareai.domain.model.SessionGoal
import com.example.emotionawareai.domain.model.TtsBackend
import com.example.emotionawareai.domain.model.TtsVoiceProfile
import com.example.emotionawareai.domain.model.WeeklyInsight
import com.example.emotionawareai.manager.ConversationManager
import com.example.emotionawareai.manager.InsightsGenerator
import com.example.emotionawareai.manager.MemoryManager
import com.example.emotionawareai.manager.ResponseEngine
import com.example.emotionawareai.tts.PiperVoiceManager
import com.example.emotionawareai.voice.AudioToneAnalyzer
import com.example.emotionawareai.voice.ToneInsight
import com.example.emotionawareai.voice.VoiceError
import com.example.emotionawareai.voice.VoiceProcessor
import com.example.emotionawareai.voice.isBenignForContinuousMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Represents the lifecycle of an in-app model file installation. */
enum class ModelInstallState { IDLE, INSTALLING, SUCCESS, ERROR }

data class SpeechCaption(
    val turnId: Long,
    val speaker: MessageRole,
    val text: String
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationManager: ConversationManager,
    private val responseEngine: ResponseEngine,
    private val emotionDetector: EmotionDetector,
    private val activityAnalyzer: ActivityAnalyzer,
    private val voiceProcessor: VoiceProcessor,
    private val memoryManager: MemoryManager,
    private val audioToneAnalyzer: AudioToneAnalyzer,
    private val billingManager: BillingManager,
    private val moodCheckInDao: MoodCheckInDao,
    private val insightsGenerator: InsightsGenerator,
    private val modelDownloader: ModelDownloader,
    private val piperVoiceManager: PiperVoiceManager,
    val deviceCapabilityDetector: DeviceCapabilityDetector
) : ViewModel() {

    // ── UI State ─────────────────────────────────────────────────────────────

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _currentEmotion = MutableStateFlow(Emotion.UNKNOWN)
    val currentEmotion: StateFlow<Emotion> = _currentEmotion.asStateFlow()

    private val _audioToneEmotion = MutableStateFlow(Emotion.UNKNOWN)
    val audioToneEmotion: StateFlow<Emotion> = _audioToneEmotion.asStateFlow()

    private val _effectiveEmotion = MutableStateFlow(Emotion.NEUTRAL)
    val effectiveEmotion: StateFlow<Emotion> = _effectiveEmotion.asStateFlow()

    private val _toneInsight = MutableStateFlow<ToneInsight?>(null)
    val toneInsight: StateFlow<ToneInsight?> = _toneInsight.asStateFlow()

    private val _activityCaptions = MutableStateFlow<List<ActivityCaption>>(emptyList())
    val activityCaptions: StateFlow<List<ActivityCaption>> = _activityCaptions.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speechCaption = MutableStateFlow<SpeechCaption?>(null)
    val speechCaption: StateFlow<SpeechCaption?> = _speechCaption.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _isModelAvailable = MutableStateFlow(false)
    val isModelAvailable: StateFlow<Boolean> = _isModelAvailable.asStateFlow()

    private val _modelInstallState = MutableStateFlow(ModelInstallState.IDLE)
    val modelInstallState: StateFlow<ModelInstallState> = _modelInstallState.asStateFlow()

    /**
     * Forwarded directly from [ModelDownloader] — `true` while the BitNet
     * model file is being downloaded in the background.
     */
    val isModelDownloading: StateFlow<Boolean> = modelDownloader.isDownloading

    /**
     * Forwarded directly from [ModelDownloader] — download progress in
     * `[0, 1]`, `-1f` when content-length is unknown, or `null` when idle.
     */
    val modelDownloadProgress: StateFlow<Float?> = modelDownloader.downloadProgress

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _cameraPermissionGranted = MutableStateFlow(false)
    val cameraPermissionGranted: StateFlow<Boolean> = _cameraPermissionGranted.asStateFlow()

    private val _audioPermissionGranted = MutableStateFlow(false)
    val audioPermissionGranted: StateFlow<Boolean> = _audioPermissionGranted.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    private val _ttsVoiceProfile = MutableStateFlow(TtsVoiceProfile.DEFAULT)
    val ttsVoiceProfile: StateFlow<TtsVoiceProfile> = _ttsVoiceProfile.asStateFlow()

    private val _ttsBackend = MutableStateFlow(TtsBackend.SYSTEM)
    val ttsBackend: StateFlow<TtsBackend> = _ttsBackend.asStateFlow()

    private val _piperVoice = MutableStateFlow(PiperVoice.ALAN)
    val piperVoice: StateFlow<PiperVoice> = _piperVoice.asStateFlow()

    private val _isSelectedPiperVoiceInstalled = MutableStateFlow(false)
    val isSelectedPiperVoiceInstalled: StateFlow<Boolean> =
        _isSelectedPiperVoiceInstalled.asStateFlow()

    val isPiperVoiceDownloading: StateFlow<Boolean> = piperVoiceManager.isDownloading
    val piperVoiceDownloadProgress: StateFlow<Float?> = piperVoiceManager.downloadProgress

    private val _isContinuousConversationEnabled = MutableStateFlow(true)
    val isContinuousConversationEnabled: StateFlow<Boolean> =
        _isContinuousConversationEnabled.asStateFlow()

    private val _isCameraEnabled = MutableStateFlow(true)
    val isCameraEnabled: StateFlow<Boolean> = _isCameraEnabled.asStateFlow()

    private val _isCameraPreviewVisible = MutableStateFlow(true)
    val isCameraPreviewVisible: StateFlow<Boolean> = _isCameraPreviewVisible.asStateFlow()

    private val _isCaptionsEnabled = MutableStateFlow(true)
    val isCaptionsEnabled: StateFlow<Boolean> = _isCaptionsEnabled.asStateFlow()

    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady: StateFlow<Boolean> = _isBillingReady.asStateFlow()

    private val _isAiAgentActive = MutableStateFlow(false)
    val isAiAgentActive: StateFlow<Boolean> = _isAiAgentActive.asStateFlow()

    private val _premiumOffers = MutableStateFlow<List<PremiumOffer>>(emptyList())
    val premiumOffers: StateFlow<List<PremiumOffer>> = _premiumOffers.asStateFlow()

    private val _isPurchaseInProgress = MutableStateFlow(false)
    val isPurchaseInProgress: StateFlow<Boolean> = _isPurchaseInProgress.asStateFlow()

    private val _isRestoreInProgress = MutableStateFlow(false)
    val isRestoreInProgress: StateFlow<Boolean> = _isRestoreInProgress.asStateFlow()

    private val _isProThemeEnabled = MutableStateFlow(false)
    val isProThemeEnabled: StateFlow<Boolean> = _isProThemeEnabled.asStateFlow()

    private val _isExportWithInsights = MutableStateFlow(true)
    val isExportWithInsights: StateFlow<Boolean> = _isExportWithInsights.asStateFlow()

    private val _exportPayload = MutableStateFlow<String?>(null)
    val exportPayload: StateFlow<String?> = _exportPayload.asStateFlow()

    private val _premiumFeatureMatrix = MutableStateFlow(buildPremiumMatrix(true))
    val premiumFeatureMatrix: StateFlow<Map<PremiumFeature, Boolean>> =
        _premiumFeatureMatrix.asStateFlow()

    /**
     * Remote kill-switch for all premium features.
     * Defaults to [true] so every user gets free access to all features.
     * Can be set to [false] via [setPremiumFeaturesEnabled] to re-gate features remotely.
     */
    private val _premiumFeaturesGloballyEnabled = MutableStateFlow(true)
    val premiumFeaturesGloballyEnabled: StateFlow<Boolean> =
        _premiumFeaturesGloballyEnabled.asStateFlow()

    // ── User profile ──────────────────────────────────────────────────────────

    /**
     * Null = not yet checked. False = no profile saved. True = profile exists.
     */
    private val _hasUserProfile = MutableStateFlow<Boolean?>(null)
    val hasUserProfile: StateFlow<Boolean?> = _hasUserProfile.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userAvatar = MutableStateFlow("😊")
    val userAvatar: StateFlow<String> = _userAvatar.asStateFlow()

    // ── LLM setup state ───────────────────────────────────────────────────────

    /**
     * Null = not yet checked. False = LLM not chosen. True = LLM setup done.
     */
    private val _isLlmSetupComplete = MutableStateFlow<Boolean?>(null)
    val isLlmSetupComplete: StateFlow<Boolean?> = _isLlmSetupComplete.asStateFlow()

    private val _selectedLlmId = MutableStateFlow("")
    val selectedLlmId: StateFlow<String> = _selectedLlmId.asStateFlow()

    private var generationJob: Job? = null
    private var messageIdCounter = 0L
    private var speechCaptionTurnId = 0L

    // ── Growth / insights / check-in state ───────────────────────────────────

    private val _activeGoals = MutableStateFlow<List<SessionGoal>>(emptyList())
    val activeGoals: StateFlow<List<SessionGoal>> = _activeGoals.asStateFlow()

    private val _weeklyInsight = MutableStateFlow<WeeklyInsight?>(null)
    val weeklyInsight: StateFlow<WeeklyInsight?> = _weeklyInsight.asStateFlow()

    private val _insights = MutableStateFlow<List<WeeklyInsight>>(emptyList())
    val insights: StateFlow<List<WeeklyInsight>> = _insights.asStateFlow()

    private val _showDailyCheckIn = MutableStateFlow(false)
    val showDailyCheckIn: StateFlow<Boolean> = _showDailyCheckIn.asStateFlow()

    private val _showPrivacyNotice = MutableStateFlow(false)
    val showPrivacyNotice: StateFlow<Boolean> = _showPrivacyNotice.asStateFlow()

    private val _growthAreas = MutableStateFlow<List<GrowthArea>>(emptyList())
    val growthAreas: StateFlow<List<GrowthArea>> = _growthAreas.asStateFlow()

    private val _checkInFrequency = MutableStateFlow("daily")
    val checkInFrequency: StateFlow<String> = _checkInFrequency.asStateFlow()

    private val _isVoiceModeActive = MutableStateFlow(false)
    val isVoiceModeActive: StateFlow<Boolean> = _isVoiceModeActive.asStateFlow()

    // ── Initialisation ────────────────────────────────────────────────────────

    init {
        responseEngine.setTtsFallbackListener { fallbackMessage ->
            // Surface neural-backend fallback in the existing error channel so the settings
            // and chat UI can immediately explain why the system voice is speaking instead.
            _errorMessage.update { fallbackMessage }
        }
        viewModelScope.launch {
            initializeSession()
            observeEmotionDetection()
            observeActivityCaptions()
            observeVoiceRecognition()
            observeAudioToneSignals()
            observeBillingState()
            observeSpeakingState()
            observePiperVoiceDownloads()
        }
    }

    private suspend fun initializeSession() {
        conversationManager.ensureConversation()
        val convId = conversationManager.getActiveConversationId()

        val history = memoryManager.getRecentContext(convId, limit = 20)
        _messages.update { history }

        _isTtsEnabled.update { memoryManager.isTtsEnabled() }
        responseEngine.setTtsEnabled(_isTtsEnabled.value)

        val profile = memoryManager.getTtsVoiceProfile()
        _ttsVoiceProfile.update { profile }
        responseEngine.setVoiceProfile(profile)

        val backend = memoryManager.getTtsBackend()
        _ttsBackend.update { backend }
        responseEngine.setTtsBackend(backend)

        val piperVoice = memoryManager.getPiperVoice()
        _piperVoice.update { piperVoice }
        responseEngine.setPiperVoice(piperVoice)
        val isPiperVoiceReady = refreshPiperVoiceInstallState()
        if (backend == TtsBackend.SHERPA_PIPER && !isPiperVoiceReady) {
            piperVoiceManager.startDownloadIfAbsent(piperVoice)
        }

        // Load the persisted continuous conversation preference; default is true (live mode).
        val continuousEnabled = memoryManager.isContinuousConversationEnabled()
        _isContinuousConversationEnabled.update { continuousEnabled }

        // Load camera and captions preferences
        _isCameraEnabled.update { memoryManager.isCameraEnabled() }
        _isCameraPreviewVisible.update { memoryManager.isCameraPreviewVisible() }
        _isCaptionsEnabled.update { memoryManager.isCaptionsEnabled() }

        _isPremiumUser.update { memoryManager.isPremiumUnlocked() }
        _isProThemeEnabled.update { memoryManager.isProThemeEnabled() }
        _isExportWithInsights.update { memoryManager.isExportWithInsightsEnabled() }
        val globallyEnabled = memoryManager.isPremiumFeaturesGloballyEnabled()
        _premiumFeaturesGloballyEnabled.update { globallyEnabled }
        // Grant all features for free; respect the remote kill-switch if off.
        _premiumFeatureMatrix.update { buildPremiumMatrix(globallyEnabled) }

        // Load user profile
        val name = memoryManager.getUserName()
        _userName.update { name }
        _userAvatar.update { memoryManager.getUserAvatar() }
        _hasUserProfile.update { name.isNotBlank() }

        // Load LLM setup state
        val llmSetup = memoryManager.isLlmSetupComplete()
        _isLlmSetupComplete.update { llmSetup }
        _selectedLlmId.update { memoryManager.getSelectedLlmId() }

        _growthAreas.update { memoryManager.getGrowthAreas() }
        _checkInFrequency.update { memoryManager.getCheckInFrequency() }

        // Load active goals
        viewModelScope.launch {
            memoryManager.observeActiveGoals().collect { goals ->
                _activeGoals.update { goals }
            }
        }

        // Load insights
        viewModelScope.launch {
            insightsGenerator.observeInsights().collect { list ->
                _insights.update { list }
                _weeklyInsight.update { list.firstOrNull() }
            }
        }

        // Show privacy notice on first launch
        if (!memoryManager.isPrivacyNoticeShown()) {
            _showPrivacyNotice.update { true }
        }

        // Check if daily check-in should be shown
        checkDailyCheckInNeeded()

        viewModelScope.launch(Dispatchers.IO) {
            // Load the model immediately if it is already on-disk (e.g. app was
            // previously used, or Application.onCreate already finished the download
            // before this ViewModel was created).
            val available = responseEngine.isModelFileAvailable()
            _isModelAvailable.update { available }
            if (available) {
                val loaded = responseEngine.loadModel()
                _isModelLoaded.update { loaded }
                updateAiActiveState()
                Log.i(TAG, "Model available: $available, loaded: $loaded")
            }
        }

        // Observe the ModelDownloader for download completion. The download was
        // started by Application.onCreate() before this ViewModel existed, so by
        // the time we reach here it may already be running or even complete.
        viewModelScope.launch(Dispatchers.IO) {
            modelDownloader.isDownloading.collect { downloading ->
                if (!downloading) {
                    val available = responseEngine.isModelFileAvailable()
                    _isModelAvailable.update { available }
                    if (available && !_isModelLoaded.value) {
                        val loaded = responseEngine.loadModel()
                        _isModelLoaded.update { loaded }
                        updateAiActiveState()
                        Log.i(TAG, "BitNet download finished — model loaded=$loaded")
                    }
                    if (!available && modelDownloader.downloadFailed.value) {
                        _errorMessage.update {
                            "BitNet model download failed. Check your internet connection and try again."
                        }
                    }
                }
            }
        }
    }

    private fun observeEmotionDetection() {
        viewModelScope.launch {
            emotionDetector.emotionFlow.collect { emotion ->
                _currentEmotion.update { emotion }
                updateEffectiveEmotion()
            }
        }
    }

    private fun observeActivityCaptions() {
        viewModelScope.launch {
            activityAnalyzer.captionFlow.collect { captions ->
                _activityCaptions.update { captions }
            }
        }
    }

    private fun observeVoiceRecognition() {
        viewModelScope.launch {
            voiceProcessor.recognizedTextFlow.collect { text ->
                if (text.isNotBlank()) {
                    // Stop any ongoing TTS so the AI doesn't hear its own response.
                    responseEngine.stopSpeaking()
                    sendMessage(text, fromVoiceInput = true)
                }
            }
        }

        viewModelScope.launch {
            voiceProcessor.listeningStateFlow.collect { listening ->
                _isListening.update { listening }
                updateAiActiveState()
            }
        }

        viewModelScope.launch {
            voiceProcessor.errorFlow.collect { error ->
                handleVoiceError(error)
            }
        }

        viewModelScope.launch {
            voiceProcessor.rmsFlow.collect { rms ->
                audioToneAnalyzer.onRmsSample(rms)
            }
        }
    }

    private fun observeAudioToneSignals() {
        viewModelScope.launch {
            audioToneAnalyzer.audioEmotionFlow.collect { emotion ->
                _audioToneEmotion.update { emotion }
                updateEffectiveEmotion()
            }
        }

        viewModelScope.launch {
            audioToneAnalyzer.toneInsightFlow.collect { insight ->
                _toneInsight.update { insight }
            }
        }
    }

    private fun observeSpeakingState() {
        viewModelScope.launch {
            responseEngine.isSpeaking.collect { speaking ->
                _isSpeaking.update { speaking }
                updateAiActiveState()
            }
        }
    }

    private fun observeBillingState() {
        viewModelScope.launch {
            billingManager.isBillingReady.collect { ready ->
                _isBillingReady.update { ready }
            }
        }

        viewModelScope.launch {
            billingManager.isPremium.collect { premium ->
                _isPremiumUser.update { premium }
                // Feature matrix is driven by the global kill-switch, not billing state.
                // All features remain free; billing state is tracked for future use.
                _premiumFeatureMatrix.update { buildPremiumMatrix(_premiumFeaturesGloballyEnabled.value) }
                memoryManager.setPremiumUnlocked(premium)
            }
        }

        viewModelScope.launch {
            billingManager.offers.collect { offers ->
                _premiumOffers.update { offers }
            }
        }

        viewModelScope.launch {
            billingManager.isPurchaseInProgress.collect { inProgress ->
                _isPurchaseInProgress.update { inProgress }
            }
        }

        viewModelScope.launch {
            billingManager.isRestoreInProgress.collect { inProgress ->
                _isRestoreInProgress.update { inProgress }
            }
        }

        viewModelScope.launch {
            billingManager.billingMessage.collect { message ->
                if (message != null) {
                    _errorMessage.update { message }
                    billingManager.clearMessage()
                }
            }
        }
    }

    // ── Public Actions ────────────────────────────────────────────────────────

    fun sendMessage(text: String, fromVoiceInput: Boolean = false) {
        if (text.isBlank() || _isGenerating.value) return

        Log.i(TAG, "sendMessage: fromVoice=$fromVoiceInput, emotion=${_effectiveEmotion.value}, length=${text.trim().length}")

        updateSpeechCaption(
            speaker = MessageRole.USER,
            text = text.trim(),
            newTurn = true
        )

        val userMessage = ChatMessage(
            id = ++messageIdCounter,
            content = text.trim(),
            role = MessageRole.USER,
            emotion = _currentEmotion.value
        )

        _messages.update { it + userMessage }

        generationJob = viewModelScope.launch {
            _isGenerating.update { true }

            // Build context BEFORE saving the user message so that the current
            // user turn does not appear in the retrieved history (which would
            // cause it to be echoed in both [CONTEXT] and [USER] prompt sections).
            val context = conversationManager.buildContext(
                userMessage = text.trim(),
                emotion = _effectiveEmotion.value,
                audioToneEmotion = _audioToneEmotion.value,
                historyLimit = if (_isPremiumUser.value) 20 else 6
            )

            // Persist the user message only after history has been fetched.
            conversationManager.saveMessage(userMessage)

            val streamingMessage = ChatMessage(
                id = ++messageIdCounter,
                content = "",
                role = MessageRole.ASSISTANT,
                isStreaming = true
            )
            _messages.update { it + streamingMessage }

            val fullResponse = StringBuilder()
            var isFirstAssistantCaption = true

            responseEngine.generateResponse(context)
                .catch { e ->
                    Log.e(TAG, "Generation error: ${e.message}", e)
                    _errorMessage.update { "Failed to generate response: ${e.message}" }
                }
                .collect { token ->
                    fullResponse.append(token)
                    updateSpeechCaption(
                        speaker = MessageRole.ASSISTANT,
                        text = fullResponse.toString(),
                        newTurn = isFirstAssistantCaption
                    )
                    isFirstAssistantCaption = false
                    _messages.update { list ->
                        list.map { msg ->
                            if (msg.id == streamingMessage.id) {
                                msg.copy(content = fullResponse.toString())
                            } else msg
                        }
                    }
                }

            val finalMessage = streamingMessage.copy(
                content = fullResponse.toString().ifBlank { "I'm sorry, I couldn't generate a response." },
                isStreaming = false
            )

            _messages.update { list ->
                list.map { msg ->
                    if (msg.id == streamingMessage.id) finalMessage else msg
                }
            }

            if (finalMessage.content.isNotBlank() && isFirstAssistantCaption) {
                updateSpeechCaption(
                    speaker = MessageRole.ASSISTANT,
                    text = finalMessage.content,
                    newTurn = true
                )
            }

            Log.i(TAG, "Generation complete: responseLength=${finalMessage.content.length}")
            conversationManager.saveMessage(finalMessage)
            _isGenerating.update { false }

            maybeResumeContinuousConversation(fromVoiceInput)
        }
    }

    fun startVoiceInput() {
        if (!_audioPermissionGranted.value) {
            _errorMessage.update { "Microphone permission is required for voice input" }
            return
        }
        Log.i(TAG, "startVoiceInput (continuous=${_isContinuousConversationEnabled.value})")
        if (_isContinuousConversationEnabled.value) {
            voiceProcessor.startContinuousListening()
        } else {
            voiceProcessor.startListening()
        }
    }

    fun stopVoiceInput() {
        voiceProcessor.stopContinuousListening()
    }

    fun toggleTts() {
        val newState = !_isTtsEnabled.value
        _isTtsEnabled.update { newState }
        responseEngine.setTtsEnabled(newState)
        viewModelScope.launch {
            memoryManager.setTtsEnabled(newState)
        }
    }

    fun setTtsVoiceProfile(profile: TtsVoiceProfile) {
        _ttsVoiceProfile.update { profile }
        responseEngine.setVoiceProfile(profile)
        viewModelScope.launch {
            memoryManager.setTtsVoiceProfile(profile)
        }
    }

    fun setTtsBackend(backend: TtsBackend) {
        _ttsBackend.update { backend }
        responseEngine.setTtsBackend(backend)
        if (backend == TtsBackend.SHERPA_PIPER) {
            piperVoiceManager.startDownloadIfAbsent(_piperVoice.value)
        }
        refreshPiperVoiceInstallStateAsync()
        viewModelScope.launch {
            memoryManager.setTtsBackend(backend)
        }
    }

    fun setPiperVoice(voice: PiperVoice) {
        _piperVoice.update { voice }
        responseEngine.setPiperVoice(voice)
        refreshPiperVoiceInstallStateAsync()
        if (_ttsBackend.value == TtsBackend.SHERPA_PIPER) {
            piperVoiceManager.startDownloadIfAbsent(voice)
        }
        viewModelScope.launch {
            memoryManager.setPiperVoice(voice)
        }
    }

    fun downloadSelectedPiperVoice() {
        piperVoiceManager.startDownload(_piperVoice.value)
    }

    fun cancelPiperVoiceDownload() {
        piperVoiceManager.cancelDownload()
    }

    fun toggleCamera() {
        val enabled = !_isCameraEnabled.value
        _isCameraEnabled.update { enabled }
        viewModelScope.launch {
            memoryManager.setCameraEnabled(enabled)
        }
        if (!enabled) {
            // Camera turned off — stop analyzers and clear emotion/caption state
            emotionDetector.release()
            activityAnalyzer.release()
            _currentEmotion.update { Emotion.NEUTRAL }
            _activityCaptions.update { emptyList() }
            updateAiActiveState()
        } else if (_cameraPermissionGranted.value) {
            emotionDetector.initialize()
            activityAnalyzer.initialize()
            updateAiActiveState()
        }
    }

    fun toggleCameraPreviewVisible() {
        val visible = !_isCameraPreviewVisible.value
        _isCameraPreviewVisible.update { visible }
        viewModelScope.launch {
            memoryManager.setCameraPreviewVisible(visible)
        }
    }

    fun toggleCaptions() {
        val enabled = !_isCaptionsEnabled.value
        _isCaptionsEnabled.update { enabled }
        viewModelScope.launch {
            memoryManager.setCaptionsEnabled(enabled)
        }
    }

    fun toggleContinuousConversation() {
        if (!hasPremiumFeature(PremiumFeature.CONTINUOUS_CONVERSATION)) {
            _errorMessage.update { "Live conversation features are currently disabled." }
            return
        }

        val enabled = !_isContinuousConversationEnabled.value
        _isContinuousConversationEnabled.update { enabled }

        viewModelScope.launch {
            memoryManager.setContinuousConversationEnabled(enabled)
        }

        if (enabled && _audioPermissionGranted.value && !_isListening.value && !_isGenerating.value) {
            voiceProcessor.startContinuousListening()
        } else if (!enabled) {
            voiceProcessor.stopContinuousListening()
        }
        updateAiActiveState()
    }

    fun startPremiumUpgrade(activity: Activity, planType: PremiumPlanType = PremiumPlanType.MONTHLY) {
        billingManager.launchUpgradeFlow(activity, planType)
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }

    fun retryBillingConnection() {
        billingManager.retryConnection()
    }

    fun toggleProTheme() {
        if (!hasPremiumFeature(PremiumFeature.PRO_THEMES)) {
            _errorMessage.update { "Pro themes are currently disabled." }
            return
        }
        val enabled = !_isProThemeEnabled.value
        _isProThemeEnabled.update { enabled }
        viewModelScope.launch {
            memoryManager.setProThemeEnabled(enabled)
        }
    }

    fun toggleExportInsights() {
        val enabled = !_isExportWithInsights.value
        _isExportWithInsights.update { enabled }
        viewModelScope.launch {
            memoryManager.setExportWithInsightsEnabled(enabled)
        }
    }

    fun prepareExportPayload() {
        if (!hasPremiumFeature(PremiumFeature.EXPORT_CHAT)) {
            _errorMessage.update { "Export is currently disabled." }
            return
        }

        val tone = _toneInsight.value
        val header = buildString {
            appendLine("MoodMitra AI Conversation Export")
            appendLine("User: ${_userName.value}")
            appendLine("Face emotion: ${_currentEmotion.value.displayName}")
            if (_isExportWithInsights.value && tone != null) {
                appendLine("Tone: ${tone.label} (confidence ${(tone.confidence * 100).toInt()}%)")
            }
            appendLine()
        }

        val transcript = messages.value.joinToString(separator = "\n") { msg ->
            val role = if (msg.isFromUser) _userName.value.ifBlank { "You" } else "MoodMitra"
            "[$role] ${msg.content}"
        }

        _exportPayload.update { header + transcript }
    }

    fun clearExportPayload() {
        _exportPayload.update { null }
    }

    /**
     * Saves the user's profile (name + avatar) and marks the profile as set.
     * Called from the login/onboarding screen.
     */
    fun saveUserProfile(name: String, avatar: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        _userName.update { trimmedName }
        _userAvatar.update { avatar }
        _hasUserProfile.update { true }
        viewModelScope.launch {
            memoryManager.setUserName(trimmedName)
            memoryManager.setUserAvatar(avatar)
            // Persist continuous conversation enabled by default for new users
            memoryManager.setContinuousConversationEnabled(true)
        }
    }

    fun dismissPrivacyNotice() {
        _showPrivacyNotice.update { false }
        viewModelScope.launch { memoryManager.setPrivacyNoticeShown() }
    }

    /**
     * Saves the user's LLM selection and marks setup as complete.
     * If a built-in LLM is selected the existing model download is not needed.
     */
    fun saveLlmSelection(option: LlmOption) {
        _selectedLlmId.update { option.id }
        _isLlmSetupComplete.update { true }
        viewModelScope.launch {
            memoryManager.setSelectedLlmId(option.id)
            memoryManager.setLlmSetupComplete()
        }
    }

    fun submitMoodCheckIn(moodScore: Int, note: String) {
        _showDailyCheckIn.update { false }
        viewModelScope.launch {
            moodCheckInDao.insert(
                MoodCheckInEntity(
                    moodScore = moodScore,
                    note = note,
                    emotion = _effectiveEmotion.value.name
                )
            )
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())
            memoryManager.setLastCheckInDate(today)
        }
    }

    fun dismissDailyCheckIn() {
        _showDailyCheckIn.update { false }
    }

    fun addGoal(title: String, area: GrowthArea) {
        if (title.isBlank()) return
        viewModelScope.launch {
            memoryManager.addGoal(title, area)
        }
    }

    fun archiveGoal(id: Long) {
        viewModelScope.launch { memoryManager.archiveGoal(id) }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch { memoryManager.deleteGoal(id) }
    }

    fun updateGoalProgress(id: Long, note: String) {
        viewModelScope.launch { memoryManager.updateGoalProgress(id, note) }
    }

    fun saveOnboardingPreferences(
        areas: List<GrowthArea>,
        frequency: String
    ) {
        _growthAreas.update { areas }
        _checkInFrequency.update { frequency }
        viewModelScope.launch {
            memoryManager.setGrowthAreas(areas)
            memoryManager.setCheckInFrequency(frequency)
            memoryManager.setOnboardingComplete()
        }
    }

    fun generateWeeklyInsight() {
        viewModelScope.launch {
            val convId = conversationManager.getActiveConversationId()
            if (convId != -1L) {
                insightsGenerator.generateCurrentWeekInsight(convId)
            }
        }
    }

    fun toggleVoiceMode() {
        val newState = !_isVoiceModeActive.value
        _isVoiceModeActive.update { newState }
        if (newState && _audioPermissionGranted.value) {
            if (_isContinuousConversationEnabled.value) {
                voiceProcessor.startContinuousListening()
            }
        } else if (!newState) {
            voiceProcessor.stopContinuousListening()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            startNewConversation()
            _messages.update { emptyList() }
            memoryManager.setUserName("")
            memoryManager.setUserAvatar("😊")
            _hasUserProfile.update { false }
            _userName.update { "" }
        }
    }

    /**
     * Updates the user's display name.
     */
    fun updateUserName(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        _userName.update { trimmedName }
        viewModelScope.launch { memoryManager.setUserName(trimmedName) }
    }

    /**
     * Updates the user's avatar emoji.
     */
    fun updateUserAvatar(avatar: String) {
        _userAvatar.update { avatar }
        viewModelScope.launch { memoryManager.setUserAvatar(avatar) }
    }

    fun onPermissionsResult(cameraGranted: Boolean, audioGranted: Boolean) {
        Log.i(TAG, "onPermissionsResult: camera=$cameraGranted, audio=$audioGranted")
        _cameraPermissionGranted.update { cameraGranted }
        _audioPermissionGranted.update { audioGranted }
        updateAiActiveState()

        if (cameraGranted && _isCameraEnabled.value) {
            emotionDetector.initialize()
            activityAnalyzer.initialize()
        }

        // Auto-start continuous voice if permission granted and mode is enabled
        if (audioGranted && _isContinuousConversationEnabled.value && !_isListening.value) {
            voiceProcessor.startContinuousListening()
        }
    }

    /**
     * Called by the camera analysis callback with each decoded frame bitmap.
     * Routes the shared bitmap to both [EmotionDetector] and [ActivityAnalyzer].
     * Each analyzer applies its own rate-limiting internally.
     */
    fun onCameraFrame(bitmap: Bitmap) {
        emotionDetector.processBitmapFrame(bitmap)
        activityAnalyzer.processBitmapFrame(bitmap)
    }

    fun clearError() {
        _errorMessage.update { null }
    }

    /** Returns the expected on-device path for the LLM model file. */
    fun getModelFilePath(): String = responseEngine.modelFilePath()

    /**
     * Installs a model file from [uri] (obtained via the system file picker),
     * copies it to the app-private models directory, and reloads the inference
     * engine. Emits progress via [modelInstallState].
     *
     * Only files whose name ends with `.gguf` are accepted; other files produce
     * an [ModelInstallState.ERROR] state without writing anything to disk.
     */
    fun installModelFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _modelInstallState.update { ModelInstallState.INSTALLING }
            try {
                // Validate file extension before reading potentially large data.
                val fileName = uri.lastPathSegment ?: ""
                if (!fileName.endsWith(".gguf", ignoreCase = true)) {
                    Log.e(TAG, "Rejected non-.gguf file: $fileName (URI: $uri)")
                    _modelInstallState.update { ModelInstallState.ERROR }
                    return@launch
                }
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e(TAG, "Could not open InputStream for URI: $uri")
                    _modelInstallState.update { ModelInstallState.ERROR }
                    return@launch
                }
                val success = inputStream.use { responseEngine.installAndLoadModel(it) }
                _isModelAvailable.update { responseEngine.isModelFileAvailable() }
                _isModelLoaded.update { responseEngine.isModelLoaded }
                updateAiActiveState()
                _modelInstallState.update {
                    if (success) ModelInstallState.SUCCESS else ModelInstallState.ERROR
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error installing model from URI", e)
                _modelInstallState.update { ModelInstallState.ERROR }
            }
        }
    }

    /** Resets [modelInstallState] back to [ModelInstallState.IDLE]. */
    fun dismissModelInstallState() {
        _modelInstallState.update { ModelInstallState.IDLE }
    }

    private suspend fun refreshPiperVoiceInstallState(): Boolean {
        val selectedVoice = _piperVoice.value
        val ready = responseEngine.isPiperVoiceReady(selectedVoice)
        if (_piperVoice.value == selectedVoice) {
            _isSelectedPiperVoiceInstalled.update { ready }
        }
        return ready
    }

    private fun refreshPiperVoiceInstallStateAsync() {
        viewModelScope.launch {
            refreshPiperVoiceInstallState()
        }
    }

    private fun observePiperVoiceDownloads() {
        viewModelScope.launch {
            piperVoiceManager.isDownloading.collect { downloading ->
                if (!downloading) {
                    val selectedVoice = _piperVoice.value
                    val isReady = refreshPiperVoiceInstallState()
                    if (_ttsBackend.value == TtsBackend.SHERPA_PIPER && !isReady) {
                        if (piperVoiceManager.downloadFailed.value) {
                            _errorMessage.update { "Failed to download Piper voice package" }
                        } else if (piperVoiceManager.isVoiceInstalled(selectedVoice)) {
                            _errorMessage.update {
                                "${selectedVoice.displayName} downloaded but could not be initialized. Falling back to Android system TTS."
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Manually triggers a BitNet model download (e.g. retry after failure or
     * to replace an existing file). Delegates to [ModelDownloader.startDownload].
     *
     * If a download is already in progress this is a no-op.
     */
    fun downloadModel() {
        modelDownloader.startDownload()
    }

    /** Cancels an in-progress BitNet model download. */
    fun cancelModelDownload() {
        modelDownloader.cancelDownload()
    }

    fun cancelGeneration() {
        Log.i(TAG, "cancelGeneration")
        generationJob?.cancel()
        _isGenerating.update { false }
        _messages.update { list ->
            list.map { msg ->
                if (msg.isStreaming) {
                    msg.copy(
                        content = msg.content.ifBlank { "(Cancelled)" },
                        isStreaming = false
                    )
                } else msg
            }
        }
    }

    fun startNewConversation() {
        Log.i(TAG, "startNewConversation")
        viewModelScope.launch {
            conversationManager.startNewConversation()
            _messages.update { emptyList() }
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private fun handleVoiceError(error: VoiceError) {
        // Benign errors in continuous mode are handled by VoiceProcessor's auto-restart.
        if (_isContinuousConversationEnabled.value && error.isBenignForContinuousMode) return
        val message = when (error) {
            VoiceError.NO_MATCH, VoiceError.SPEECH_TIMEOUT ->
                "Couldn't hear you — please try again"
            VoiceError.INSUFFICIENT_PERMISSIONS ->
                "Microphone permission denied"
            VoiceError.NOT_AVAILABLE ->
                "Voice recognition not available on this device"
            else -> "Voice input error: ${error.message}"
        }
        _errorMessage.update { message }
    }

    private fun updateSpeechCaption(
        speaker: MessageRole,
        text: String,
        newTurn: Boolean
    ) {
        if (text.isBlank()) return
        val turnId = if (newTurn || _speechCaption.value == null) {
            ++speechCaptionTurnId
        } else {
            _speechCaption.value?.turnId ?: ++speechCaptionTurnId
        }
        _speechCaption.update {
            SpeechCaption(
                turnId = turnId,
                speaker = speaker,
                text = text
            )
        }
    }

    private suspend fun maybeResumeContinuousConversation(fromVoiceInput: Boolean) {
        if (!fromVoiceInput) return
        if (!_isContinuousConversationEnabled.value) return
        if (!_audioPermissionGranted.value) return

        // Wait for TTS to finish speaking before restarting the microphone.
        // Without this wait the recogniser would capture the AI's own TTS output
        // and feed it back as a new user message, creating an infinite loop.
        val ttsWaitStart = System.currentTimeMillis()
        while (responseEngine.isTtsSpeaking &&
            (System.currentTimeMillis() - ttsWaitStart) < TTS_MAX_WAIT_MS
        ) {
            delay(TTS_POLL_INTERVAL_MS)
        }

        // Small buffer after TTS ends to avoid clipping the end of speech.
        delay(300)
        if (!_isListening.value) {
            voiceProcessor.startContinuousListening()
        }
    }

    private fun updateEffectiveEmotion() {
        _effectiveEmotion.update {
            when {
                _currentEmotion.value != Emotion.UNKNOWN -> _currentEmotion.value
                _audioToneEmotion.value != Emotion.UNKNOWN -> _audioToneEmotion.value
                else -> Emotion.NEUTRAL
            }
        }
    }

    private fun updateAiActiveState() {
        _isAiAgentActive.update {
            _isModelLoaded.value || _cameraPermissionGranted.value || _isListening.value || _isSpeaking.value
        }
    }

    private fun hasPremiumFeature(feature: PremiumFeature): Boolean {
        return _premiumFeatureMatrix.value[feature] == true
    }

    private fun checkDailyCheckInNeeded() {
        viewModelScope.launch {
            val freq = _checkInFrequency.value
            if (freq == "as_needed") return@launch
            val lastDate = memoryManager.getLastCheckInDate()
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())
            val shouldShow = when (freq) {
                "weekly" -> {
                    if (lastDate.isBlank()) true
                    else {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        val last = runCatching { sdf.parse(lastDate)?.time ?: 0L }.getOrDefault(0L)
                        System.currentTimeMillis() - last > java.util.concurrent.TimeUnit.DAYS.toMillis(7)
                    }
                }
                else -> lastDate != today // "daily" or unknown
            }
            if (shouldShow) {
                _showDailyCheckIn.update { true }
            }
        }
    }

    /**
     * Remotely enables or disables all premium features for every installed instance.
     * When [enabled] is `true` (default), every user gets free access to all features.
     * Setting it to `false` re-gates them, acting as a remote kill-switch.
     */
    fun setPremiumFeaturesEnabled(enabled: Boolean) {
        _premiumFeaturesGloballyEnabled.update { enabled }
        _premiumFeatureMatrix.update { buildPremiumMatrix(enabled) }
        viewModelScope.launch {
            memoryManager.setPremiumFeaturesGloballyEnabled(enabled)
        }
    }

    /**
     * Builds the premium feature access map.
     * When [featuresEnabled] is true all features are granted (free-for-all mode).
     * When false, all features are disabled (remote kill-switch).
     */
    private fun buildPremiumMatrix(featuresEnabled: Boolean): Map<PremiumFeature, Boolean> {
        return PremiumFeature.entries.associateWith { featuresEnabled }
    }

    override fun onCleared() {
        super.onCleared()
        responseEngine.release()
        emotionDetector.release()
        activityAnalyzer.release()
        voiceProcessor.release()
    }

    companion object {
        private const val TAG = "ChatViewModel"

        /** Maximum time to wait for TTS to finish before restarting the mic. */
        private const val TTS_MAX_WAIT_MS = 15_000L

        /** Polling interval while waiting for TTS to finish. */
        private const val TTS_POLL_INTERVAL_MS = 100L
    }
}
