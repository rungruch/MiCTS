package com.parallelc.micts.ui.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.parallelc.micts.R
import com.parallelc.micts.data.AndroidExternalActionGateway
import com.parallelc.micts.data.CaptureFiles
import com.parallelc.micts.data.ExternalActionKind
import com.parallelc.micts.data.ExternalActionResult
import com.parallelc.micts.data.LensShareGateway
import com.parallelc.micts.domain.AiChatMessage
import com.parallelc.micts.domain.AiMessageRole
import com.parallelc.micts.domain.CaptureFailureReason
import com.parallelc.micts.domain.CropGeometry
import com.parallelc.micts.domain.CropHandle
import com.parallelc.micts.domain.EditorActionPolicy
import com.parallelc.micts.domain.FloatPoint
import com.parallelc.micts.domain.FloatRect
import com.parallelc.micts.domain.FloatSize
import com.parallelc.micts.domain.IntCropRect
import com.parallelc.micts.domain.RecognizedTextLine
import com.parallelc.micts.domain.ViewportState
import com.parallelc.micts.ui.theme.MiCTSTheme
import com.parallelc.micts.ui.theme.editorScrimColor
import com.parallelc.micts.ui.theme.geminiGradientColors
import com.parallelc.micts.ui.theme.glassBorderColor
import com.parallelc.micts.ui.theme.glassContainerColor
import com.parallelc.micts.ui.theme.glassContentColor
import com.parallelc.micts.ui.viewmodel.CaptureContentState
import com.parallelc.micts.ui.viewmodel.CropEditorUiState
import com.parallelc.micts.ui.viewmodel.CropViewModel
import com.parallelc.micts.ui.viewmodel.TextRecognitionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import kotlin.math.hypot
import kotlin.math.roundToInt

class CropActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_AUTO_LENS = "auto_lens"

        fun createIntent(
            context: android.content.Context,
            probablyProtected: Boolean,
            failureReason: CaptureFailureReason?,
            autoLens: Boolean = false,
        ): Intent = Intent(context, CropActivity::class.java).apply {
            putExtra(CropViewModel.EXTRA_PROBABLY_PROTECTED, probablyProtected)
            putExtra(EXTRA_AUTO_LENS, autoLens)
            failureReason?.let { putExtra(CropViewModel.EXTRA_FAILURE_REASON, it.name) }
        }
    }

    private val viewModel by viewModels<CropViewModel> { CropViewModel.Factory }
    private lateinit var externalActions: AndroidExternalActionGateway
    private var lensUnavailable by mutableStateOf(false)
    private var browserFallback by mutableStateOf<ExternalActionResult.BrowserFallback?>(null)
    private var unavailableAction by mutableStateOf<ExternalActionKind?>(null)

    private var autoLensRequested = false
    private var autoLensCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()
        externalActions = AndroidExternalActionGateway(this)

        autoLensRequested = intent.getBooleanExtra(EXTRA_AUTO_LENS, false)

        setContent {
            MiCTSTheme {
                val state by viewModel.state.collectAsState()
                CropRoute(
                    state = state,
                    lensUnavailable = lensUnavailable,
                    onSelectionChanged = viewModel::updateSelection,
                    onViewportChanged = viewModel::updateViewport,
                    onLineTapped = viewModel::selectLine,
                    onRetryRecognition = viewModel::retryRecognition,
                    onCopy = { runTextAction(externalActions.copy(state.selectedText)) },
                    onSearch = { runTextAction(externalActions.search(state.selectedText)) },
                    onTranslate = { runTextAction(externalActions.translate(state.selectedText)) },
                    onLens = { searchWithLens(fullScreen = false) },
                    onFullScreenLens = { searchWithLens(fullScreen = true) },
                    onRetake = ::retake,
                    onCancel = ::cancel,
                    onDismissLensUnavailable = { lensUnavailable = false },
                    onOpenGoogleStore = ::openGoogleStore,
                    onAskAiInitial = { viewModel.askAi(getString(R.string.ai_summarize_prompt), isInitial = true) },
                    onSendAiMessage = { viewModel.askAi(it, isInitial = false) },
                    onRetryAi = viewModel::retryAi,
                    onCopyText = { text -> runTextAction(externalActions.copy(text)) },
                )
                browserFallback?.let { fallback ->
                    BrowserFallbackDialog(
                        kind = fallback.kind,
                        onDismiss = { browserFallback = null },
                        onOpenBrowser = {
                            browserFallback = null
                            if (externalActions.openBrowser(fallback.uri)) {
                                CaptureFiles.deleteCapture(this@CropActivity)
                                finish()
                            } else {
                                unavailableAction = fallback.kind
                            }
                        },
                    )
                }
                unavailableAction?.let { kind ->
                    ExternalUnavailableDialog(
                        kind = kind,
                        onDismiss = { unavailableAction = null },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (autoLensRequested && !autoLensCompleted) {
            autoLensRequested = false
            autoLensCompleted = true
            requestAutoLens()
        }
    }

    private fun requestAutoLens() {
        // Run after the activity is genuinely foreground: starting another app's
        // activity (Lens) from an activity that is not yet resumed is treated as
        // a background start and fails. This mirrors the user-tap path that works.
        val lens = LensShareGateway(this)
        if (lens.canShareToGoogle() && lens.share(CaptureFiles.capture(this))) {
            finish()
        } else {
            // Lens unavailable or share failed: fall back gracefully to the
            // normal editor and explain why the Lens action is unavailable.
            lensUnavailable = true
        }
    }

    private fun runTextAction(result: ExternalActionResult) {
        when (result) {
            ExternalActionResult.Copied -> Toast.makeText(
                this,
                R.string.text_copied,
                Toast.LENGTH_SHORT,
            ).show()
            ExternalActionResult.Launched -> {
                CaptureFiles.deleteCapture(this)
                finish()
            }
            is ExternalActionResult.BrowserFallback -> browserFallback = result
            is ExternalActionResult.Unavailable -> unavailableAction = result.kind
        }
    }

    private fun searchWithLens(fullScreen: Boolean) {
        val state = viewModel.state.value
        val bitmap = (state.content as? CaptureContentState.Ready)?.bitmap ?: return
        val selection = state.selection ?: return
        if (state.isActing) return
        val lensGateway = LensShareGateway(this)
        if (!lensGateway.canShareToGoogle()) {
            lensUnavailable = true
            return
        }
        val crop = if (fullScreen) {
            IntCropRect(0, 0, bitmap.width, bitmap.height)
        } else {
            CropGeometry.toIntRect(selection, bitmap.width, bitmap.height)
        }
        viewModel.setActing(true)
        lifecycleScope.launch {
            val output = CaptureFiles.capture(this@CropActivity)
            val readyToShare = if (fullScreen) {
                output.isFile
            } else {
                withContext(Dispatchers.IO) { writeCrop(bitmap, crop) }
            }
            if (!readyToShare) {
                viewModel.setActing(false)
                Toast.makeText(
                    this@CropActivity,
                    R.string.capture_write_failed,
                    Toast.LENGTH_LONG,
                ).show()
                return@launch
            }
            if (lensGateway.share(output)) {
                finish()
            } else {
                if (!fullScreen) withContext(Dispatchers.IO) { writeBitmap(bitmap) }
                viewModel.setActing(false)
                lensUnavailable = true
            }
        }
    }

    private fun writeCrop(bitmap: Bitmap, crop: IntCropRect): Boolean = runCatching {
        val cropped = Bitmap.createBitmap(bitmap, crop.left, crop.top, crop.width, crop.height)
        try {
            writeBitmap(cropped)
        } finally {
            cropped.recycle()
        }
    }.getOrDefault(false)

    private fun writeBitmap(bitmap: Bitmap): Boolean {
        val output = CaptureFiles.capture(this)
        return FileOutputStream(output).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private fun retake() {
        CaptureFiles.deleteCapture(this)
        startActivity(MainActivity.createLensFallbackIntent(this))
        finish()
    }

    private fun cancel() {
        CaptureFiles.deleteCapture(this)
        finish()
    }

    private fun openGoogleStore() {
        val packageName = LensShareGateway.GOOGLE_APP_PACKAGE
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        try {
            startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                ),
            )
        }
    }
}

@Composable
private fun CropRoute(
    state: CropEditorUiState,
    lensUnavailable: Boolean,
    onSelectionChanged: (FloatRect) -> Unit,
    onViewportChanged: (ViewportState) -> Unit,
    onLineTapped: (RecognizedTextLine) -> Unit,
    onRetryRecognition: () -> Unit,
    onCopy: () -> Unit,
    onSearch: () -> Unit,
    onTranslate: () -> Unit,
    onLens: () -> Unit,
    onFullScreenLens: () -> Unit,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
    onDismissLensUnavailable: () -> Unit,
    onOpenGoogleStore: () -> Unit,
    onAskAiInitial: () -> Unit,
    onSendAiMessage: (String) -> Unit,
    onRetryAi: () -> Unit,
    onCopyText: (String) -> Unit,
) {
    var showAiChat by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when (val content = state.content) {
            CaptureContentState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            CaptureContentState.Protected -> CaptureProblem(
                title = stringResource(R.string.protected_capture_title),
                message = stringResource(R.string.protected_capture_message),
                onRetake = onRetake,
                onCancel = onCancel,
                modifier = Modifier.fillMaxSize(),
            )
            is CaptureContentState.Error -> CaptureProblem(
                title = stringResource(R.string.capture_failed_title),
                message = captureFailureMessage(content.reason),
                onRetake = onRetake,
                onCancel = onCancel,
                modifier = Modifier.fillMaxSize(),
            )
            is CaptureContentState.Ready -> CropScreen(
                state = state,
                bitmap = content.bitmap,
                onSelectionChanged = onSelectionChanged,
                onViewportChanged = onViewportChanged,
                onLineTapped = onLineTapped,
                onRetryRecognition = onRetryRecognition,
                onCopy = onCopy,
                onSearch = onSearch,
                onTranslate = onTranslate,
                onLens = onLens,
                onAskAi = {
                    showAiChat = true
                    if (state.aiConversation.messages.isEmpty()) {
                        onAskAiInitial()
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onClose = onCancel,
                onRetake = onRetake,
                onFullScreenLens = onFullScreenLens,
            )
        }
    }
    if (lensUnavailable) {
        LensUnavailableDialog(onDismissLensUnavailable, onOpenGoogleStore)
    }
    if (showAiChat) {
        AiChatSheet(
            state = state,
            onDismiss = { showAiChat = false },
            onSendMessage = onSendAiMessage,
            onRetry = onRetryAi,
            onCopyMessage = onCopyText,
        )
    }
}

/**
 * Full-bleed, Circle-to-Search style editor: the captured screenshot fills the
 * screen behind floating glass controls and a bottom search pill.
 */
@Composable
internal fun CropScreen(
    state: CropEditorUiState,
    bitmap: Bitmap,
    onSelectionChanged: (FloatRect) -> Unit,
    onViewportChanged: (ViewportState) -> Unit,
    onLineTapped: (RecognizedTextLine) -> Unit,
    onRetryRecognition: () -> Unit,
    onCopy: () -> Unit,
    onSearch: () -> Unit,
    onTranslate: () -> Unit,
    onLens: () -> Unit,
    modifier: Modifier = Modifier,
    onAskAi: () -> Unit = {},
    onClose: () -> Unit = {},
    onRetake: () -> Unit = {},
    onFullScreenLens: () -> Unit = {},
) {
    var overflowExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize()) {
        state.selection?.let { selection ->
            CropEditor(
                bitmap = bitmap,
                selection = selection,
                viewport = state.viewport,
                lines = state.recognizedLines,
                status = state.recognitionStatus,
                onSelectionChanged = onSelectionChanged,
                onViewportChanged = onViewportChanged,
                onLineTapped = onLineTapped,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Floating top controls (Circle-to-Search style chrome).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(onClick = onClose) {
                Icon(Icons.Default.Close, stringResource(R.string.close))
            }
            Spacer(Modifier.weight(1f))
            GlassIconButton(onClick = onRetake) {
                Icon(Icons.Default.Refresh, stringResource(R.string.retake))
            }
            Spacer(Modifier.width(8.dp))
            Box {
                GlassIconButton(onClick = { overflowExpanded = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.more_actions))
                }
                DropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.search_full_screen_lens)) },
                        leadingIcon = { Icon(Icons.Default.ImageSearch, null) },
                        enabled = !state.isActing,
                        onClick = {
                            overflowExpanded = false
                            onFullScreenLens()
                        },
                    )
                }
            }
        }

        // Recognition status pill floating under the top controls.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 64.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            RecognitionStatusPill(state.recognitionStatus, onRetryRecognition)
        }

        EditorBottomBar(
            state = state,
            onCopy = onCopy,
            onSearch = onSearch,
            onTranslate = onTranslate,
            onLens = onLens,
            onAskAi = onAskAi,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = glassContainerColor(),
        contentColor = glassContentColor(),
        border = BorderStroke(1.dp, glassBorderColor()),
    ) {
        Box(contentAlignment = Alignment.Center) { icon() }
    }
}

