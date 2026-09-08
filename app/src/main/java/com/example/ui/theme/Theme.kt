package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DungeonColorScheme = darkColorScheme(
  primary = DarkPrimary,
  onPrimary = DarkOnPrimary,
  primaryContainer = DarkPrimaryContainer,
  onPrimaryContainer = DarkOnPrimaryContainer,
  secondary = DarkSecondary,
  onSecondary = DarkOnSecondary,
  secondaryContainer = DarkSecondaryContainer,
  onSecondaryContainer = DarkOnSecondaryContainer,
  tertiary = DarkTertiary,
  onTertiary = DarkOnTertiary,
  background = DungeonBackground,
  onBackground = TextPrimary,
  surface = DungeonSurface,
  onSurface = TextPrimary,
  surfaceVariant = DungeonSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = DungeonBorder
)

@Composable
fun DungeonTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = DungeonColorScheme,
    typography = Typography,
    content = content
  )
}
