package com.example.ui.screens.home.subcomponents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThingsAlpha
import com.example.ui.theme.ThingsIconSize
import com.example.ui.theme.ThingsMotion
import com.example.ui.theme.ThingsSpacing
import com.example.ui.theme.ThingsStroke
import com.example.ui.theme.ThingsTheme

private val SELECTION_CIRCLE_SIZE = 28.dp
private val SELECTION_RING_GAP = 2.dp
private const val SELECT_BOUNCE_MS = 80

/**
 * Кружок выбора строки в режиме множественного выбора — у задач и у проектов главного экрана: обводка,
 * при выборе — синяя заливка с пружинкой; [isDragSelecting] — строку сейчас выбирают протяжкой.
 */
@Composable
fun SelectionCircle(isSelected: Boolean, isDragSelecting: Boolean, onToggle: () -> Unit) {
    val targetBorderColor = when {
        isSelected -> ThingsTheme.colors.accent
        isDragSelecting -> ThingsTheme.colors.accent.copy(alpha = ThingsAlpha.HALF)
        else -> ThingsTheme.colors.checkboxBorder
    }
    val animatedBorderColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = ThingsMotion.BASE),
        label = "selectionBorderColor"
    )
    val animatedCheckScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "selectionCheckScale"
    )

    val animatedDragSelectScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isDragSelecting) 1.14f else 1f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = ThingsMotion.BASE),
        label = "dragSelectScale"
    )

    val iconBounceScale = remember { androidx.compose.animation.core.Animatable(1f) }
    var isInitialComposition by remember { mutableStateOf(true) }

    LaunchedEffect(isSelected) {
        if (isInitialComposition) {
            isInitialComposition = false
        } else {
            iconBounceScale.animateTo(
                targetValue = 1.18f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = SELECT_BOUNCE_MS,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
            iconBounceScale.animateTo(
                targetValue = 1.0f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            )
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(ThingsSpacing.S))
        val view = androidx.compose.ui.platform.LocalView.current
        Box(
            modifier = Modifier
                .size(SELECTION_CIRCLE_SIZE)
                .graphicsLayer {
                    scaleX = iconBounceScale.value * animatedDragSelectScale
                    scaleY = iconBounceScale.value * animatedDragSelectScale
                }
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    onToggle()
                },
            contentAlignment = Alignment.Center
        ) {
            val selectionFill = ThingsTheme.colors.accent
            androidx.compose.foundation.Canvas(
                modifier = Modifier.size(ThingsIconSize.XL)
            ) {
                val strokeWidth = ThingsStroke.BOLD.toPx()
                val radius = size.minDimension / 2f
                
                // 1. Внешняя окружность с плавным переходом цвета
                drawCircle(
                    color = animatedBorderColor,
                    radius = radius - strokeWidth / 2f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )

                // 2. Анимированное заполнение при выборе задачи (spring bounce)
                if (animatedCheckScale > 0.001f) {
                    // Центральная синяя заливка с пружинным масштабированием
                    val targetInnerRadius = (radius - strokeWidth) - SELECTION_RING_GAP.toPx()
                    val currentInnerRadius = targetInnerRadius * animatedCheckScale
                    if (currentInnerRadius > 0f) {
                        drawCircle(
                            color = selectionFill,
                            radius = currentInnerRadius
                        )
                    }
                }
            }
        }
    }
}
