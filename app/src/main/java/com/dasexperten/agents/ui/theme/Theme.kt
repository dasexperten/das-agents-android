package com.dasexperten.agents.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrandGreen = Color(0xFF1A5F4A)
private val BrandGold = Color(0xFFC9A227)
private val BrandInk = Color(0xFF111111)

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    secondary = BrandGold,
    onSecondary = BrandInk,
    background = Color(0xFFF7F7F5),
    onBackground = BrandInk,
    surface = Color.White,
    onSurface = BrandInk,
    surfaceVariant = Color(0xFFECEAE4),
    onSurfaceVariant = Color(0xFF444444),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FBF9E),
    onPrimary = Color(0xFF003824),
    secondary = BrandGold,
    onSecondary = BrandInk,
    background = Color(0xFF121212),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF1C1C1C),
    onSurface = Color(0xFFF2F2F2),
)

@Composable
fun DasAgentsTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
