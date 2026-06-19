package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThingsBlue
import com.example.ui.theme.ThingsDividerDark
import com.example.ui.theme.ThingsDividerLight

/**
 * Круглый индикатор прогресса проекта.
 * Рисует тонкую целую окружность серого цвета (трек) и сплошной круговой сектор
 * внутри неё, отображающий процент выполнения задач по проекту.
 */
@Composable
fun ProjectProgressArc(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
    // [ИЗМЕНЕНИЕ]: Добавлен параметр цвета, по умолчанию серый, для возможности кастомизации на экране проекта
    color: Color = Color.Gray.copy(alpha = 0.6f)
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
            style = Stroke(width = strokeWidth)
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
