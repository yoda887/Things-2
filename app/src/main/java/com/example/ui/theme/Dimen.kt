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
    val mainCheckboxSize: Dp = 16.dp,
    val taskLeftColumnWidthDefault: Dp = 16.dp,
    val taskSpacingToTextDefault: Dp = 8.dp,
    val searchLeftColumnWidth: Dp = 24.dp,
    val searchSpacingToText: Dp = 12.dp,
    val taskItemEstimatedHeight: Dp = 56.dp,
    val mainHeaderPaddingTop: Dp = 24.dp,
    val dialogWidth: Dp = 320.dp,
    val dialogHeight: Dp = 480.dp,
    val dialogRoundedCornerSize: Dp = 16.dp,
    val dialogInnerContentPadding: Dp = 16.dp,
    val floatingToolbarHeight: Dp = 50.dp,
    val floatingToolbarBottomPadding: Dp = 24.dp,
    val floatingToolbarCornerRadius: Dp = 25.dp,
    val eveningSectionSpacing: Dp = 32.dp,
    val mainHeaderPaddingBottom: Dp = 25.dp,
    val calendarBetweenSectionSpacing: Dp = 25.dp,
    val tagsBetweenSectionSpacing: Dp = 11.dp,
    val taskExpandedVerticalGap: Dp = 42.dp
)

val LocalAppDimens = staticCompositionLocalOf { AppDimens() }

val MaterialTheme.dimens: AppDimens
    @Composable
    @ReadOnlyComposable
    get() = LocalAppDimens.current
