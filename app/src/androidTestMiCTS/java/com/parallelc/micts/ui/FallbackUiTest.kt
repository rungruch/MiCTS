package com.parallelc.micts.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parallelc.micts.ui.activity.CaptureProblem
import com.parallelc.micts.ui.activity.CaptureSetupScreen
import com.parallelc.micts.ui.activity.ConsentExplanationScreen
import com.parallelc.micts.ui.activity.LensUnavailableDialog
import com.parallelc.micts.ui.activity.NativeConfirmationDialog
import com.parallelc.micts.ui.theme.MiCTSTheme
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

        composeRule.onNodeWithText("Did native Circle to Search appear?").assertIsDisplayed()
        composeRule.onNodeWithText("Yes, keep native").performClick()
        assertTrue(nativeSelected)

        composeRule.onNodeWithText("No, use Lens fallback").performClick()
        assertTrue(lensSelected)
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
    fun captureSetupExplainsProcessScopedApprovalAndOffersChoices() {
        var approveOnceSelected = false
        var askEveryTimeSelected = false
        composeRule.setContent {
            MiCTSTheme {
                CaptureSetupScreen(
                    onApproveOnce = { approveOnceSelected = true },
                    onAskEveryTime = { askEveryTimeSelected = true },
                    onCancel = {},
                )
            }
        }

        composeRule.onNodeWithText("Set up screen capture").assertIsDisplayed()
        composeRule.onNodeWithText("Approve once").performClick()
        assertTrue(approveOnceSelected)

        composeRule.onNodeWithText("Ask every time instead").performClick()
        assertTrue(askEveryTimeSelected)
    }

    @Test
    fun consentExplanationExplainsPerTriggerConsentOnAndroid14() {
        var continued = false
        composeRule.setContent {
            MiCTSTheme {
                ConsentExplanationScreen(
                    onContinue = { continued = true },
                    onCancel = {},
                )
            }
        }

        composeRule.onNodeWithText("Screen capture approval").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()
        assertTrue(continued)
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
}
