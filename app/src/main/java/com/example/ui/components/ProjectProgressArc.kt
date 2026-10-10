package com.example.ui.components

import androidx.compose.ui.graphics.PathEffect
import com.example.ui.theme.ThingsAlpha
import com.example.ui.theme.ThingsTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Круглый индикатор прогресса проекта.
 * Рисует тонкую целую окружность серого цвета (трек) и сплошной круговой сектор
 * внутри неё, отображающий процент выполнения задач по проекту.
 */

/** Сколько штрихов у пунктирного контура проекта «Когда-нибудь». */
private const val DASHED_RING_SEGMENTS = 12

@Composable
fun ProjectProgressArc(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
    // [ИЗМЕНЕНИЕ]: Добавлен параметр цвета, по умолчанию серый, для возможности кастомизации на экране проекта
    color: Color = ThingsTheme.colors.textSecondary.copy(alpha = ThingsAlpha.HINT),
    // Проект «Когда-нибудь» — контур пунктиром, как чекбокс задачи «Когда-нибудь»
    dashed: Boolean = false
) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    
    // [ИЗМЕНЕНИЕ]: Применяем .aspectRatio(1f), чтобы холст всегда стремился быть квадратным
    Canvas(modifier = modifier.aspectRatio(1f)) {
        // [ИЗМЕНЕНИЕ]: Вычисляем диаметр на основе минимальной стороны, чтобы окружность никогда не растягивалась и не сплющивалась
        val diameter = minOf(size.width, size.height)
        
        // Динамическая толщина линии внешнего кольца (ровно 8% от диаметра)
        val strokeWidth = diameter * 0.08f
        // Минимальный отступ от краев Canvas, чтобы внешнее кольцо не обрезалось
        val edgePadding = strokeWidth / 2f
        
        // Координаты центрирования круга относительно фактических сторон холста
        val xOffset = (size.width - diameter) / 2f + edgePadding
        val yOffset = (size.height - diameter) / 2f + edgePadding
        
        val circleColor = color
        
        // Рисуем целую окружность серого цвета в качестве контура
        drawArc(
            color = circleColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(xOffset, yOffset),
            size = androidx.compose.ui.geometry.Size(diameter - 2 * edgePadding, diameter - 2 * edgePadding),
            style = Stroke(
                width = strokeWidth,
                pathEffect = if (dashed) {
                    // Окружность делится на равные штрихи и промежутки
                    val dash = (Math.PI.toFloat() * (diameter - 2 * edgePadding)) / (DASHED_RING_SEGMENTS * 2)
                    PathEffect.dashPathEffect(floatArrayOf(dash, dash))
                } else null
            )
        )
        
        // Заливаем круговой сектор серым цветом внутри контура
        if (progress > 0f) {
            // Динамический отступ для внутреннего кругового сектора (ровно 8% от диаметра)
            val innerPadding = strokeWidth + (diameter * 0.08f)
            val innerXOffset = (size.width - diameter) / 2f + innerPadding
            val innerYOffset = (size.height - diameter) / 2f + innerPadding
            
            if (diameter > 2 * innerPadding) {
                drawArc(
                    color = circleColor,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(innerXOffset, innerYOffset),
                    size = androidx.compose.ui.geometry.Size(diameter - 2 * innerPadding, diameter - 2 * innerPadding)
                )
            }
        }
    }
}

/**
 * Закрытый проект — в «Журнале» и в результатах поиска: кольцо той же толщины, внутри галочка
 * (завершён) или крестик ([cancelled]), как у закрытой задачи.
 */
@Composable
fun ClosedProjectIcon(
    cancelled: Boolean,
    modifier: Modifier = Modifier,
    color: Color = ThingsTheme.colors.project
) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val diameter = minOf(size.width, size.height)
        val strokeWidth = diameter * 0.08f
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = color, radius = (diameter - strokeWidth) / 2f, center = center, style = Stroke(width = strokeWidth))
        val markStroke = Stroke(width = diameter * 0.1f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        val path = androidx.compose.ui.graphics.Path().apply {
            if (cancelled) {
                val d = diameter * 0.2f
                moveTo(center.x - d, center.y - d); lineTo(center.x + d, center.y + d)
                moveTo(center.x + d, center.y - d); lineTo(center.x - d, center.y + d)
            } else {
                moveTo(center.x - diameter * 0.22f, center.y + diameter * 0.01f)
                lineTo(center.x - diameter * 0.06f, center.y + diameter * 0.17f)
                lineTo(center.x + diameter * 0.24f, center.y - diameter * 0.16f)
            }
        }
        drawPath(path, color = color, style = markStroke)
    }
}
