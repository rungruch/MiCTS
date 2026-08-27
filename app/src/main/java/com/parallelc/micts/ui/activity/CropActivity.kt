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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var overflowExpanded by remember { mutableStateOf(false) }
    var showAiChat by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.smart_search_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, stringResource(R.string.close))
                    }
                },
                actions = {
                    IconButton(onClick = onRetake, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.retake))
                    }
                    Box {
                        IconButton(
                            onClick = { overflowExpanded = true },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(Icons.Default.MoreVert, stringResource(R.string.more_actions))
                        }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.search_full_screen_lens)) },
                                leadingIcon = { Icon(Icons.Default.ImageSearch, null) },
                                enabled = state.content is CaptureContentState.Ready && !state.isActing,
                                onClick = {
                                    overflowExpanded = false
                                    onFullScreenLens()
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val content = state.content) {
            CaptureContentState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            CaptureContentState.Protected -> CaptureProblem(
                title = stringResource(R.string.protected_capture_title),
                message = stringResource(R.string.protected_capture_message),
                onRetake = onRetake,
                onCancel = onCancel,
                modifier = Modifier.padding(padding),
            )
            is CaptureContentState.Error -> CaptureProblem(
                title = stringResource(R.string.capture_failed_title),
                message = captureFailureMessage(content.reason),
                onRetake = onRetake,
                onCancel = onCancel,
                modifier = Modifier.padding(padding),
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
                modifier = Modifier.padding(padding),
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
    onAskAi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                EditorContent(
                    state, bitmap, onSelectionChanged, onViewportChanged, onLineTapped,
                    onRetryRecognition, Modifier.weight(1f).fillMaxHeight(),
                )
                EditorActions(
                    state.selectedText, state.isActing, state.aiConfigured, true, onCopy, onSearch, onTranslate,
                    onLens, onAskAi, Modifier.width(132.dp).fillMaxHeight(),
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                EditorContent(
                    state, bitmap, onSelectionChanged, onViewportChanged, onLineTapped,
                    onRetryRecognition, Modifier.weight(1f).fillMaxWidth(),
                )
                EditorActions(
                    state.selectedText, state.isActing, state.aiConfigured, false, onCopy, onSearch, onTranslate,
                    onLens, onAskAi, Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun EditorContent(
    state: CropEditorUiState,
    bitmap: Bitmap,
    onSelectionChanged: (FloatRect) -> Unit,
    onViewportChanged: (ViewportState) -> Unit,
    onLineTapped: (RecognizedTextLine) -> Unit,
    onRetryRecognition: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier) {
        RecognitionStatusChip(state.recognitionStatus, onRetryRecognition)
        CropEditor(
            bitmap = bitmap,
            selection = state.selection ?: return@Column,
            viewport = state.viewport,
            lines = state.recognizedLines,
            onSelectionChanged = onSelectionChanged,
            onViewportChanged = onViewportChanged,
            onLineTapped = onLineTapped,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = state.selectedText.ifBlank {
                    stringResource(R.string.select_text_or_image_hint)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.selectedText.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RecognitionStatusChip(status: TextRecognitionStatus, onRetry: () -> Unit) {
    when (status) {
        TextRecognitionStatus.DISABLED -> Unit
        TextRecognitionStatus.FINDING -> AssistChip(
            onClick = {},
            enabled = false,
            label = { Text(stringResource(R.string.finding_text)) },
            leadingIcon = { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
        TextRecognitionStatus.EMPTY -> AssistChip(
            onClick = onRetry,
            label = { Text(stringResource(R.string.no_text_found)) },
            leadingIcon = { Icon(Icons.Default.Refresh, null) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
        TextRecognitionStatus.FAILED -> AssistChip(
            onClick = onRetry,
            label = { Text(stringResource(R.string.text_recognition_retry)) },
            leadingIcon = { Icon(Icons.Default.Refresh, null) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
        TextRecognitionStatus.READY -> Unit
    }
}

@Composable
private fun EditorActions(
    selectedText: String,
    isActing: Boolean,
    aiVisible: Boolean,
    vertical: Boolean,
    onCopy: () -> Unit,
    onSearch: () -> Unit,
    onTranslate: () -> Unit,
    onLens: () -> Unit,
    onAskAi: () -> Unit,
    modifier: Modifier,
) {
    val availability = EditorActionPolicy.availability(selectedText, isActing)
    if (vertical) {
        Column(
            modifier = modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            EditorAction(
                stringResource(R.string.copy_text), { Icon(Icons.Default.ContentCopy, null) },
                availability.copy, onCopy, Modifier.fillMaxWidth(),
            )
            EditorAction(
                stringResource(R.string.search_text), { Icon(Icons.Default.Search, null) },
                availability.search, onSearch, Modifier.fillMaxWidth(),
            )
            EditorAction(
                stringResource(R.string.translate_text), { Icon(Icons.Default.Translate, null) },
                availability.translate, onTranslate, Modifier.fillMaxWidth(),
            )
            if (aiVisible) {
                EditorAction(
                    stringResource(R.string.ask_ai), { Icon(Icons.Default.AutoAwesome, null) },
                    !isActing, onAskAi, Modifier.fillMaxWidth(),
                )
            }
            LensButton(availability.lens, isActing, onLens, Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditorAction(
                stringResource(R.string.copy_text), { Icon(Icons.Default.ContentCopy, null) },
                availability.copy, onCopy, Modifier.weight(1f),
            )
            EditorAction(
                stringResource(R.string.search_text), { Icon(Icons.Default.Search, null) },
                availability.search, onSearch, Modifier.weight(1f),
            )
            EditorAction(
                stringResource(R.string.translate_text), { Icon(Icons.Default.Translate, null) },
                availability.translate, onTranslate, Modifier.weight(1.1f),
            )
            if (aiVisible) {
                EditorAction(
                    stringResource(R.string.ask_ai), { Icon(Icons.Default.AutoAwesome, null) },
                    !isActing, onAskAi, Modifier.weight(1.1f),
                )
            }
            LensButton(availability.lens, isActing, onLens, Modifier.weight(1.3f))
        }
    }
}

@Composable
private fun LensButton(
    enabled: Boolean,
    isActing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.height(56.dp)) {
        if (isActing) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.ImageSearch, null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.lens))
        }
    }
}

@Composable
private fun EditorAction(
    label: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.height(56.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun CropEditor(
    bitmap: Bitmap,
    selection: FloatRect,
    viewport: ViewportState,
    lines: List<RecognizedTextLine>,
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
    val borderColor = MaterialTheme.colorScheme.primary
    val editorDescription = stringResource(R.string.crop_editor_description)
    val density = LocalDensity.current
    val handleRadiusPx = with(density) { 28.dp.toPx() }
    val minCropSizePx = with(density) { 48.dp.toPx() }
    val touchSlopPx = with(density) { 8.dp.toPx() }
    val imageSize = FloatSize(bitmap.width.toFloat(), bitmap.height.toFloat())

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
        lines.forEach { line ->
            val lineRect = CropGeometry.imageToView(line.bounds, transform)
            drawRect(
                color = borderColor.copy(alpha = 0.12f),
                topLeft = Offset(lineRect.left, lineRect.top),
                size = Size(lineRect.width, lineRect.height),
            )
            drawRect(
                color = borderColor.copy(alpha = 0.8f),
                topLeft = Offset(lineRect.left, lineRect.top),
                size = Size(lineRect.width, lineRect.height),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        val scrim = Color.Black.copy(alpha = 0.58f)
        drawRect(scrim, Offset.Zero, Size(size.width, rect.top.coerceAtLeast(0f)))
        drawRect(
            scrim,
            Offset(0f, rect.bottom),
            Size(size.width, (size.height - rect.bottom).coerceAtLeast(0f)),
        )
        drawRect(scrim, Offset(0f, rect.top), Size(rect.left.coerceAtLeast(0f), rect.height))
        drawRect(
            scrim,
            Offset(rect.right, rect.top),
            Size((size.width - rect.right).coerceAtLeast(0f), rect.height),
        )
        drawRect(
            color = borderColor,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            style = Stroke(width = 3.dp.toPx()),
        )
        listOf(
            Offset(rect.left, rect.top),
            Offset(rect.right, rect.top),
            Offset(rect.left, rect.bottom),
            Offset(rect.right, rect.bottom),
        ).forEach { corner ->
            drawCircle(Color.White, radius = 8.dp.toPx(), center = corner)
            drawCircle(borderColor, radius = 5.dp.toPx(), center = corner)
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
            OutlinedButton(onClick = onCancel, modifier = Modifier.height(48.dp)) {
                Text(stringResource(android.R.string.cancel))
            }
            Button(onClick = onRetake, modifier = Modifier.height(48.dp)) {
                Text(stringResource(R.string.retake))
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
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
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
