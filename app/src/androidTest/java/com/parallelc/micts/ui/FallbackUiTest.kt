package com.parallelc.micts.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parallelc.micts.domain.FloatRect
import com.parallelc.micts.domain.ViewportState
import com.parallelc.micts.ui.activity.CaptureProblem
import com.parallelc.micts.ui.activity.CropScreen
import com.parallelc.micts.ui.activity.LensUnavailableDialog
import com.parallelc.micts.ui.activity.NativeConfirmationDialog
import com.parallelc.micts.ui.theme.MiCTSTheme
import com.parallelc.micts.ui.viewmodel.CaptureContentState
import com.parallelc.micts.ui.viewmodel.CropEditorUiState
import com.parallelc.micts.ui.viewmodel.TextRecognitionStatus
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FallbackUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nativeConfirmationExposesBothDecisions() {
        var nativeSelected = false
        var lensSelected = false
        composeRule.setContent {
            MiCTSTheme {
                NativeConfirmationDialog(
                    onDismiss = {},
                    onNativeWorked = { nativeSelected = true },
                    onUseLensFallback = { lensSelected = true },
                )
            }
        }

        composeRule.onNodeWithText("Did native Circle to Search appear?")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Yes, keep native").performClick()
        assertTrue(nativeSelected)

        composeRule.setContent {
            MiCTSTheme {
                NativeConfirmationDialog(
                    onDismiss = {},
                    onNativeWorked = {},
                    onUseLensFallback = { lensSelected = true },
                )
            }
        }
        composeRule.onNodeWithText("No, use Lens fallback").performClick()
        assertTrue(lensSelected)
    }

    @Test
    fun captureProblemOffersRetakeAndCancel() {
        composeRule.setContent {
            MiCTSTheme {
                CaptureProblem(
                    title = "Screen capture failed",
                    message = "Test failure",
                    onRetake = {},
                    onCancel = {},
                )
            }
        }

        composeRule.onNodeWithText("Screen capture failed").assertIsDisplayed()
        composeRule.onNodeWithText("Retake").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun captureDenialOffersAnotherConsentRequest() {
        var retakeSelected = false
        composeRule.setContent {
            MiCTSTheme {
                CaptureProblem(
                    title = "Screen capture permission needed",
                    message = "Screen capture permission was not granted.",
                    onRetake = { retakeSelected = true },
                    onCancel = {},
                )
            }
        }

        composeRule.onNodeWithText("Screen capture permission needed").assertIsDisplayed()
        composeRule.onNodeWithText("Retake").performClick()
        assertTrue(retakeSelected)
    }

    @Test
    fun protectedContentExplainsSecureCapture() {
        composeRule.setContent {
            MiCTSTheme {
                CaptureProblem(
                    title = "This screen could not be captured",
                    message = "The visible app may protect its content.",
                    onRetake = {},
                    onCancel = {},
                )
            }
        }

        composeRule.onNodeWithText("This screen could not be captured").assertIsDisplayed()
        composeRule.onNodeWithText("The visible app may protect its content.").assertIsDisplayed()
    }

    @Test
    fun lensUnavailableKeepsUserInFallbackFlow() {
        var storeSelected = false
        composeRule.setContent {
            MiCTSTheme {
                LensUnavailableDialog(
                    onDismiss = {},
                    onOpenGoogleStore = { storeSelected = true },
                )
            }
        }

        composeRule.onNodeWithText("Google Lens is unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Open Google app page").performClick()
        assertTrue(storeSelected)
    }

    @Test
    fun cropScreenRoutesSelectedRegionToFakeLensGateway() {
        val bitmap = Bitmap.createBitmap(200, 400, Bitmap.Config.ARGB_8888)
        var lensSelected = false
        composeRule.setContent {
            MiCTSTheme {
                CropScreen(
                    state = readyState(bitmap),
                    bitmap = bitmap,
                    onSelectionChanged = {},
                    onViewportChanged = {},
                    onLineTapped = {},
                    onRetryRecognition = {},
                    onCopy = {},
                    onSearch = {},
                    onTranslate = {},
                    onLens = { lensSelected = true },
                )
            }
        }

        composeRule.onNodeWithText("Lens").assertIsEnabled().performClick()
        assertTrue(lensSelected)
    }

    @Test
    fun textActionsEnableOnlyWhenOcrTextIsSelected() {
        val bitmap = Bitmap.createBitmap(200, 400, Bitmap.Config.ARGB_8888)
        composeRule.setContent {
            MiCTSTheme {
                CropScreen(
                    state = readyState(bitmap),
                    bitmap = bitmap,
                    onSelectionChanged = {},
                    onViewportChanged = {},
                    onLineTapped = {},
                    onRetryRecognition = {},
                    onCopy = {},
                    onSearch = {},
                    onTranslate = {},
                    onLens = {},
                )
            }
        }
        composeRule.onNodeWithText("Copy").assertIsNotEnabled()
        composeRule.onNodeWithText("Lens").assertIsEnabled()

        composeRule.setContent {
            MiCTSTheme {
                CropScreen(
                    state = readyState(bitmap).copy(selectedText = "Huawei MatePad"),
                    bitmap = bitmap,
                    onSelectionChanged = {},
                    onViewportChanged = {},
                    onLineTapped = {},
                    onRetryRecognition = {},
                    onCopy = {},
                    onSearch = {},
                    onTranslate = {},
                    onLens = {},
                )
            }
        }
        composeRule.onNodeWithText("Huawei MatePad").assertIsDisplayed()
        composeRule.onNodeWithText("Copy").assertIsEnabled()
        composeRule.onNodeWithText("Search").assertIsEnabled()
        composeRule.onNodeWithText("Translate").assertIsEnabled()
    }

    @Test
    fun recognitionFailureOffersRetryWithoutDisablingLens() {
        val bitmap = Bitmap.createBitmap(200, 400, Bitmap.Config.ARGB_8888)
        var retried = false
        composeRule.setContent {
            MiCTSTheme {
                CropScreen(
                    state = readyState(bitmap).copy(
                        recognitionStatus = TextRecognitionStatus.FAILED,
                    ),
                    bitmap = bitmap,
                    onSelectionChanged = {},
                    onViewportChanged = {},
                    onLineTapped = {},
                    onRetryRecognition = { retried = true },
                    onCopy = {},
                    onSearch = {},
                    onTranslate = {},
                    onLens = {},
                )
            }
        }
        composeRule.onNodeWithText("Text recognition failed — tap to retry").performClick()
        assertTrue(retried)
        composeRule.onNodeWithText("Lens").assertIsEnabled()
    }

    private fun readyState(bitmap: Bitmap) = CropEditorUiState(
        content = CaptureContentState.Ready(bitmap),
        selection = FloatRect(20f, 40f, 180f, 360f),
        viewport = ViewportState(),
        recognitionStatus = TextRecognitionStatus.DISABLED,
    )
}
