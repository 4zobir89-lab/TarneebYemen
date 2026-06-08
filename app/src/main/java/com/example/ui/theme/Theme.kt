package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TarneebColorScheme = darkColorScheme(
    primary = YemenGold,
    secondary = YemenSand,
    tertiary = YemenRed,
    background = TableGreen,
    surface = Color(0xCC0A1F0E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = YemenCream,
    onSurface = YemenCream,
    surfaceVariant = Color(0xCC1B5E20),
    onSurfaceVariant = YemenCream,
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TarneebColorScheme,
        typography = Typography,
        content = content
    )
}
