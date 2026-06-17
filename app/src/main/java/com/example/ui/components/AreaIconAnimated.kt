package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private val OvershootEasing = Easing { t ->
    val tension = 2.0f
    val t1 = t - 1.0f
    t1 * t1 * ((tension + 1) * t1 + tension) + 1.0f
}

private val StrokeColor = Color(0xFF9C9C9C)
private val FillColor = Color.White

@Composable
fun AreaIconAnimated(
    isClosed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(targetState = isClosed, label = "boxTransition")

    // 1. Анимация смещения крышки
    val lidTranslateY by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                // Закрытие
                tween(400, easing = OvershootEasing)
            } else {
                // Открытие[cite: 2]
                tween(400, easing = FastOutSlowInEasing) 
            }
        },
        label = "lidTranslateY"
    ) { state -> if (state) -0.2f else -5f }

    // 2. Анимация прозрачности заливки и основной обводки крышки
    val lidFillAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(100, easing = LinearEasing)
            } else {
                // Открытие: задержка 200мс, длительность 195мс[cite: 2]
                tween(195, delayMillis = 200, easing = LinearEasing)
            }
        },
        label = "lidFillAlpha"
    ) { state -> if (state) 1f else 0f }

    // 3. Анимация прозрачности дополнительной обводки крышки
    val lidStrokeAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(100, easing = LinearEasing)
            } else {
                // Открытие: задержка 200мс, длительность 175мс[cite: 2]
                tween(175, delayMillis = 200, easing = LinearEasing)
            }
        },
        label = "lidStrokeAlpha"
    ) { state -> if (state) 1f else 0f }

    // 4. Анимация верхней линии корзины
    val bodyTopLineAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(100, delayMillis = 50, easing = LinearEasing)
            } else {
                tween(100, easing = LinearEasing) // Быстрое появление при открытии
            }
        },
        label = "bodyTopLineAlpha"
    ) { state -> if (state) 0f else 1f }

    // 5. Анимация высоты стенки (морфинг)
    val wallHeight by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(250, delayMillis = 150, easing = OvershootEasing)
            } else {
                // Длительность 300мс из закомментированного блока XML[cite: 2]
                tween(300, easing = FastOutSlowInEasing)
            }
        },
        label = "wallHeight"
    ) { state -> if (state) 6f else 7f }

    val bodyTopLinePath = remember { PathParser().parsePathString("M3 16.5c-1 1.2-.2 1.9.8 2.4L10.2 22.1Q12 23 13.8 22.1L20.2 18.9c1-.5 1.8-1.2.8-2.4").toPath() }
    val lidFillPath = remember { PathParser().parsePathString("M2 18.5 2 20.5 10.2 24.6Q12 25.5 13.8 24.6L22 20.5 22 18.5Q22 16.5 20.2 15.6L13.8 12.4Q12 11.5 10.2 12.4L3.8 15.6Q2 16.5 2 18.5Z").toPath() }
    
    val lidStrokeTrimmed = remember {
        val basePath = PathParser().parsePathString("M2 16.3l8.2 4.1q1.8.9 3.6 0l8.2-4.1").toPath()
        val measure = PathMeasure().apply { setPath(basePath, false) }
        Path().apply { measure.getSegment(measure.length * 0.05f, measure.length * 0.95f, this, true) }
    }

    val dynamicBodyPath = remember { Path() }
    
    val mainStroke = remember { Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val thinStroke1 = remember { Stroke(width = 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val thinStroke2 = remember { Stroke(width = 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round) }

    val interactionSource = remember { MutableInteractionSource() }

    Canvas(
        modifier = modifier
            .size(24.dp, 34.dp)
            .clickable(interactionSource = interactionSource, indication = null) { onToggle() }
    ) {
        val scale = minOf(size.width / 24f, size.height / 34f)
        val dx = (size.width - 24f * scale) / 2f
        val dy = (size.height - 34f * scale) / 2f - 4.dp.toPx()

        translate(left = dx, top = dy) {
            scale(scale, scale, pivot = Offset.Zero) {
                
                dynamicBodyPath.rewind()
                dynamicBodyPath.apply {
                    moveTo(2f, 25.5f)
                    relativeQuadraticTo(0f, 2f, 1.8f, 2.9f)
                    relativeLineTo(6.4f, 3.2f)
                    relativeQuadraticTo(1.8f, 0.9f, 3.6f, 0f)
                    relativeLineTo(6.4f, -3.2f)
                    relativeQuadraticTo(1.8f, -0.9f, 1.8f, -2.9f)
                    relativeLineTo(0f, -wallHeight)
                    relativeQuadraticTo(0f, -2f, -1.8f, -2.9f)
                    relativeLineTo(-6.4f, -3.2f)
                    relativeQuadraticTo(-1.8f, -0.9f, -3.6f, 0f)
                    relativeLineTo(-6.4f, 3.2f)
                    relativeQuadraticTo(-1.8f, 0.9f, -1.8f, 2.9f)
                    close()
                }

                drawPath(path = dynamicBodyPath, color = StrokeColor, style = mainStroke)

                drawPath(
                    path = bodyTopLinePath,
                    color = StrokeColor.copy(alpha = bodyTopLineAlpha),
                    style = thinStroke1
                )

                translate(top = lidTranslateY) {
                    drawPath(path = lidFillPath, color = FillColor.copy(alpha = lidFillAlpha), style = Fill)
                    drawPath(path = lidFillPath, color = StrokeColor.copy(alpha = lidFillAlpha), style = mainStroke)
                    // Используем отдельную переменную alpha для обрезанной обводки крышки
                    drawPath(path = lidStrokeTrimmed, color = StrokeColor.copy(alpha = lidStrokeAlpha), style = thinStroke2)
                }
            }
        }
    }
}