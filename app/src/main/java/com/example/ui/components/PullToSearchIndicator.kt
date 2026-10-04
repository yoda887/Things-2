package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThingsTheme
import com.example.ui.theme.*

// Круг индикатора: центр внутри холста и радиус. Нужны и для отрисовки,
// и чтобы Quick Find вырастал ровно из этого круга
val PULL_TO_SEARCH_CIRCLE_CENTER_Y = 32.dp
val PULL_TO_SEARCH_CIRCLE_RADIUS = 26.dp

/**
 * Вертикальное смещение холста индикатора при оттяжке [pullOffset].
 * Индикатор сразу выезжает 1:1, а вытянувшись, плавно смещается к центру зоны растяжения.
 */
fun pullToSearchIndicatorTranslationY(pullOffset: Float, density: Density): Float = with(density) {
    val maxOffsetPx = 150.dp.toPx()
    val indicatorHeightPx = 36.dp.toPx()
    if (pullOffset <= indicatorHeightPx) {
        pullOffset
    } else {
        val centeredY = pullOffset / 2f + indicatorHeightPx / 2f + 20.dp.toPx()
        val excessProgress = ((pullOffset - indicatorHeightPx) / (maxOffsetPx - indicatorHeightPx)).coerceIn(0f, 1f)
        pullOffset + (centeredY - pullOffset) * excessProgress
    }
}

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
    val density = androidx.compose.ui.platform.LocalDensity.current
    val maxOffsetPx = with(density) { 150.dp.toPx() }

    val progress = if (thresholdPx > 0f) (pullOffset / thresholdPx).coerceIn(0f, 1f) else 0f
    val isTriggered = pullOffset >= thresholdPx

    // Цвета с дискретным переключением темы (_selected) строго на пороге с коротким кроссфейдом 100 мс
    val circleColor by animateColorAsState(
        targetValue = if (isTriggered) ThingsTheme.colors.accent else ThingsTheme.colors.pullIndicatorBackground,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "pullCircleColor"
    )
    // В эталоне лупа всегда чисто белая
    val iconColor = Color.White

    // Цвета стрелки: normal / selected из ролей темы
    val arrowColor by animateColorAsState(
        targetValue = if (isTriggered) ThingsTheme.colors.pullArrowSelected else ThingsTheme.colors.pullArrow,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "pullArrowColor"
    )

    if (pullOffset > 0f) {
        val fontScale = density.fontScale

        Canvas(
            modifier = modifier
                .size(width = 60.dp, height = 140.dp)
                .graphicsLayer {
                    scaleX = 1f
                    scaleY = 1f

                    translationY = pullToSearchIndicatorTranslationY(pullOffset, density)

                    alpha = progress
                }
        ) {
            val circleRadius = PULL_TO_SEARCH_CIRCLE_RADIUS.toPx()
            val circleCenterX = size.width / 2f
            val circleCenterY = PULL_TO_SEARCH_CIRCLE_CENTER_Y.toPx()

            // Стрелка привязана к текущему нижнему краю круга.
            // В эталоне до порога срабатывания зазор стабилен (~10 dp),
            // а при перетяжке за порог круг демпфируется, и стрелка отъезжает вниз (до ~40 dp).
            val circleBottomY = circleCenterY + circleRadius
            val baseGap = (10.dp * fontScale).toPx()
            val excessOffset = (pullOffset - thresholdPx).coerceAtLeast(0f)
            val maxExcess = (maxOffsetPx - thresholdPx).coerceAtLeast(1f)
            val stretchProgress = (excessOffset / maxExcess).coerceIn(0f, 1f)
            val dynamicGap = baseGap + (30.dp * fontScale).toPx() * stretchProgress
            val arrowBaseY = circleBottomY + dynamicGap

            // --- 1. КРУГ ---
            drawCircle(
                color = circleColor,
                radius = circleRadius,
                center = Offset(circleCenterX, circleCenterY)
            )

            // --- 2. ЛУПА (PullToSearchLoupe) ---
            // Диаметр линзы ~25 dp (радиус 12.5 dp), обводка 3 dp, ручка 3 dp длиной 10 dp.
            // Фиксированный масштаб 100% на протяжении всей протяжки (без масштабирования).
            val ringStrokeWidth = (3.dp * fontScale).toPx()
            val handleStrokeWidth = (3.dp * fontScale).toPx()
            val lensRadius = (12.5.dp * fontScale).toPx()
            val fullHandleLength = (10.dp * fontScale).toPx()

            // Центр линзы слегка смещен от центра круга вверх-влево для балансировки ручки под 45°
            val magnifierCenterX = circleCenterX - (1.5.dp * fontScale).toPx()
            val magnifierCenterY = circleCenterY - (1.5.dp * fontScale).toPx()

            // Геометрия аутентичного смыкания Things:
            // Края дуги кольца лупы смыкаются слева вверху (-135° / 10:30), а не возле ручки (45°).
            // Верхний конец дуги фиксируется в -135°, а нижний обегает по часовой стрелке до стыка в -135° (225°).
            val normProgress = if (isTriggered) 1f else (progress / 0.85f).coerceIn(0f, 1f)
            val topProg = (normProgress / 0.4f).coerceIn(0f, 1f)
            val topEnd = -45f - 90f * topProg
            val botEnd = 60f + 165f * normProgress
            val startAngle = if (isTriggered || normProgress >= 1f) -135f else topEnd
            val sweepAngle = if (isTriggered || normProgress >= 1f) 360f else (botEnd - topEnd)
            val handleAngle = if (isTriggered) 45f else (45f - 45f * (1f - normProgress))

            if (progress > 0.08f || isTriggered) {
                drawArc(
                    color = iconColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(magnifierCenterX - lensRadius, magnifierCenterY - lensRadius),
                    size = Size(lensRadius * 2, lensRadius * 2),
                    style = Stroke(width = ringStrokeWidth, cap = StrokeCap.Round)
                )
            }

            // Ручка появляется на ведущем краю дуги и поворачивается строго вместе с кончиком дуги в 45°
            if (progress > 0.15f || isTriggered) {
                val handleProgress = if (isTriggered) 1f else ((progress - 0.15f) / 0.85f).coerceIn(0f, 1f)
                val rad = Math.toRadians(handleAngle.toDouble())
                val cosA = kotlin.math.cos(rad).toFloat()
                val sinA = kotlin.math.sin(rad).toFloat()
                val startX = magnifierCenterX + lensRadius * cosA
                val startY = magnifierCenterY + lensRadius * sinA
                val length = fullHandleLength * handleProgress

                drawLine(
                    color = iconColor,
                    start = Offset(startX, startY),
                    end = Offset(startX + length * cosA, startY + length * sinA),
                    strokeWidth = handleStrokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // --- 3. СТРЕЛКА (PullArrow) ---
            val arrowStrokeWidth = (3.dp * fontScale).toPx()
            val arrowCenterX = size.width / 2f
            // Округление смещения до целого пикселя
            val pixelArrowY = kotlin.math.round(arrowBaseY)
            val halfWidthPx = (13.dp * fontScale).toPx()
            val arrowDepthPx = (8.75.dp * fontScale).toPx()

            val arrowPath = Path().apply {
                moveTo(arrowCenterX - halfWidthPx, pixelArrowY)
                lineTo(arrowCenterX, pixelArrowY + arrowDepthPx)
                lineTo(arrowCenterX + halfWidthPx, pixelArrowY)
            }

            drawPath(
                path = arrowPath,
                color = arrowColor,
                style = Stroke(
                    width = arrowStrokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