@Composable
private fun RecognitionStatusPill(status: TextRecognitionStatus, onRetry: () -> Unit) {
    val visible = status == TextRecognitionStatus.FINDING ||
        status == TextRecognitionStatus.EMPTY ||
        status == TextRecognitionStatus.FAILED
    var lastVisibleStatus by remember { mutableStateOf(TextRecognitionStatus.FINDING) }
    LaunchedEffect(status) {
        if (status == TextRecognitionStatus.FINDING ||
            status == TextRecognitionStatus.EMPTY ||
            status == TextRecognitionStatus.FAILED
        ) {
            lastVisibleStatus = status
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut(),
    ) {
        val containerColor = glassContainerColor()
        val contentColor = glassContentColor()
        when (lastVisibleStatus) {
            TextRecognitionStatus.FINDING -> Surface(
                shape = CircleShape,
                color = containerColor,
                contentColor = contentColor,
                border = BorderStroke(1.dp, glassBorderColor()),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.finding_text),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            else -> Surface(
                onClick = onRetry,
                shape = CircleShape,
                color = containerColor,
                contentColor = contentColor,
                border = BorderStroke(1.dp, glassBorderColor()),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(
                            if (lastVisibleStatus == TextRecognitionStatus.EMPTY) {
                                R.string.no_text_found
                            } else {
                                R.string.text_recognition_retry
                            },
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorBottomBar(
    state: CropEditorUiState,
    onCopy: () -> Unit,
    onSearch: () -> Unit,
    onTranslate: () -> Unit,
    onLens: () -> Unit,
    onAskAi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enter = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { enter.targetState = true }
    val availability = EditorActionPolicy.availability(state.selectedText, state.isActing)
    AnimatedVisibility(
        visibleState = enter,
        enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EditorActionChip(
                    label = stringResource(R.string.copy_text),
                    enabled = availability.copy,
                    onClick = onCopy,
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(17.dp))
                }
                EditorActionChip(
                    label = stringResource(R.string.search_text),
                    enabled = availability.search,
                    onClick = onSearch,
                ) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(17.dp))
                }
                EditorActionChip(
                    label = stringResource(R.string.translate_text),
                    enabled = availability.translate,
                    onClick = onTranslate,
                ) {
                    Icon(Icons.Default.Translate, null, modifier = Modifier.size(17.dp))
                }
                if (state.aiConfigured) {
                    EditorActionChip(
                        label = stringResource(R.string.ask_ai),
                        enabled = !state.isActing,
                        onClick = onAskAi,
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            modifier = Modifier.size(17.dp),
                            tint = geminiGradientColors()[1],
                        )
                    }
                }
            }
            SearchActionPill(
                selectedText = state.selectedText,
                searchEnabled = availability.search,
                lensEnabled = availability.lens,
                isActing = state.isActing,
                onSearch = onSearch,
                onLens = onLens,
            )
        }
    }
}

