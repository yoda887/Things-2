package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = ThingsBlue,
    onPrimary = Color.White,
    secondary = ThingsSomedayGrey,
    background = ThingsBackgroundDark,
    surface = ThingsSurfaceDark,
    onBackground = ThingsTextPrimaryDark,
    onSurface = ThingsTextPrimaryDark,
    outline = ThingsDividerDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ThingsBlue,
    onPrimary = Color.White,
    secondary = ThingsSomedayGrey,
    background = ThingsBackgroundLight,
    surface = ThingsSurfaceLight,
    onBackground = ThingsTextPrimaryLight,
    onSurface = ThingsTextPrimaryLight,
    outline = ThingsDividerLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false, // Use Things custom precise colors instead of dynamic wallpaper colors
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val typography = rememberThingsTypography()
  CompositionLocalProvider(LocalAppDimens provides AppDimens()) {
    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
  }
}
