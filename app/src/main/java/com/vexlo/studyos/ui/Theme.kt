package com.vexlo.studyos.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Accent colours are the same in light and dark. */
val Blue = Color(0xFF3D7BFF)
val Purple = Color(0xFF8B5CF6)
val Mint = Color(0xFF34D399)
val Amber = Color(0xFFFBBF24)
val Rose = Color(0xFFFB7185)
val Cyan = Color(0xFF22D3EE)

val Grad = Brush.horizontalGradient(listOf(Blue, Purple))
val CourseColors = listOf(Blue, Purple, Mint, Amber, Rose, Cyan)

/** Colours that change with the theme. */
data class Palette(
    val dark: Boolean,
    val bg: Color,
    val surface1: Color,
    val surface2: Color,
    val textHi: Color,
    val textLo: Color,
    val header: Brush
)

val DarkPalette = Palette(
    dark = true,
    bg = Color(0xFF0A0E1F),
    surface1 = Color(0xFF131A33),
    surface2 = Color(0xFF1B2447),
    textHi = Color(0xFFEAF0FF),
    textLo = Color(0xFF93A0C4),
    header = Brush.verticalGradient(listOf(Color(0xFF3B2F8F), Color(0xFF1E3A8A)))
)

val LightPalette = Palette(
    dark = false,
    bg = Color(0xFFEEF1FA),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFE4E9F7),
    textHi = Color(0xFF1A2040),
    textLo = Color(0xFF6B7499),
    header = Brush.verticalGradient(listOf(Color(0xFF6C5CE7), Color(0xFF4A7BF7)))
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

val Navy: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.bg
val Surface1: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface1
val Surface2: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface2
val TextHi: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textHi
val TextLo: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textLo
val HeaderBrush: Brush @Composable @ReadOnlyComposable get() = LocalPalette.current.header
val IsDark: Boolean @Composable @ReadOnlyComposable get() = LocalPalette.current.dark

/** mode: 0 = follow system, 1 = light, 2 = dark. */
@Composable
fun StudyTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) {
        0 -> isSystemInDarkTheme()
        1 -> false
        else -> true
    }
    val palette = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = Blue, secondary = Purple, background = palette.bg, surface = palette.surface1,
            surfaceVariant = palette.surface2, onPrimary = Color.White, onSecondary = Color.White,
            onBackground = palette.textHi, onSurface = palette.textHi, onSurfaceVariant = palette.textLo,
            outline = Color(0xFF3A4570)
        )
    } else {
        lightColorScheme(
            primary = Blue, secondary = Purple, background = palette.bg, surface = palette.surface1,
            surfaceVariant = palette.surface2, onPrimary = Color.White, onSecondary = Color.White,
            onBackground = palette.textHi, onSurface = palette.textHi, onSurfaceVariant = palette.textLo,
            outline = Color(0xFFB8C0DC)
        )
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