@Composable
private fun EditorActionChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val containerColor = glassContainerColor()
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) containerColor else containerColor.copy(alpha = containerColor.alpha * 0.4f),
        contentColor = glassContentColor().let {
            if (enabled) it else it.copy(alpha = 0.38f)
        },
        border = BorderStroke(1.dp, glassBorderColor()),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun SearchActionPill(
    selectedText: String,
    searchEnabled: Boolean,
    lensEnabled: Boolean,
    isActing: Boolean,
    onSearch: () -> Unit,
    onLens: () -> Unit,
) {
    val contentColor = glassContentColor()
    Surface(
        onClick = onSearch,
        enabled = searchEnabled,
        shape = CircleShape,
        color = glassContainerColor(),
        contentColor = contentColor,
        border = BorderStroke(1.dp, glassBorderColor()),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GradientBadge(size = 40.dp)
            Text(
                text = selectedText.ifBlank {
                    stringResource(R.string.select_text_or_image_hint)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (selectedText.isBlank()) {
                    contentColor.copy(alpha = 0.6f)
                } else {
                    contentColor
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LensGradientButton(
                enabled = lensEnabled,
                isActing = isActing,
                onClick = onLens,
            )
        }
    }
}

/** Small circular badge with the Gemini gradient, used for sparkle icons. */
@Composable
private fun GradientBadge(
    size: Dp,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.AutoAwesome,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(Brush.linearGradient(geminiGradientColors()), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size / 2),
        )
    }
}

