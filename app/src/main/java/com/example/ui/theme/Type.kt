package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

// Default static typography as a fallback
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    )
)

/**
 * Creates and remembers a responsive Material 3 Typography system.
 * This dynamically adjusts font sizes based on screen scale factor
 * and maps all custom sizes directly to the standard Material 3 slots.
 */
@Composable
fun rememberThingsTypography(): Typography {
    val configuration = LocalConfiguration.current
    val isLargeScreen = configuration.screenWidthDp >= 600
    val scaleFactor = if (isLargeScreen) 1.25f else 1.0f

    return Typography(
        // Hero title (e.g. Inbox / Today big name) - 32.sp base
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (32 * scaleFactor).sp
        ),
        // Hero Emoji - 30.sp base
        displayMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (30 * scaleFactor).sp
        ),
        // Category headers and project titles (decreased and synchronized)
        displaySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = (19 * scaleFactor).sp
        ),
        // App / Group Settings Header - 24.sp base
        headlineLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        ),
        // Bottom Sheet / Dialog Title - 22.sp base
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (22 * scaleFactor).sp
        ),
        // Dialog Subheaders / Prompt Titles / "When?" Header - 18.sp base
        headlineSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18 * scaleFactor).sp
        ),
        // Section titles / Alternative Medium headers - 16.sp base
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = (16 * scaleFactor).sp
        ),
        // Main Task Title - 15.6.sp base (scaled)
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (18f * scaleFactor).sp
        ),
        // Sheet Header / Editor Notes / Alternative Subheader - 15.sp base (scaled)
        titleSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (15 * scaleFactor).sp
        ),
        // Default text / TextField input / Task details body - increased to 18.sp base
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (18 * scaleFactor).sp
        ),
        // Helper text / Project description / Main tags - increased to 16.sp base
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * scaleFactor).sp
        ),
        // Task List secondary / Date subtitle - 14.sp base (scaled)
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * scaleFactor).sp
        ),
        // Clickable buttons / Action trigger text - increased to 18.sp base
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (18 * scaleFactor).sp
        ),
        // Smaller UI Labels / Subtitle stamps / Badges - 12.sp base (scaled)
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * scaleFactor).sp
        ),
        // Tiny tags / Details Category Label / Priority badge - 11.sp base (scaled)
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (11 * scaleFactor).sp
        )
    )
}

/**
 * Кастомный стиль для заголовка Top App Bar размером 20.sp с масштабированием на планшетах.
 */
val Typography.topAppBarTitle: TextStyle
    @Composable
    get() {
        val configuration = LocalConfiguration.current
        val isLargeScreen = configuration.screenWidthDp >= 600
        val scaleFactor = if (isLargeScreen) 1.25f else 1.0f
        return TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = (20 * scaleFactor).sp
        )
    }

