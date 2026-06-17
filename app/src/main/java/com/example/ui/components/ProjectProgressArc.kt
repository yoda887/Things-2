package com.example.ui.components

import androidx.compose.foundation.Canvas
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
    modifier: Modifier = Modifier
) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    
    Canvas(modifier = modifier) {
        // Динамическая толщина линии внешнего кольца (ровно 8% от диаметра)
        val strokeWidth = size.width * 0.08f
        // Минимальный отступ от краев Canvas, чтобы внешнее кольцо не обрезалось
        val edgePadding = strokeWidth / 2f
        
        val circleColor = Color.Gray.copy(alpha = 0.6f)
        
        // Рисуем целую окружность серого цвета в качестве контура
        drawArc(
            color = circleColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(edgePadding, edgePadding),
            size = androidx.compose.ui.geometry.Size(size.width - 2 * edgePadding, size.height - 2 * edgePadding),
            style = Stroke(width = strokeWidth)
        )
        
        // Заливаем круговой сектор серым цветом внутри контура
        if (progress > 0f) {
            // Динамический отступ для внутреннего кругового сектора (ровно 8% от диаметра)
            val innerPadding = strokeWidth + (size.width * 0.08f)
            if (size.width > 2 * innerPadding) {
                drawArc(
                    color = circleColor,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(innerPadding, innerPadding),
                    size = androidx.compose.ui.geometry.Size(size.width - 2 * innerPadding, size.height - 2 * innerPadding)
                )
            }
        }
    }
}