@Composable
private fun LensGradientButton(
    enabled: Boolean,
    isActing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradientBrush = Brush.horizontalGradient(geminiGradientColors())
    val disabledColor = glassContentColor().copy(alpha = 0.12f)
    val showGradient = enabled || isActing
    val contentColor = if (showGradient) {
        Color.White
    } else {
        glassContentColor().copy(alpha = 0.38f)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (showGradient) {
                        gradientBrush
                    } else {
                        Brush.horizontalGradient(listOf(disabledColor, disabledColor))
                    },
                    CircleShape,
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isActing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ImageSearch,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.lens),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CropEditor(
    bitmap: Bitmap,
    selection: FloatRect,
    viewport: ViewportState,
    lines: List<RecognizedTextLine>,
    status: TextRecognitionStatus,
    onSelectionChanged: (FloatRect) -> Unit,
    onViewportChanged: (ViewportState) -> Unit,
    onLineTapped: (RecognizedTextLine) -> Unit,
    modifier: Modifier = Modifier,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val currentSelection by rememberUpdatedState(selection)
    val currentViewport by rememberUpdatedState(viewport)
    val currentLines by rememberUpdatedState(lines)
    val currentSelectionCallback by rememberUpdatedState(onSelectionChanged)
    val currentViewportCallback by rememberUpdatedState(onViewportChanged)
    val currentLineCallback by rememberUpdatedState(onLineTapped)
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val editorDescription = stringResource(R.string.crop_editor_description)
    val density = LocalDensity.current
    val handleRadiusPx = with(density) { 28.dp.toPx() }
    val minCropSizePx = with(density) { 48.dp.toPx() }
    val touchSlopPx = with(density) { 8.dp.toPx() }
    val imageSize = FloatSize(bitmap.width.toFloat(), bitmap.height.toFloat())

    // Fx: animated gradient slide (selection border/handles) and the scanning
    // shimmer shown while local text recognition runs. The animated values are
    // only read inside the Canvas draw block so they invalidate draw only.
    val fxTransition = rememberInfiniteTransition(label = "editorFx")
    val gradientSlide by fxTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "gradientSlide",
    )
    val shimmerProgress by fxTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "shimmerProgress",
    )
    val linesAlpha by animateFloatAsState(
        targetValue = if (status == TextRecognitionStatus.READY) 1f else 0f,
        animationSpec = tween(450),
        label = "linesAlpha",
    )
    val gradientColors = geminiGradientColors()
    val scrimColor = editorScrimColor()

    Canvas(
        modifier = modifier
            .semantics { contentDescription = editorDescription }
            .onSizeChanged { canvasSize = it }
            .pointerInput(bitmap, canvasSize) {
                if (canvasSize == IntSize.Zero) return@pointerInput
                val container = FloatSize(canvasSize.width.toFloat(), canvasSize.height.toFloat())
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var transform = CropGeometry.viewport(imageSize, container, currentViewport)
                    val downPoint = FloatPoint(down.position.x, down.position.y)
                    val startImagePoint = CropGeometry.viewToImage(downPoint, transform)
                    val viewSelection = CropGeometry.imageToView(currentSelection, transform)
                    var handle = CropGeometry.hitTest(viewSelection, downPoint, handleRadiusPx)
                    val downInsideImage = transform.displayedBounds.contains(downPoint)
                    var previous = down.position
                    var totalDistance = 0f
                    var transformed = false
                    var dragged = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (pressed.size >= 2) {
                            transformed = true
                            currentViewportCallback(
                                CropGeometry.transformViewport(
                                    state = currentViewport,
                                    centroid = event.calculateCentroid().let { FloatPoint(it.x, it.y) },
                                    pan = event.calculatePan().let { FloatPoint(it.x, it.y) },
                                    zoomChange = event.calculateZoom(),
                                    image = imageSize,
                                    container = container,
                                ),
                            )
                            event.changes.forEach { it.consume() }
                            continue
                        }
                        if (transformed) continue
                        val change = pressed.first()
                        val delta = change.position - previous
                        previous = change.position
                        totalDistance += hypot(delta.x, delta.y)
                        if (totalDistance <= touchSlopPx) continue
                        dragged = true
                        transform = CropGeometry.viewport(imageSize, container, currentViewport)
                        val minImageSize = minCropSizePx / transform.scale
                        if (handle == CropHandle.NONE) {
                            if (!downInsideImage) continue
                            currentSelectionCallback(
                                CropGeometry.rectFromPoints(
                                    startImagePoint,
                                    CropGeometry.viewToImage(
                                        FloatPoint(change.position.x, change.position.y),
                                        transform,
                                    ),
                                    FloatRect(0f, 0f, imageSize.width, imageSize.height),
                                    minImageSize,
                                ),
                            )
                        } else {
                            currentSelectionCallback(
                                CropGeometry.drag(
                                    currentSelection,
                                    handle,
                                    FloatPoint(delta.x / transform.scale, delta.y / transform.scale),
                                    FloatRect(0f, 0f, imageSize.width, imageSize.height),
                                    minImageSize,
                                ),
                            )
                        }
                        change.consume()
                    }
                    if (!dragged && !transformed) {
                        transform = CropGeometry.viewport(imageSize, container, currentViewport)
                        currentLines
                            .filter { CropGeometry.imageToView(it.bounds, transform).contains(downPoint) }
                            .minByOrNull { it.bounds.width * it.bounds.height }
                            ?.let(currentLineCallback)
                    }
                    handle = CropHandle.NONE
                }
            },
    ) {
        if (canvasSize == IntSize.Zero) return@Canvas
        val transform = CropGeometry.viewport(imageSize, FloatSize(size.width, size.height), viewport)
        val rect = CropGeometry.imageToView(selection, transform)
        drawImage(
            image = image,
            dstOffset = IntOffset(transform.offsetX.roundToInt(), transform.offsetY.roundToInt()),
            dstSize = IntSize(
                transform.displayedWidth.roundToInt(),
                transform.displayedHeight.roundToInt(),
            ),
            filterQuality = FilterQuality.High,
        )

        // Dimmed scrim with a rounded "hole" for the selection (CtS style).
        val selectionRadius = minOf(24.dp.toPx(), rect.width / 2f, rect.height / 2f)
            .coerceAtLeast(0f)
        val scrimPath = Path().apply {
            addRect(Rect(Offset.Zero, size))
            addRoundRect(
                RoundRect(
                    left = rect.left,
                    top = rect.top,
                    right = rect.right,
                    bottom = rect.bottom,
                    cornerRadius = CornerRadius(selectionRadius, selectionRadius),
                ),
            )
            fillType = PathFillType.EvenOdd
        }
        drawPath(scrimPath, color = scrimColor)

        // Scanning shimmer sweeping over the dimmed capture while OCR runs.
        if (status == TextRecognitionStatus.FINDING) {
            val bandHeight = size.height * 0.22f
            val bandTop = -bandHeight + (size.height + bandHeight * 2f) * shimmerProgress
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.16f),
                        Color.Transparent,
                    ),
                    startY = bandTop,
                    endY = bandTop + bandHeight,
                ),
            )
        }

        // Recognized text lines highlighted as translucent pills.
        if (linesAlpha > 0f) {
            lines.forEach { line ->
                val lineRect = CropGeometry.imageToView(line.bounds, transform)
                val lineRadius = minOf(lineRect.height / 2f, 20.dp.toPx(), lineRect.width / 2f)
                    .coerceAtLeast(0f)
                val corner = CornerRadius(lineRadius, lineRadius)
                drawRoundRect(
                    color = Color.White,
                    alpha = 0.16f * linesAlpha,
                    topLeft = Offset(lineRect.left, lineRect.top),
                    size = Size(lineRect.width, lineRect.height),
                    cornerRadius = corner,
                )
                drawRoundRect(
                    color = Color.White,
                    alpha = 0.35f * linesAlpha,
                    topLeft = Offset(lineRect.left, lineRect.top),
                    size = Size(lineRect.width, lineRect.height),
                    cornerRadius = corner,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }

        // Selection border: soft gradient glow + animated Gemini gradient stroke.
        val slideStart = -400f + 800f * gradientSlide
        val borderBrush = Brush.linearGradient(
            colors = gradientColors,
            start = Offset(slideStart, slideStart),
            end = Offset(slideStart + 400f, slideStart + 400f),
        )
        val selectionCorner = CornerRadius(selectionRadius, selectionRadius)
        drawRoundRect(
            brush = borderBrush,
            alpha = 0.30f,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = selectionCorner,
            style = Stroke(width = 9.dp.toPx()),
        )
        drawRoundRect(
            brush = borderBrush,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = selectionCorner,
            style = Stroke(width = 3.dp.toPx()),
        )
        listOf(
            Offset(rect.left, rect.top),
            Offset(rect.right, rect.top),
            Offset(rect.left, rect.bottom),
            Offset(rect.right, rect.bottom),
        ).forEach { corner ->
            drawCircle(Color.White, radius = 7.dp.toPx(), center = corner)
            drawCircle(borderBrush, radius = 4.5.dp.toPx(), center = corner)
        }
    }
}

