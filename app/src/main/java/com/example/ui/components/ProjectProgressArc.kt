package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.ThingsDividerDark
import com.example.ui.theme.ThingsDividerLight

@Composable
fun ProjectProgressArc(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    val isDark = false
    val trackColor = if (isDark) ThingsDividerDark else ThingsDividerLight
    
    Canvas(modifier = modifier) {
        // Track
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 3.dp.toPx())
        )
        // Fill
        drawArc(
            color = ThingsBlue,
            startAngle = -90f,
            sweepAngle = progress * 360f,
            useCenter = false,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
