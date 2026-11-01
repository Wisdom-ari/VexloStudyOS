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
val Blue = Color(0xFF0A6EBD)
val Purple = Color(0xFF6C5CE7)
val Mint = Color(0xFF10B981)
val Amber = Color(0xFFF5B301)
val Rose = Color(0xFFE5484D)
val Cyan = Color(0xFF0EA5C6)

val Grad = Brush.horizontalGradient(listOf(Color(0xFF0A6EBD), Color(0xFF2B8DDB)))

/** Course thumbnails: green, blue, pink, grey, violet, yellow. */
val CourseColors = listOf(
    Color(0xFF10B981), Color(0xFF1E88E5), Color(0xFFF472B6),
    Color(0xFF9AA5B1), Color(0xFF6C5CE7), Color(0xFFFBBF24)
)

/** Colours that change with the theme. */
data class Palette(
    val dark: Boolean,
    val bg: Color,
    val surface1: Color,
    val surface2: Color,
    val textHi: Color,
    val textLo: Color,
    val border: Color,
    val strip: Color
)

val DarkPalette = Palette(
    dark = true,
    bg = Color(0xFF0E1420),
    surface1 = Color(0xFF172033),
    surface2 = Color(0xFF202B42),
    textHi = Color(0xFFE8EEF8),
    textLo = Color(0xFF9AA7BD),
    border = Color(0xFF2A3650),
    strip = Color(0xFF0A3A63)
)

val LightPalette = Palette(
    dark = false,
    bg = Color(0xFFF2F5F9),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEAEFF5),
    textHi = Color(0xFF1B2733),
    textLo = Color(0xFF667385),
    border = Color(0xFFDDE3EB),
    strip = Color(0xFF0A6EBD)
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

val Navy: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.bg
val Surface1: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface1
val Surface2: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface2
val TextHi: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textHi
val TextLo: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textLo
val Border: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.border
val Strip: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.strip
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
            outline = palette.border
        )
    } else {
        lightColorScheme(
            primary = Blue, secondary = Purple, background = palette.bg, surface = palette.surface1,
            surfaceVariant = palette.surface2, onPrimary = Color.White, onSecondary = Color.White,
            onBackground = palette.textHi, onSurface = palette.textHi, onSurfaceVariant = palette.textLo,
            outline = palette.border
        )
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
