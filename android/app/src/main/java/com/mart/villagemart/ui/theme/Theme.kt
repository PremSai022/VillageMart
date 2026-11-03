package com.mart.villagemart.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ForestDark,
    secondary = Mint,
    tertiary = Amber,
    background = Color(0xFF101A16),
    surface = Color(0xFF17231D),
    onSurface = InkDark
)

private val LightColorScheme = lightColorScheme(
    primary = Forest,
    secondary = Color(0xFF4E7566),
    tertiary = Amber,
    error = AlertRed,
    background = SurfaceWarm,
    surface = Color.White,
    onSurface = Ink
)

@Composable
fun VillageMartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}