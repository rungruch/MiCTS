package com.parallelc.micts.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.data.AiGateway
import com.parallelc.micts.data.AiGatewayFactory
import com.parallelc.micts.data.AiImageEncoder
import com.parallelc.micts.data.AiKeyStorageFactory
import com.parallelc.micts.data.CaptureFiles
import com.parallelc.micts.data.TextRecognitionGateway
import com.parallelc.micts.data.TextRecognitionGatewayFactory
import com.parallelc.micts.domain.AiChatMessage
import com.parallelc.micts.domain.AiConversationState
import com.parallelc.micts.domain.AiMessageRole
import com.parallelc.micts.domain.AiPromptBuilder
import com.parallelc.micts.domain.CaptureFailureReason
import com.parallelc.micts.domain.CropGeometry
import com.parallelc.micts.domain.FloatRect
import com.parallelc.micts.domain.RecognitionResult
import com.parallelc.micts.domain.RecognizedTextLine
import com.parallelc.micts.domain.TextSelection
import com.parallelc.micts.domain.ViewportState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface CaptureContentState {
    data object Loading : CaptureContentState
    data class Ready(val bitmap: Bitmap) : CaptureContentState
    data object Protected : CaptureContentState
    data class Error(val reason: CaptureFailureReason) : CaptureContentState
}

enum class TextRecognitionStatus {
    DISABLED,
    FINDING,
    READY,
    EMPTY,
    FAILED,
}

data class CropEditorUiState(
    val content: CaptureContentState = CaptureContentState.Loading,
    val selection: FloatRect? = null,
    val viewport: ViewportState = ViewportState(),
    val recognizedLines: List<RecognizedTextLine> = emptyList(),
    val selectedText: String = "",
    val recognitionStatus: TextRecognitionStatus = TextRecognitionStatus.DISABLED,
    val isActing: Boolean = false,
    val aiConversation: AiConversationState = AiConversationState(),
    val aiEnabled: Boolean = false,
    val aiConfigured: Boolean = false,
)

class CropViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    companion object {
        const val EXTRA_PROBABLY_PROTECTED = "probably_protected"
        const val EXTRA_FAILURE_REASON = "failure_reason"

        private const val STATE_SELECTION_LEFT = "selection_left"
        private const val STATE_SELECTION_TOP = "selection_top"
        private const val STATE_SELECTION_RIGHT = "selection_right"
        private const val STATE_SELECTION_BOTTOM = "selection_bottom"
        private const val STATE_VIEWPORT_ZOOM = "viewport_zoom"
        private const val STATE_VIEWPORT_PAN_X = "viewport_pan_x"
        private const val STATE_VIEWPORT_PAN_Y = "viewport_pan_y"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CropViewModel(
                    application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as Application,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }

    private val prefs = application.getSharedPreferences(AppConfig.CONFIG_NAME, Context.MODE_PRIVATE)
    private val aiKeyStorage = AiKeyStorageFactory.create(application)

    private val recognitionEnabled = prefs.getBoolean(
        AppConfig.KEY_LOCAL_TEXT_RECOGNITION,
        AppConfig.DEFAULT_CONFIG[AppConfig.KEY_LOCAL_TEXT_RECOGNITION] as Boolean,
    )
    val aiEnabled: Boolean = prefs.getBoolean(
        AppConfig.KEY_AI_ENABLED,
        AppConfig.DEFAULT_CONFIG[AppConfig.KEY_AI_ENABLED] as Boolean,
    )
    val aiBaseUrl: String = prefs.getString(
        AppConfig.KEY_AI_BASE_URL,
        AppConfig.DEFAULT_AI_BASE_URL,
    ) ?: AppConfig.DEFAULT_AI_BASE_URL
    val aiApiKey: String = aiKeyStorage.getApiKey()
    val aiModel: String = prefs.getString(
        AppConfig.KEY_AI_MODEL,
        AppConfig.DEFAULT_AI_MODEL,
    ) ?: AppConfig.DEFAULT_AI_MODEL
    val aiSendImage: Boolean = prefs.getBoolean(
        AppConfig.KEY_AI_SEND_IMAGE,
        AppConfig.DEFAULT_CONFIG[AppConfig.KEY_AI_SEND_IMAGE] as Boolean,
    )
    val aiConfigured: Boolean = aiEnabled && aiApiKey.isNotBlank() && aiModel.isNotBlank() && aiBaseUrl.isNotBlank()

