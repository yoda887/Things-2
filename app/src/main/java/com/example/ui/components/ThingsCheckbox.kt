package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThingsBlue

/**
 * ThingsCheckbox: A high-fidelity, custom-designed checkbox that mimics the Things app's aesthetics.
 * Animated using vectors, hand-drawn paths, and spring-based kinetics.
 */
@Composable
fun ThingsCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    checkedColor: Color = ThingsBlue,
    uncheckedColor: Color = Color(0xFFC7C7CC)
) {
    // 1. Анимация ПРУЖИНЫ для всей карточки (нажатие/отжатие)
    // Использование Spring.DampingRatioHighBouncy дает легкий пружинящий отскок в конце
    val scaleAnim by animateFloatAsState(
        targetValue = if (checked) 1.0f else 1.0f, 
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkbox_scale"
    )

    // 2. Анимация заполнения фона (от 0 до 1)
    val fillProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "checkbox_fill"
    )

    // 3. Анимация прорисовки галочки по контуру (от 0 до 1)
    val checkDrawProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "check_path_draw"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Canvas(
        modifier = modifier
            .size(size) // Dynamic size parameter
            // Отключаем стандартный круглый ripple-эффект андроида, чтобы он не портил минимализм
            .clickable(
                interactionSource = interactionSource,
                indication = null 
            ) { onCheckedChange() }
    ) {
        val width = this.size.width
        val height = this.size.height
        val cornerRadius = (size * 0.23f).toPx()
        val strokeWidth = (size * 0.07f).toPx().coerceAtLeast(1.dp.toPx())

        // Векторный путь самой галочки внутри квадрата
        val checkPath = Path().apply {
            moveTo(width * 0.28f, height * 0.5f)
            lineTo(width * 0.45f, height * 0.68f)
            lineTo(width * 0.75f, height * 0.32f)
        }

        // Рисуем пустую неактивную рамку (подложку)
        drawRoundRect(
            color = uncheckedColor,
            size = Size(width, height),
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = strokeWidth)
        )

        // Рисуем ПЛАВНО КРАСЯЩИЙСЯ фон, который расширяется из центра
        if (fillProgress > 0f) {
            drawRoundRect(
                color = checkedColor,
                topLeft = Offset(
                    x = (width / 2) * (1f - fillProgress),
                    y = (height / 2) * (1f - fillProgress)
                ),
                size = Size(width * fillProgress, height * fillProgress),
                cornerRadius = CornerRadius(cornerRadius * fillProgress)
            )
        }

        // ЭФФЕКТ КАРАНДАША: Рисуем галочку строго по ее длине на основе прогресса анимации
        if (checkDrawProgress > 0f) {
            val pathMeasure = PathMeasure()
            pathMeasure.setPath(checkPath, false)
            val partialPath = Path()
            
            // Отрезаем кусок пути от 0 до текущего прогресса анимации
            pathMeasure.getSegment(
                startDistance = 0f,
                stopDistance = pathMeasure.length * checkDrawProgress,
                destination = partialPath,
                startWithMoveTo = true
            )

            drawPath(
                path = partialPath,
                color = Color.White,
                style = Stroke(
                    width = (size * 0.09f).toPx().coerceAtLeast(1.5.dp.toPx()),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}
