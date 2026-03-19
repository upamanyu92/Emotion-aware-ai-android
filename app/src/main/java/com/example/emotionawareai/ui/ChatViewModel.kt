package com.example.emotionawareai.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.emotionawareai.domain.model.ChatMessage
import com.example.emotionawareai.domain.model.Emotion
import com.example.emotionawareai.domain.model.MessageRole
import com.example.emotionawareai.engine.EmotionDetector
import com.example.emotionawareai.manager.ConversationManager
import com.example.emotionawareai.manager.MemoryManager
import com.example.emotionawareai.manager.ResponseEngine
import com.example.emotionawareai.voice.VoiceError
import com.example.emotionawareai.voice.VoiceProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val voiceProcessor: VoiceProcessor,
    private val memoryManager: MemoryManager
) : ViewModel() {

    // ── UI State ─────────────────────────────────────────────────────────────

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _currentEmotion = MutableStateFlow(Emotion.NEUTRAL)
    val currentEmotion: StateFlow<Emotion> = _currentEmotion.asStateFlow()

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

    private var generationJob: Job? = null
    private var messageIdCounter = 0L

    // ── Initialisation ────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            initializeSession()
            observeEmotionDetection()
            observeVoiceRecognition()
        }
    }

    private suspend fun initializeSession() {
        conversationManager.ensureConversation()
        val convId = conversationManager.getActiveConversationId()

        val history = memoryManager.getRecentContext(convId, limit = 20)
        _messages.update { history }

        _isTtsEnabled.update { memoryManager.isTtsEnabled() }
        responseEngine.setTtsEnabled(_isTtsEnabled.value)

        viewModelScope.launch(Dispatchers.IO) {
            val loaded = responseEngine.loadModel()
            _isModelLoaded.update { loaded }
            Log.i(TAG, "Model loaded: $loaded")
        }
    }

    private fun observeEmotionDetection() {
        viewModelScope.launch {
            emotionDetector.emotionFlow.collect { emotion ->
                _currentEmotion.update { emotion }
            }
        }
    }

    private fun observeVoiceRecognition() {
        viewModelScope.launch {
            voiceProcessor.recognizedTextFlow.collect { text ->
                if (text.isNotBlank()) {
                    sendMessage(text)
                }
            }
        }

        viewModelScope.launch {
            voiceProcessor.listeningStateFlow.collect { listening ->
                _isListening.update { listening }
            }
        }

        viewModelScope.launch {
            voiceProcessor.errorFlow.collect { error ->
                handleVoiceError(error)
            }
        }
    }

    // ── Public Actions ────────────────────────────────────────────────────────

    fun sendMessage(text: String) {
        if (text.isBlank() || _isGenerating.value) return

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
                emotion = _currentEmotion.value
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

            conversationManager.saveMessage(finalMessage)
            _isGenerating.update { false }
        }
    }

    fun startVoiceInput() {
        if (!_audioPermissionGranted.value) {
            _errorMessage.update { "Microphone permission is required for voice input" }
            return
        }
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

    fun onPermissionsResult(cameraGranted: Boolean, audioGranted: Boolean) {
        _cameraPermissionGranted.update { cameraGranted }
        _audioPermissionGranted.update { audioGranted }

        if (cameraGranted) {
            emotionDetector.initialize()
        }
    }

    fun clearError() {
        _errorMessage.update { null }
    }

    fun cancelGeneration() {
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

    override fun onCleared() {
        super.onCleared()
        responseEngine.release()
        emotionDetector.release()
        voiceProcessor.release()
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