    private var recognitionGateway: TextRecognitionGateway? = null
    private var aiGateway: AiGateway? = null

    private val _state = MutableStateFlow(
        CropEditorUiState(
            viewport = restoredViewport(),
            recognitionStatus = if (recognitionEnabled) {
                TextRecognitionStatus.FINDING
            } else {
                TextRecognitionStatus.DISABLED
            },
            aiEnabled = aiEnabled,
            aiConfigured = aiConfigured,
        ),
    )
    val state: StateFlow<CropEditorUiState> = _state.asStateFlow()

    init {
        val failureReason = savedStateHandle.get<String>(EXTRA_FAILURE_REASON)
            ?.let { stored -> CaptureFailureReason.entries.firstOrNull { it.name == stored } }
        val probablyProtected = savedStateHandle[EXTRA_PROBABLY_PROTECTED] ?: false
        when {
            failureReason != null -> _state.update {
                it.copy(content = CaptureContentState.Error(failureReason))
            }
            probablyProtected -> _state.update { it.copy(content = CaptureContentState.Protected) }
            else -> loadCapture()
        }
    }

    fun retryRecognition() {
        val bitmap = (_state.value.content as? CaptureContentState.Ready)?.bitmap ?: return
        if (!recognitionEnabled || _state.value.recognitionStatus == TextRecognitionStatus.FINDING) {
            return
        }
        recognize(bitmap)
    }

    fun updateSelection(selection: FloatRect) {
        val bitmap = (_state.value.content as? CaptureContentState.Ready)?.bitmap ?: return
        val bounds = FloatRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        val safe = CropGeometry.clampToBounds(selection, bounds, 1f)
        persistSelection(safe)
        _state.update {
            it.copy(
                selection = safe,
                selectedText = TextSelection.textInside(safe, it.recognizedLines),
            )
        }
    }

    fun selectLine(line: RecognizedTextLine) {
        val bitmap = (_state.value.content as? CaptureContentState.Ready)?.bitmap ?: return
        val padding = (minOf(bitmap.width, bitmap.height) * 0.012f).coerceAtLeast(8f)
        updateSelection(
            CropGeometry.clampToBounds(
                FloatRect(
                    line.bounds.left - padding,
                    line.bounds.top - padding,
                    line.bounds.right + padding,
                    line.bounds.bottom + padding,
                ),
                FloatRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
                1f,
            ),
        )
    }

    fun updateViewport(viewport: ViewportState) {
        val safe = viewport.copy(zoom = viewport.zoom.coerceIn(1f, 5f))
        savedStateHandle[STATE_VIEWPORT_ZOOM] = safe.zoom
        savedStateHandle[STATE_VIEWPORT_PAN_X] = safe.panXFraction
        savedStateHandle[STATE_VIEWPORT_PAN_Y] = safe.panYFraction
        _state.update { it.copy(viewport = safe) }
    }

    fun setActing(acting: Boolean) {
        _state.update { it.copy(isActing = acting) }
    }