@Composable
internal fun LensUnavailableDialog(
    onDismiss: () -> Unit,
    onOpenGoogleStore: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lens_unavailable_title)) },
        text = { Text(stringResource(R.string.lens_unavailable_message)) },
        confirmButton = {
            TextButton(onClick = onOpenGoogleStore) {
                Text(stringResource(R.string.open_google_app_store))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun BrowserFallbackDialog(
    kind: ExternalActionKind,
    onDismiss: () -> Unit,
    onOpenBrowser: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (kind == ExternalActionKind.TRANSLATE) {
                        R.string.translate_in_browser_title
                    } else {
                        R.string.search_in_browser_title
                    },
                ),
            )
        },
        text = { Text(stringResource(R.string.browser_handoff_message)) },
        confirmButton = {
            TextButton(onClick = onOpenBrowser) { Text(stringResource(R.string.open_browser)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun ExternalUnavailableDialog(kind: ExternalActionKind, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.external_action_unavailable_title)) },
        text = {
            Text(
                stringResource(
                    if (kind == ExternalActionKind.TRANSLATE) {
                        R.string.translate_unavailable_message
                    } else {
                        R.string.search_unavailable_message
                    },
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
        },
    )
}

@Composable
internal fun CaptureProblem(
    title: String,
    message: String,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                shape = CircleShape,
                modifier = Modifier.height(48.dp),
            ) {
                Text(stringResource(android.R.string.cancel))
            }
            Button(
                onClick = onRetake,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(),
                modifier = Modifier.height(48.dp),
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(geminiGradientColors()),
                            CircleShape,
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Text(stringResource(R.string.retake))
                }
            }
        }
    }
}

