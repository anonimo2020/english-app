package com.example.englishapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.englishapp.model.ThemePreference
import com.example.englishapp.ui.theme.FluentVoiceTheme

class MainActivity : ComponentActivity() {
  private val mainViewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      val settings by mainViewModel.settings.collectAsState()
      val systemDark = isSystemInDarkTheme()
      val isDark = when (settings.themePreference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> systemDark
      }

      LaunchedEffect(isDark) {
        val barStyle = if (isDark) {
          SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
          SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
      }

      FluentVoiceTheme(themePreference = settings.themePreference) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background,
        ) {
          MainNavigation(mainViewModel = mainViewModel)
        }
      }
    }
  }
}
