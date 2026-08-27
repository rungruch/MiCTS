package com.parallelc.micts.ui.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.parallelc.micts.R
import com.parallelc.micts.data.CaptureFiles
import com.parallelc.micts.data.LensShareGateway
import com.parallelc.micts.domain.CaptureFailureReason
import com.parallelc.micts.domain.CropGeometry
import com.parallelc.micts.domain.CropHandle
import com.parallelc.micts.domain.FitCenterTransform
import com.parallelc.micts.domain.FloatPoint
import com.parallelc.micts.domain.FloatRect
import com.parallelc.micts.domain.FloatSize
import com.parallelc.micts.domain.IntCropRect
import com.parallelc.micts.ui.theme.MiCTSTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import kotlin.math.roundToInt

class CropActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_PROBABLY_PROTECTED = "probably_protected"
        private const val EXTRA_FAILURE_REASON = "failure_reason"

        fun createIntent(
            context: android.content.Context,
            probablyProtected: Boolean,
            failureReason: CaptureFailureReason?,
        ): Intent = Intent(context, CropActivity::class.java).apply {
            putExtra(EXTRA_PROBABLY_PROTECTED, probablyProtected)
            failureReason?.let { putExtra(EXTRA_FAILURE_REASON, it.name) }
        }
    }

    private var uiState by mutableStateOf<CropUiState>(CropUiState.Loading)
    private var lensUnavailable by mutableStateOf(false)
    private var isSearching by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()

        val failureReason = intent.getStringExtra(EXTRA_FAILURE_REASON)
            ?.let { stored -> CaptureFailureReason.entries.firstOrNull { it.name == stored } }
        val probablyProtected = intent.getBooleanExtra(EXTRA_PROBABLY_PROTECTED, false)
        uiState = when {
            failureReason != null -> CropUiState.Error(failureReason)
            probablyProtected -> CropUiState.Protected
            else -> CropUiState.Loading
        }

        setContent {
            MiCTSTheme {
                CropRoute(
                    state = uiState,
                    isSearching = isSearching,
                    lensUnavailable = lensUnavailable,
                    onSearch = ::searchWithLens,
                    onRetake = ::retake,
                    onCancel = ::finish,
                    onDismissLensUnavailable = { lensUnavailable = false },
                    onOpenGoogleStore = ::openGoogleStore,
                )
            }
        }

        if (uiState == CropUiState.Loading) loadCapture()
    }

    private fun loadCapture() {
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(CaptureFiles.capture(this@CropActivity).absolutePath)
            }
            uiState = if (bitmap == null) {
                CropUiState.Error(CaptureFailureReason.EMPTY_IMAGE)
            } else {
                CropUiState.Ready(bitmap)
            }
        }
    }

    private fun searchWithLens(crop: IntCropRect) {
        val bitmap = (uiState as? CropUiState.Ready)?.bitmap ?: return
        if (isSearching) return
        isSearching = true
        lifecycleScope.launch {
            val croppedFile = withContext(Dispatchers.IO) {
                runCatching {
                    val croppedBitmap = Bitmap.createBitmap(
                        bitmap,
                        crop.left,
                        crop.top,
                        crop.width,
                        crop.height,
                    )
                    // Overwrite the full frame after the crop is safely held in memory. This keeps
                    // exactly one cached PNG throughout the fallback flow.
                    val output = CaptureFiles.capture(this@CropActivity)
                    val written = FileOutputStream(output).use { stream ->
                        croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    croppedBitmap.recycle()
                    if (!written) error("Could not encode cropped capture")
                    output
                }.getOrNull()
            }
            isSearching = false
            if (croppedFile == null) {
                uiState = CropUiState.Error(CaptureFailureReason.WRITE_FAILED)
                return@launch
            }
            if (LensShareGateway(this@CropActivity).share(croppedFile)) {
                finish()
            } else {
                lensUnavailable = true
            }
        }
    }

    private fun retake() {
        startActivity(MainActivity.createLensFallbackIntent(this))
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

private sealed interface CropUiState {
    data object Loading : CropUiState
    data class Ready(val bitmap: Bitmap) : CropUiState
    data object Protected : CropUiState
    data class Error(val reason: CaptureFailureReason) : CropUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CropRoute(
    state: CropUiState,
    isSearching: Boolean,
    lensUnavailable: Boolean,
    onSearch: (IntCropRect) -> Unit,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
    onDismissLensUnavailable: () -> Unit,
    onOpenGoogleStore: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.crop_title)) }) },
    ) { padding ->
        when (state) {
            CropUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            CropUiState.Protected -> CaptureProblem(
                title = stringResource(R.string.protected_capture_title),
                message = stringResource(R.string.protected_capture_message),
                onRetake = onRetake,
                onCancel = onCancel,
                modifier = Modifier.padding(padding),
            )
            is CropUiState.Error -> CaptureProblem(
                title = stringResource(R.string.capture_failed_title),
                message = captureFailureMessage(state.reason),
                onRetake = onRetake,
                onCancel = onCancel,
                modifier = Modifier.padding(padding),
            )
            is CropUiState.Ready -> CropScreen(
                bitmap = state.bitmap,
                isSearching = isSearching,
                onSearch = onSearch,
                onRetake = onRetake,
                onCancel = onCancel,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (lensUnavailable) {
        LensUnavailableDialog(
            onDismiss = onDismissLensUnavailable,
            onOpenGoogleStore = onOpenGoogleStore,
        )
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
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
internal fun CropScreen(
    bitmap: Bitmap,
    isSearching: Boolean,
    onSearch: (IntCropRect) -> Unit,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var cropSelection by remember(bitmap) { mutableStateOf<CropSelection?>(null) }
    Column(modifier = modifier.fillMaxSize()) {
        CropEditor(
            bitmap = bitmap,
            onSelectionChanged = { cropSelection = it },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.height(48.dp)) {
                Text(stringResource(android.R.string.cancel))
            }
            OutlinedButton(onClick = onRetake, modifier = Modifier.height(48.dp)) {
                Text(stringResource(R.string.retake))
            }
            Button(
                onClick = { cropSelection?.imageRect?.let(onSearch) },
                enabled = cropSelection != null && !isSearching,
                modifier = Modifier.height(48.dp),
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.search_with_lens))
                }
            }
        }
    }
}

