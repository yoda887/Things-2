package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * AppDimens contains custom dimension values for the application layout.
 */
data class AppDimens(
    val mainCheckboxSize: Dp = 16.dp
)

val LocalAppDimens = staticCompositionLocalOf { AppDimens() }

val MaterialTheme.dimens: AppDimens
    @Composable
    @ReadOnlyComposable
    get() = LocalAppDimens.current