@Composable
private fun captureFailureMessage(reason: CaptureFailureReason): String = when (reason) {
    CaptureFailureReason.INVALID_PERMISSION_RESULT -> stringResource(R.string.capture_permission_invalid)
    CaptureFailureReason.SERVICE_START_FAILED -> stringResource(R.string.capture_service_failed)
    CaptureFailureReason.PROJECTION_STOPPED -> stringResource(R.string.capture_projection_stopped)
    CaptureFailureReason.TIMED_OUT -> stringResource(R.string.capture_timed_out)
    CaptureFailureReason.EMPTY_IMAGE -> stringResource(R.string.capture_empty_image)
    CaptureFailureReason.WRITE_FAILED -> stringResource(R.string.capture_write_failed)
    CaptureFailureReason.CONSENT_EXPIRED -> stringResource(R.string.capture_consent_expired)
    CaptureFailureReason.UNKNOWN -> stringResource(R.string.capture_unknown_error)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiChatSheet(
    state: CropEditorUiState,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit,
    onRetry: () -> Unit,
    onCopyMessage: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.aiConversation.messages.size, state.aiConversation.isLoading) {
        val count = state.aiConversation.messages.size + (if (state.aiConversation.isLoading) 1 else 0)
        if (count > 0) {
            listState.animateScrollToItem(count - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GradientBadge(size = 28.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.ai_chat_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            HorizontalDivider()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.aiConversation.messages) { message ->
                    AiMessageBubble(message, onCopyMessage)
                }

                if (state.aiConversation.isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.finding_text),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                state.aiConversation.error?.let { errorText ->
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = errorText,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = onRetry,
                                    modifier = Modifier.align(Alignment.End),
                                ) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.ai_retry))
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(stringResource(R.string.ai_chat_placeholder)) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    maxLines = 4,
                    enabled = !state.aiConversation.isLoading,
                )
                IconButton(
                    onClick = {
                        val textToSend = inputText.trim()
                        if (textToSend.isNotBlank()) {
                            inputText = ""
                            onSendMessage(textToSend)
                        }
                    },
                    enabled = inputText.isNotBlank() && !state.aiConversation.isLoading,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.ai_send),
                        tint = if (inputText.isNotBlank() && !state.aiConversation.isLoading) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: AiChatMessage,
    onCopyMessage: (String) -> Unit,
) {
    val isUser = message.role == AiMessageRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val displayText = if (isUser && message.text.startsWith("Recognized text from screenshot:\n---")) {
        val parts = message.text.split("\n---\n")
        if (parts.size >= 3) {
            parts.last().trim()
        } else {
            message.text
        }
    } else {
        message.text
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Surface(
            color = containerColor,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                SelectionContainer {
                    Text(
                        text = displayText,
                        color = contentColor,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (!isUser) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(
                            onClick = { onCopyMessage(displayText) },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.ai_copy_message),
                                modifier = Modifier.size(16.dp),
                                tint = contentColor.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
    }
}
