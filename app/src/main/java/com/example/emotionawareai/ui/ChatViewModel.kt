package com.example.emotionawareai.ui

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.emotionawareai.domain.model.ActivityCaption
import com.example.emotionawareai.domain.model.ChatMessage
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.MessageRole
import com.example.emotionawareai.billing.BillingManager
import com.example.emotionawareai.billing.PremiumOffer
import com.example.emotionawareai.billing.PremiumPlanType
import com.example.emotionawareai.domain.model.PremiumFeature
import com.example.emotionawareai.engine.ActivityAnalyzer
import com.example.emotionawareai.engine.EmotionDetector
import com.example.emotionawareai.manager.ConversationManager
import com.example.emotionawareai.manager.MemoryManager
import com.example.emotionawareai.manager.ResponseEngine
import com.example.emotionawareai.voice.AudioToneAnalyzer
import com.example.emotionawareai.voice.ToneInsight
import com.example.emotionawareai.voice.VoiceError
import com.example.emotionawareai.voice.VoiceProcessor
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

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationManager: ConversationManager,
    private val responseEngine: ResponseEngine,
    private val emotionDetector: EmotionDetector,
    private val activityAnalyzer: ActivityAnalyzer,
    private val voiceProcessor: VoiceProcessor,
    private val memoryManager: MemoryManager,
    private val audioToneAnalyzer: AudioToneAnalyzer,
    private val billingManager: BillingManager
) : ViewModel() {

    // ── UI State ─────────────────────────────────────────────────────────────

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _currentEmotion = MutableStateFlow(Emotion.NEUTRAL)
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

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _cameraPermissionGranted = MutableStateFlow(false)
    val cameraPermissionGranted: StateFlow<Boolean> = _cameraPermissionGranted.asStateFlow()

    private val _audioPermissionGranted = MutableStateFlow(false)
    val audioPermissionGranted: StateFlow<Boolean> = _audioPermissionGranted.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    private val _isContinuousConversationEnabled = MutableStateFlow(false)
    val isContinuousConversationEnabled: StateFlow<Boolean> =
        _isContinuousConversationEnabled.asStateFlow()

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

    private var generationJob: Job? = null
    private var messageIdCounter = 0L

    // ── Initialisation ────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            initializeSession()
            observeEmotionDetection()
            observeActivityCaptions()
            observeVoiceRecognition()
            observeAudioToneSignals()
            observeBillingState()
        }
    }

    private suspend fun initializeSession() {
        conversationManager.ensureConversation()
        val convId = conversationManager.getActiveConversationId()

        val history = memoryManager.getRecentContext(convId, limit = 20)
        _messages.update { history }

        _isTtsEnabled.update { memoryManager.isTtsEnabled() }
        responseEngine.setTtsEnabled(_isTtsEnabled.value)

        _isContinuousConversationEnabled.update { memoryManager.isContinuousConversationEnabled() }
        _isPremiumUser.update { memoryManager.isPremiumUnlocked() }
        _isProThemeEnabled.update { memoryManager.isProThemeEnabled() }
        _isExportWithInsights.update { memoryManager.isExportWithInsightsEnabled() }
        val globallyEnabled = memoryManager.isPremiumFeaturesGloballyEnabled()
        _premiumFeaturesGloballyEnabled.update { globallyEnabled }
        // Grant all features for free; respect the remote kill-switch if off.
        _premiumFeatureMatrix.update { buildPremiumMatrix(globallyEnabled) }

        viewModelScope.launch(Dispatchers.IO) {
            val loaded = responseEngine.loadModel()
            _isModelLoaded.update { loaded }
            updateAiActiveState()
            Log.i(TAG, "Model loaded: $loaded")
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

        val userMessage = ChatMessage(
            id = ++messageIdCounter,
            content = text.trim(),
            role = MessageRole.USER,
            emotion = _currentEmotion.value
        )

        _messages.update { it + userMessage }

        generationJob = viewModelScope.launch {
            conversationManager.saveMessage(userMessage)
            _isGenerating.update { true }

            val context = conversationManager.buildContext(
                userMessage = text.trim(),
                emotion = _effectiveEmotion.value,
                audioToneEmotion = _audioToneEmotion.value,
                historyLimit = if (_isPremiumUser.value) 20 else 6
            )

            val streamingMessage = ChatMessage(
                id = ++messageIdCounter,
                content = "",
                role = MessageRole.ASSISTANT,
                isStreaming = true
            )
            _messages.update { it + streamingMessage }

            val fullResponse = StringBuilder()

            responseEngine.generateResponse(context)
                .catch { e ->
                    Log.e(TAG, "Generation error: ${e.message}", e)
                    _errorMessage.update { "Failed to generate response: ${e.message}" }
                }
                .collect { token ->
                    fullResponse.append(token)
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
        Log.i(TAG, "startVoiceInput")
        voiceProcessor.startListening()
    }

    fun stopVoiceInput() {
        voiceProcessor.stopListening()
    }

    fun toggleTts() {
        val newState = !_isTtsEnabled.value
        _isTtsEnabled.update { newState }
        responseEngine.setTtsEnabled(newState)
        viewModelScope.launch {
            memoryManager.setTtsEnabled(newState)
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
            startVoiceInput()
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
            appendLine("Face emotion: ${_currentEmotion.value.displayName}")
            if (_isExportWithInsights.value && tone != null) {
                appendLine("Tone: ${tone.label} (confidence ${(tone.confidence * 100).toInt()}%)")
            }
            appendLine()
        }

        val transcript = messages.value.joinToString(separator = "\n") { msg ->
            val role = if (msg.isFromUser) "You" else "MoodMitra"
            "[$role] ${msg.content}"
        }

        _exportPayload.update { header + transcript }
    }

    fun clearExportPayload() {
        _exportPayload.update { null }
    }

    fun onPermissionsResult(cameraGranted: Boolean, audioGranted: Boolean) {
        Log.i(TAG, "onPermissionsResult: camera=$cameraGranted, audio=$audioGranted")
        _cameraPermissionGranted.update { cameraGranted }
        _audioPermissionGranted.update { audioGranted }
        updateAiActiveState()

        if (cameraGranted) {
            emotionDetector.initialize()
            activityAnalyzer.initialize()
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

    private suspend fun maybeResumeContinuousConversation(fromVoiceInput: Boolean) {
        if (!fromVoiceInput) return
        if (!_isContinuousConversationEnabled.value) return
        if (!_audioPermissionGranted.value) return

        // Small delay helps avoid cutting off the tail end of spoken output.
        delay(300)
        startVoiceInput()
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
            _isModelLoaded.value || _cameraPermissionGranted.value || _isListening.value
        }
    }

    private fun hasPremiumFeature(feature: PremiumFeature): Boolean {
        return _premiumFeatureMatrix.value[feature] == true
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
    }
}
