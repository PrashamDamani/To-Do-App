package com.example.c030_prashamdamani_assignment2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF283593),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E6FB),
    onPrimaryContainer = Color(0xFF111A5E),
    secondary = Color(0xFF6A5FB0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FA),
    onSecondaryContainer = Color(0xFF241B5C),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF1B1C22),
    surface = Color.White,
    onSurface = Color(0xFF1B1C22),
    surfaceVariant = Color(0xFFF0F1F7),
    onSurfaceVariant = Color(0xFF494B57),
    outline = Color(0xFF767889),
    outlineVariant = Color(0xFFD9DBE6),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8C0FF),
    onPrimary = Color(0xFF111A5E),
    primaryContainer = Color(0xFF2F3A9A),
    onPrimaryContainer = Color(0xFFE3E6FB),
    secondary = Color(0xFFC9C1F2),
    secondaryContainer = Color(0xFF3D3470),
    background = Color(0xFF12131A),
    surface = Color(0xFF1B1C24),
    surfaceVariant = Color(0xFF2A2B36),
    onSurfaceVariant = Color(0xFFC6C7D6)
)

@Composable
fun ToDoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
