package com.fourctech.todaylist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.fourctech.todaylist.domain.model.ThemeMode

private val Navy = Color(0xFF1B4F72)
private val Slate = Color(0xFF2C3E50)
private val SoftBlue = Color(0xFF5DADE2)
private val Mist = Color(0xFFF4F7FA)
private val Ink = Color(0xFF1A1F24)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = SoftBlue,
    onSecondary = Ink,
    background = Mist,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8EEF3),
    onSurfaceVariant = Slate,
)

private val DarkColors = darkColorScheme(
    primary = SoftBlue,
    onPrimary = Ink,
    secondary = SoftBlue,
    onSecondary = Ink,
    background = Color(0xFF12161A),
    onBackground = Color(0xFFE8EEF3),
    surface = Color(0xFF1A2128),
    onSurface = Color(0xFFE8EEF3),
    surfaceVariant = Color(0xFF24303A),
    onSurfaceVariant = Color(0xFFB8C4CE),
)

@Composable
fun TodayListTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
