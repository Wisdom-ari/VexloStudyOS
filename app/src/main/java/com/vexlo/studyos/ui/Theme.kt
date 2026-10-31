package com.vexlo.studyos.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF0A0E1F)
val Surface1 = Color(0xFF131A33)
val Surface2 = Color(0xFF1B2447)
val Blue = Color(0xFF3D7BFF)
val Purple = Color(0xFF8B5CF6)
val Mint = Color(0xFF34D399)
val Amber = Color(0xFFFBBF24)
val Rose = Color(0xFFFB7185)
val Cyan = Color(0xFF22D3EE)
val TextHi = Color(0xFFEAF0FF)
val TextLo = Color(0xFF93A0C4)

val Grad = Brush.horizontalGradient(listOf(Blue, Purple))
val CourseColors = listOf(Blue, Purple, Mint, Amber, Rose, Cyan)

private val Scheme = darkColorScheme(
    primary = Blue,
    secondary = Purple,
    background = Navy,
    surface = Surface1,
    surfaceVariant = Surface2,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextHi,
    onSurface = TextHi,
    onSurfaceVariant = TextLo,
    outline = Color(0xFF3A4570)
)

@Composable
fun StudyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
