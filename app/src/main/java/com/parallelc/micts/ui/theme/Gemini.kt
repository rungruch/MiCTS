package com.parallelc.micts.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Gemini-style gradient palette (blue -> violet -> pink).
private val GeminiBlueLight = Color(0xFF4E8DF5)
private val GeminiVioletLight = Color(0xFF9B72F2)
private val GeminiPinkLight = Color(0xFFE06C9F)

private val GeminiBlueDark = Color(0xFF6EA8FF)
private val GeminiVioletDark = Color(0xFFB28DFF)
private val GeminiPinkDark = Color(0xFFF28BB5)

/** Signature Gemini gradient stops for the current theme. */
@Composable
fun geminiGradientColors(): List<Color> = if (isSystemInDarkTheme()) {
    listOf(GeminiBlueDark, GeminiVioletDark, GeminiPinkDark)
} else {
    listOf(GeminiBlueLight, GeminiVioletLight, GeminiPinkLight)
}

/**
 * Gemini gradient brush. [slide] in 0f..1f shifts the gradient window so the
 * colors appear to sweep along the element when animated.
 */
@Composable
fun geminiBrush(slide: Float = 0f): Brush {
    val colors = geminiGradientColors()
    val start = -400f + 800f * slide
    return Brush.linearGradient(
        colors = colors,
        start = Offset(start, start),
        end = Offset(start + 400f, start + 400f),
    )
}

/** Translucent "glass" container color that adapts to dark/light theme. */
@Composable
fun glassContainerColor(): Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)

/** Content color to use on top of [glassContainerColor]. */
@Composable
fun glassContentColor(): Color = MaterialTheme.colorScheme.onSurface

/** Subtle hairline border for glass surfaces. */
@Composable
fun glassBorderColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

/**
 * Scrim drawn over the captured screenshot. Like Circle to Search the captured
 * content is dimmed regardless of the app theme, slightly stronger in dark
 * theme.
 */
@Composable
fun editorScrimColor(): Color = Color.Black.copy(alpha = if (isSystemInDarkTheme()) 0.60f else 0.45f)
