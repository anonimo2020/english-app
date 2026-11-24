package com.example.englishapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.englishapp.model.ThemePreference
import com.example.englishapp.model.TutorCharacter

@Immutable
data class FluentExtraColors(
  val isDark: Boolean,
  val manuAccent: Color,
  val eddyAccent: Color,
  val success: Color,
  val correction: Color,
  val recording: Color,
  val cardBorder: Color,
  val userBubbleGradientStart: Color,
  val userBubbleGradientEnd: Color,
  val cyanGlow: Color,
  val violetGlow: Color,
  val violetDeep: Color,
  val streakFlame: Color,
  val callRed: Color,
  val glassSurface: Color,
) {
  fun accentFor(character: TutorCharacter): Color =
    if (character == TutorCharacter.MANU) manuAccent else eddyAccent
}

private val DarkExtraColors = FluentExtraColors(
  isDark = true,
  manuAccent = DarkManuAccent,
  eddyAccent = DarkEddyAccent,
  success = DarkSuccess,
  correction = DarkCorrection,
  recording = DarkRecording,
  cardBorder = DarkOutline,
  userBubbleGradientStart = Color(0xFF7C3AED),
  userBubbleGradientEnd = Color(0xFF4F46E5),
  cyanGlow = DarkCyanGlow,
  violetGlow = DarkPrimary,
  violetDeep = DarkVioletDeep,
  streakFlame = DarkStreakFlame,
  callRed = DarkCallRed,
  glassSurface = Color(0xCC14182E),
)

private val LightExtraColors = FluentExtraColors(
  isDark = false,
  manuAccent = LightManuAccent,
  eddyAccent = LightEddyAccent,
  success = LightSuccess,
  correction = LightCorrection,
  recording = LightRecording,
  cardBorder = LightOutline,
  userBubbleGradientStart = Color(0xFF7C3AED),
  userBubbleGradientEnd = Color(0xFF4F46E5),
  cyanGlow = LightCyanGlow,
  violetGlow = LightPrimary,
  violetDeep = LightVioletDeep,
  streakFlame = LightStreakFlame,
  callRed = LightCallRed,
  glassSurface = Color(0xEEFFFFFF),
)

val LocalFluentColors = staticCompositionLocalOf { DarkExtraColors }

private val DarkColorScheme = darkColorScheme(
  primary = DarkPrimary,
  onPrimary = DarkOnPrimary,
  primaryContainer = Color(0xFF282159),
  onPrimaryContainer = DarkTextPrimary,
  secondary = DarkCyanGlow,
  onSecondary = DarkCanvas,
  tertiary = DarkManuAccent,
  onTertiary = DarkOnPrimary,
  background = DarkCanvas,
  onBackground = DarkTextPrimary,
  surface = DarkSurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkOutline,
  error = DarkRecording,
  onError = DarkCanvas,
)

private val LightColorScheme = lightColorScheme(
  primary = LightPrimary,
  onPrimary = LightOnPrimary,
  primaryContainer = Color(0xFFEDE9FE),
  onPrimaryContainer = Color(0xFF2E1065),
  secondary = LightEddyAccent,
  onSecondary = Color.White,
  tertiary = LightManuAccent,
  onTertiary = Color.White,
  background = LightCanvas,
  onBackground = LightTextPrimary,
  surface = LightSurface,
  onSurface = LightTextPrimary,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightTextSecondary,
  outline = LightOutline,
  error = LightRecording,
  onError = Color.White,
)

@Composable
fun FluentVoiceTheme(
  themePreference: ThemePreference = ThemePreference.DARK,
  content: @Composable () -> Unit,
) {
  val isDark = when (themePreference) {
    ThemePreference.DARK -> true
    ThemePreference.LIGHT -> false
    ThemePreference.SYSTEM -> isSystemInDarkTheme()
  }
  val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
  val extra = if (isDark) DarkExtraColors else LightExtraColors

  CompositionLocalProvider(LocalFluentColors provides extra) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = FluentTypography,
      content = content,
    )
  }
}

object FluentTheme {
  val colors: FluentExtraColors
    @Composable get() = LocalFluentColors.current
}