private data class CropSelection(
    val viewRect: FloatRect,
    val imageRect: IntCropRect,
)

@Composable
private fun CropEditor(
    bitmap: Bitmap,
    onSelectionChanged: (CropSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var transform by remember { mutableStateOf<FitCenterTransform?>(null) }
    var cropRect by remember { mutableStateOf<FloatRect?>(null) }
    var activeHandle by remember { mutableStateOf(CropHandle.NONE) }
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val borderColor = MaterialTheme.colorScheme.primary
    val editorDescription = stringResource(R.string.crop_editor_description)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val handleRadiusPx = with(density) { 28.dp.toPx() }
    val minCropSizePx = with(density) { 96.dp.toPx() }

    LaunchedEffect(canvasSize, bitmap) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return@LaunchedEffect
        val fitted = CropGeometry.fitCenter(
            FloatSize(bitmap.width.toFloat(), bitmap.height.toFloat()),
            FloatSize(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
        )
        transform = fitted
        val initial = CropGeometry.initialCrop(fitted.displayedBounds)
        cropRect = initial
        onSelectionChanged(
            CropSelection(
                initial,
                CropGeometry.toImageRect(initial, fitted, bitmap.width, bitmap.height),
            ),
        )
    }

    Canvas(
        modifier = modifier
            .semantics { contentDescription = editorDescription }
            .onSizeChanged { canvasSize = it }
            .pointerInput(transform) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val rect = cropRect ?: return@detectDragGestures
                        activeHandle = CropGeometry.hitTest(
                            rect,
                            FloatPoint(offset.x, offset.y),
                            handleRadiusPx,
                        )
                    },
                    onDragEnd = { activeHandle = CropHandle.NONE },
                    onDragCancel = { activeHandle = CropHandle.NONE },
                    onDrag = { change, dragAmount ->
                        val fitted = transform ?: return@detectDragGestures
                        val current = cropRect ?: return@detectDragGestures
                        if (activeHandle == CropHandle.NONE) return@detectDragGestures
                        change.consume()
                        val updated = CropGeometry.drag(
                            current,
                            activeHandle,
                            FloatPoint(dragAmount.x, dragAmount.y),
                            fitted.displayedBounds,
                            minCropSizePx,
                        )
                        cropRect = updated
                        onSelectionChanged(
                            CropSelection(
                                updated,
                                CropGeometry.toImageRect(
                                    updated,
                                    fitted,
                                    bitmap.width,
                                    bitmap.height,
                                ),
                            ),
                        )
                    },
                )
            },
    ) {
        val fitted = transform ?: return@Canvas
        val rect = cropRect ?: return@Canvas
        drawImage(
            image = image,
            dstOffset = IntOffset(fitted.offsetX.roundToInt(), fitted.offsetY.roundToInt()),
            dstSize = IntSize(
                fitted.displayedWidth.roundToInt(),
                fitted.displayedHeight.roundToInt(),
            ),
            filterQuality = FilterQuality.High,
        )

        val scrim = Color.Black.copy(alpha = 0.58f)
        drawRect(scrim, Offset.Zero, Size(size.width, rect.top.coerceAtLeast(0f)))
        drawRect(
            scrim,
            Offset(0f, rect.bottom),
            Size(size.width, (size.height - rect.bottom).coerceAtLeast(0f)),
        )
        drawRect(
            scrim,
            Offset(0f, rect.top),
            Size(rect.left.coerceAtLeast(0f), rect.height),
        )
        drawRect(
            scrim,
            Offset(rect.right, rect.top),
            Size((size.width - rect.right).coerceAtLeast(0f), rect.height),
        )
        drawRect(
            color = borderColor,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()),
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
private fun captureFailureMessage(reason: CaptureFailureReason): String = when (reason) {
    CaptureFailureReason.INVALID_PERMISSION_RESULT -> stringResource(R.string.capture_permission_invalid)
    CaptureFailureReason.SERVICE_START_FAILED -> stringResource(R.string.capture_service_failed)
    CaptureFailureReason.PROJECTION_STOPPED -> stringResource(R.string.capture_projection_stopped)
    CaptureFailureReason.TIMED_OUT -> stringResource(R.string.capture_timed_out)
    CaptureFailureReason.EMPTY_IMAGE -> stringResource(R.string.capture_empty_image)
    CaptureFailureReason.WRITE_FAILED -> stringResource(R.string.capture_write_failed)
    CaptureFailureReason.UNKNOWN -> stringResource(R.string.capture_unknown_error)
}
