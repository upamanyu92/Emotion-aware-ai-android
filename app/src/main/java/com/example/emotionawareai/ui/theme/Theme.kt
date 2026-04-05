package com.example.emotionawareai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = BackgroundDark,
    surface = SurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = BackgroundLight,
    surface = SurfaceLight
)

// ── NeoPOP / CRED-inspired deep dark scheme ──────────────────────────────────
private val NeoDarkColorScheme = darkColorScheme(
    primary              = NeonPurple,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFF2D1460),
    onPrimaryContainer   = NeonPurpleLight,
    secondary            = NeonCyan,
    onSecondary          = Color(0xFF003731),
    secondaryContainer   = Color(0xFF004D45),
    onSecondaryContainer = NeonCyan,
    tertiary             = NeonGold,
    onTertiary           = Color(0xFF3A2800),
    error                = NeonRose,
    background           = NeoBg1,
    onBackground         = Color(0xFFE2E8F0),
    surface              = NeoBg2,
    onSurface            = Color(0xFFCBD5E1),
    surfaceVariant       = NeoBg3,
    onSurfaceVariant     = Color(0xFF94A3B8),
    outline              = Color(0xFF334155),
    surfaceContainerHighest = NeoBg3
)

private val ProDarkColorScheme = darkColorScheme(
    primary = ProPurple,
    secondary = ProBlue,
    tertiary = ProAqua,
    background = ProBackgroundDark,
    surface = ProSurfaceDark
)

private val ProLightColorScheme = lightColorScheme(
    primary = ProPurple,
    secondary = ProBlue,
    tertiary = ProAqua,
    background = ProBackgroundLight,
    surface = ProSurfaceLight
)

@Composable
fun EmotionAwareAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    proThemeEnabled: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        proThemeEnabled && darkTheme -> ProDarkColorScheme
        proThemeEnabled             -> ProLightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> NeoDarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // window.statusBarColor was deprecated in API 35 (edge-to-edge is enforced by default);
            // keep the explicit transparent setting for API 30–34 where it is still honoured.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                @Suppress("DEPRECATION")
                window.statusBarColor = Color.Transparent.toArgb()
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
