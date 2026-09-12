package com.example.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.offset

/**
 * Отступы, плавно переходящие от [collapsed] к [expanded] по мере [progress] (0..1).
 *
 * В отличие от `padding(lerp(...))`, прогресс читается только на этапе раскладки: анимация
 * меняет отступы без перекомпозиции содержимого — каждый кадр лишь заново измеряется этот узел.
 * Округление и размещение — как у стандартного `padding`.
 */
fun Modifier.progressPadding(
    progress: () -> Float,
    collapsed: PaddingValues,
    expanded: PaddingValues
): Modifier = layout { measurable, constraints ->
    val p = progress()
    val start = lerp(collapsed.calculateStartPadding(layoutDirection), expanded.calculateStartPadding(layoutDirection), p).roundToPx()
    val end = lerp(collapsed.calculateEndPadding(layoutDirection), expanded.calculateEndPadding(layoutDirection), p).roundToPx()
    val top = lerp(collapsed.calculateTopPadding(), expanded.calculateTopPadding(), p).roundToPx()
    val bottom = lerp(collapsed.calculateBottomPadding(), expanded.calculateBottomPadding(), p).roundToPx()

    val horizontal = start + end
    val vertical = top + bottom
    val placeable = measurable.measure(constraints.offset(-horizontal, -vertical))
    val width = constraints.constrainWidth(placeable.width + horizontal)
    val height = constraints.constrainHeight(placeable.height + vertical)
    layout(width, height) {
        placeable.placeRelative(start, top)
    }
}
