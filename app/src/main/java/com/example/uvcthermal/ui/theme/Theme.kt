package com.example.uvcthermal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Ember,
    onPrimary = SurfaceNightAlt,
    primaryContainer = EmberSoft,
    onPrimaryContainer = SurfaceNightAlt,
    secondary = Mist,
    onSecondary = SurfaceNightAlt,
    tertiary = SuccessMint,
    background = DeepSea,
    onBackground = Bone,
    surface = SurfaceNight,
    onSurface = Bone,
    surfaceVariant = DeepSeaAlt,
    onSurfaceVariant = Mist
)

@Composable
fun UVCThermalTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
