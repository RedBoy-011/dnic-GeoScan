package com.denic.geoscan.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = DarkBase,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = NeonGreen,
    secondary = NeonCyan,
    onSecondary = DarkBase,
    secondaryContainer = DarkSurfaceHighlight,
    onSecondaryContainer = NeonCyan,
    tertiary = NeonRed,
    onTertiary = DarkBase,
    background = DarkBase,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = GlassSurfaceSubtle
)

@Composable
fun DenicGeoScanTheme(
    darkTheme: Boolean = true, // Default to dark aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            try {
                val window = (view.context as? Activity)?.window
                if (window != null) {
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = false
                    insetsController.isAppearanceLightNavigationBars = false
                }
            } catch (_: Throwable) {
                // Safely ignored on devices with custom or strict window decors
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
