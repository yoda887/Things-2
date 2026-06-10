package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

/**
 * Полностью Canvas-ориентированный индикатор pull-to-search с разделенной анимацией.
 * Включает динамическое позиционирование: лупа плавно выдвигается из-за верхней границы Canvas
 * и останавливается к 75% свайпа, а стрелка плавно выдвигается из-под круга на протяжении всего свайпа.
 * 
 * @param pullOffset Текущее смещение натяжения списка в пикселях.
 * @param thresholdPx Пороговое значение в пикселях, при достижении которого поиск активируется.
 * @param modifier Модификатор для настройки визуального контейнера.
 */
@Composable
fun PullToSearchIndicator(
    pullOffset: Float,
    thresholdPx: Float,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val maxOffsetPx = with(density) { 150.dp.toPx() }

    val progress = if (thresholdPx > 0f) (pullOffset / thresholdPx).coerceIn(0f, 1f) else 0f
    val isTriggered = pullOffset >= thresholdPx

    // "Лупа" завершает анимацию на 75% от thresholdPx
    val magnifierProgress = (progress / 0.75f).coerceIn(0f, 1f)
    
    // "Стрелка" плавно выдвигается из-под круга на протяжении всего растяжения до maxOffsetPx
    val arrowProgress = if (maxOffsetPx > 0f) (pullOffset / maxOffsetPx).coerceIn(0f, 1f) else 0f

    // Цвета c плавным переходом (lerp)
    val bgNormal = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    val circleColor = androidx.compose.ui.graphics.lerp(bgNormal, ThingsBlue, progress)
    val iconNormal = if (isDark) ThingsTextSecondaryDark else ThingsTextSecondaryLight
    val iconColor = androidx.compose.ui.graphics.lerp(iconNormal, Color.White, progress)
    val arrowColor = androidx.compose.ui.graphics.lerp(iconNormal, ThingsBlue, progress)

    if (pullOffset > 0f) {
        Canvas(
            modifier = modifier
                .size(width = 44.dp, height = 96.dp)
                .graphicsLayer {
                    scaleX = 1f
                    scaleY = 1f

                    // stretchProgress — позиция контейнера на экране относительно списка.
                    val indicatorHeightPx = 36.dp.toPx()//72.dp.toPx()

                    // Индикатор сразу вытягивается (выезжает 1:1) пока pullOffset <= 72dp.
                    // Когда вытянется полностью, он плавно перемещается к центру зоны растяжения.
                    translationY = if (pullOffset <= indicatorHeightPx) {
                        pullOffset
                    } else {
                        val centeredY = pullOffset / 2f + indicatorHeightPx / 2f + 20.dp.toPx()
                        val excessProgress = ((pullOffset - indicatorHeightPx) / (maxOffsetPx - indicatorHeightPx)).coerceIn(0f, 1f)
                        pullOffset + (centeredY - pullOffset) * excessProgress
                    }

                    alpha = progress
                }
        ) {
            val strokeWidth = 3.dp.toPx()
            val circleRadius = 22.dp.toPx()
            val circleCenterX = size.width / 2f

            // --- ПОЗИЦИИ ---

            // Лупа центрируется внутри Canvas на высоте 32dp и рисуется без смещения,
            // чтобы индикатор сразу отображался при вытягивании.
            val circleCenterY = 32.dp.toPx()

            // Стрелка привязана к текущему нижнему краю круга — всегда строго под лупой.
            // arrowProgress плавно "выдвигает" её вниз от края круга до финального отступа.
            val circleBottomY = circleCenterY + circleRadius
            val arrowGap = 24.dp.toPx()
            val arrowBaseY = circleBottomY + arrowGap * arrowProgress

            // --- 1. КРУГ ---
            drawCircle(
                color = circleColor,
                radius = circleRadius,
                center = Offset(circleCenterX, circleCenterY)
            )

            // --- 2. ЛУПА ---
            val lensRadius = 6.5.dp.toPx()
            val offsetCompensation = 2.dp.toPx()
            val magnifierCenterX = circleCenterX - offsetCompensation
            // Центр лупы едет вместе с кругом
            val magnifierCenterY = circleCenterY - offsetCompensation

            val currentRotation = magnifierProgress * 30f

            withTransform({
                rotate(degrees = currentRotation, pivot = Offset(magnifierCenterX, magnifierCenterY))
            }) {
                val sweep = if (isTriggered) 360f else magnifierProgress * 360f
                drawArc(
                    color = iconColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(magnifierCenterX - lensRadius, magnifierCenterY - lensRadius),
                    size = Size(lensRadius * 2, lensRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Ручка появляется во второй половине анимации лупы
                if (magnifierProgress > 0.5f) {
                    val handleProgress = if (isTriggered) 1f else (magnifierProgress - 0.5f) * 2f
                    val cos45 = 0.7071068f
                    val startX = magnifierCenterX + lensRadius * cos45
                    val startY = magnifierCenterY + lensRadius * cos45
                    val length = 6.5.dp.toPx() * handleProgress
                    drawLine(
                        color = iconColor,
                        start = Offset(startX, startY),
                        end = Offset(startX + length * cos45, startY + length * cos45),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            // --- 3. СТРЕЛКА ---
            val arrowWidth = 14.dp.toPx()
            val arrowHeight = 7.dp.toPx()
            val arrowCenterX = size.width / 2f

            val arrowPath = Path().apply {
                moveTo(arrowCenterX - arrowWidth / 2f, arrowBaseY)
                lineTo(arrowCenterX, arrowBaseY + arrowHeight)
                lineTo(arrowCenterX + arrowWidth / 2f, arrowBaseY)
            }

            drawPath(
                path = arrowPath,
                color = arrowColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
