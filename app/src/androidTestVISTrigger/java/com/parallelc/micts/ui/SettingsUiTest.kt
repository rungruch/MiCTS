package com.parallelc.micts.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.parallelc.micts.R
import com.parallelc.micts.ui.activity.SettingsActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<SettingsActivity>()

    @Test
    fun settingsExposeOnlyDirectVisPreferences() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithText(context.getString(R.string.default_trigger_delay))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.tile_trigger_delay))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.vibrate))
            .assertIsDisplayed()

        listOf(
            "Module Settings",
            "System trigger service",
            "Device spoof for Google",
            "Google Lens fallback",
            "Recognize text locally",
            "AI assistant",
        ).forEach { removedSetting ->
            composeRule.onAllNodesWithText(removedSetting).assertCountEquals(0)
        }
    }
}