    private fun loadCapture() {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(CaptureFiles.capture(getApplication()).absolutePath)
            }
            if (bitmap == null) {
                _state.update {
                    it.copy(content = CaptureContentState.Error(CaptureFailureReason.EMPTY_IMAGE))
                }
                return@launch
            }
            val selection = restoredSelection() ?: CropGeometry.initialCrop(
                FloatRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
            )
            persistSelection(selection)
            _state.update {
                it.copy(content = CaptureContentState.Ready(bitmap), selection = selection)
            }
            if (recognitionEnabled) recognize(bitmap)
        }
    }

    private fun recognize(bitmap: Bitmap) {
        _state.update { it.copy(recognitionStatus = TextRecognitionStatus.FINDING) }
        viewModelScope.launch {
            val gateway = recognitionGateway ?: TextRecognitionGatewayFactory.create().also {
                recognitionGateway = it
            }
            when (val result = withContext(Dispatchers.Default) { gateway.recognize(bitmap) }) {
                is RecognitionResult.Success -> {
                    val selection = _state.value.selection
                    _state.update {
                        it.copy(
                            recognizedLines = result.lines,
                            selectedText = selection?.let { selected ->
                                TextSelection.textInside(selected, result.lines)
                            }.orEmpty(),
                            recognitionStatus = if (result.lines.isEmpty()) {
                                TextRecognitionStatus.EMPTY
                            } else {
                                TextRecognitionStatus.READY
                            },
                        )
                    }
                }
                is RecognitionResult.Failure -> _state.update {
                    it.copy(recognitionStatus = TextRecognitionStatus.FAILED)
                }
            }
        }
    }

    private fun restoredSelection(): FloatRect? {
        val left = savedStateHandle.get<Float>(STATE_SELECTION_LEFT) ?: return null
        val top = savedStateHandle.get<Float>(STATE_SELECTION_TOP) ?: return null
        val right = savedStateHandle.get<Float>(STATE_SELECTION_RIGHT) ?: return null
        val bottom = savedStateHandle.get<Float>(STATE_SELECTION_BOTTOM) ?: return null
        return FloatRect(left, top, right, bottom)
    }

    private fun persistSelection(selection: FloatRect) {
        savedStateHandle[STATE_SELECTION_LEFT] = selection.left
        savedStateHandle[STATE_SELECTION_TOP] = selection.top
        savedStateHandle[STATE_SELECTION_RIGHT] = selection.right
        savedStateHandle[STATE_SELECTION_BOTTOM] = selection.bottom
    }

    private fun restoredViewport(): ViewportState = ViewportState(
        zoom = savedStateHandle[STATE_VIEWPORT_ZOOM] ?: 1f,
        panXFraction = savedStateHandle[STATE_VIEWPORT_PAN_X] ?: 0f,
        panYFraction = savedStateHandle[STATE_VIEWPORT_PAN_Y] ?: 0f,
    )

    fun askAi(question: String, isInitial: Boolean = false) {
        val currentConversation = _state.value.aiConversation
        if (currentConversation.isLoading) return

        val bitmap = (_state.value.content as? CaptureContentState.Ready)?.bitmap
        val selection = _state.value.selection

        val imageBase64Url = if (aiSendImage && isInitial && bitmap != null && selection != null) {
            val cropRect = CropGeometry.toIntRect(selection, bitmap.width, bitmap.height)
            AiImageEncoder.encodeRegionToBase64(bitmap, cropRect)
        } else {
            null
        }

        val allLinesText = _state.value.recognizedLines.joinToString("\n") { it.text.trim() }
        val promptText = if (isInitial) {
            AiPromptBuilder.buildUserQuestion(_state.value.selectedText, allLinesText, question)
        } else {
            question.trim()
        }

        val userMessage = AiChatMessage(
            role = AiMessageRole.USER,
            text = promptText,
            imageBase64Url = imageBase64Url,
        )

        val updatedMessages = currentConversation.messages + userMessage
        _state.update {
            it.copy(
                aiConversation = it.aiConversation.copy(
                    messages = updatedMessages,
                    isLoading = true,
                    error = null,
                )
            )
        }

        viewModelScope.launch {
            val gateway = aiGateway ?: AiGatewayFactory.create(aiBaseUrl, aiApiKey).also {
                aiGateway = it
            }

            val result = withContext(Dispatchers.Default) {
                gateway.chat(updatedMessages, aiModel, aiSendImage)
            }

            result.fold(
                onSuccess = { assistantReply ->
                    val assistantMessage = AiChatMessage(
                        role = AiMessageRole.ASSISTANT,
                        text = assistantReply,
                    )
                    _state.update {
                        it.copy(
                            aiConversation = it.aiConversation.copy(
                                messages = updatedMessages + assistantMessage,
                                isLoading = false,
                                error = null,
                            )
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            aiConversation = it.aiConversation.copy(
                                isLoading = false,
                                error = error.localizedMessage ?: error.message ?: "Unknown error",
                            )
                        )
                    }
                }
            )
        }
    }

    fun retryAi() {
        val lastUserMessage = _state.value.aiConversation.messages.lastOrNull { it.role == AiMessageRole.USER }
        if (lastUserMessage != null) {
            val historyWithoutLast = _state.value.aiConversation.messages.dropLast(1)
            _state.update {
                it.copy(
                    aiConversation = it.aiConversation.copy(
                        messages = historyWithoutLast,
                        error = null,
                    )
                )
            }
            askAi(lastUserMessage.text, isInitial = false)
        }
    }

    fun resetAiConversation() {
        _state.update {
            it.copy(aiConversation = AiConversationState())
        }
    }

    override fun onCleared() {
        recognitionGateway?.close()
        aiGateway?.close()
        (_state.value.content as? CaptureContentState.Ready)?.bitmap?.recycle()
    }
}
