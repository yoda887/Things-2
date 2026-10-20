package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import com.example.ui.theme.ThingsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Подсветка найденного через поиск — по кадрам эталона Things 3
private const val FOUND_GLOW_IN_MS = 160
private const val FOUND_GLOW_HOLD_MS = 80L
private const val FOUND_GLOW_OUT_MS = 720
private const val FOUND_SCALE_DELAY_MS = 40L
private const val FOUND_SCALE_UP_MS = 60
private const val FOUND_SCALE_DOWN_MS = 650
private const val FOUND_SCALE_PEAK = 1.05f

/** Пауза перед подсветкой найденного: экран успевает открыться и докрутить до него */
const val FOUND_HIGHLIGHT_DELAY_MS = 450L

/** Ход подсветки найденного: жёлтый (0..1) и пульс размера строки */
class FoundHighlight internal constructor(internal val glow: Animatable<Float, *>, internal val pulse: Animatable<Float, *>) {
    val glowValue: Float get() = glow.value
    val scale: Float get() = pulse.value
}

/**
 * Подсветка строки, к которой перешли из поиска ([isFound]): после паузы строка за ~160 мс наливается жёлтым,
 * чуть увеличивается и за ~650 мс возвращается к своему размеру, а жёлтый за ~720 мс растворяется.
 */
@Composable
fun rememberFoundHighlight(isFound: Boolean): FoundHighlight {
    val highlight = remember { FoundHighlight(Animatable(0f), Animatable(1f)) }
    LaunchedEffect(isFound) {
        if (isFound) {
            delay(FOUND_HIGHLIGHT_DELAY_MS)
            launch {
                highlight.glow.animateTo(1f, tween(FOUND_GLOW_IN_MS, easing = LinearEasing))
                delay(FOUND_GLOW_HOLD_MS)
                highlight.glow.animateTo(0f, tween(FOUND_GLOW_OUT_MS, easing = LinearOutSlowInEasing))
            }
            delay(FOUND_SCALE_DELAY_MS)
            highlight.pulse.animateTo(FOUND_SCALE_PEAK, tween(FOUND_SCALE_UP_MS, easing = LinearEasing))
            highlight.pulse.animateTo(1f, tween(FOUND_SCALE_DOWN_MS, easing = LinearOutSlowInEasing))
        } else {
            highlight.glow.snapTo(0f)
            highlight.pulse.snapTo(1f)
        }
    }
    return highlight
}

/**
 * Жёлтый найденного — поверх строки (foreground), как выделение ([selectionTint]): ложится и на плашки, чипы и
 * чекбокс. Цвет смешивается с содержимым, а не закрывает его: в светлой теме — умножением (белое становится
 * жёлтым, тёмный текст остаётся тёмным), в тёмной — осветлением (тёмный фон желтеет, светлый текст остаётся светлым).
 */
@Composable
fun Modifier.foundHighlight(highlight: FoundHighlight, shape: Shape): Modifier {
    val color = ThingsTheme.colors.searchMatchHighlight
    val blend = if (ThingsTheme.colors.isDark) BlendMode.Screen else BlendMode.Multiply
    return drawWithContent {
        drawContent()
        val glow = highlight.glowValue
        if (glow > 0f) drawOutline(shape.createOutline(size, layoutDirection, this), color.copy(alpha = glow), blendMode = blend)
    }
}
