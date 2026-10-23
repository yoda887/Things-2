package com.example.ui.screens.home.inlineeditor.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * Свойства раскрытой задачи (теги, повтор, дата, дедлайн — строками [content], в своём порядке) и иконки нижнего
 * тулбара ([icons]) справа. Нижнее свойство стоит на уровне иконок, если целиком помещается в одну строку в месте
 * до них (с промежутком [gap]); иначе оно — строкой выше, во всю ширину, а иконки — на своей строке под ним.
 * Ничего не обтекает иконки: поднявшееся свойство на их уровень сбоку не заходит; между ним и строкой иконок —
 * [stackGap], тот же промежуток, что между свойствами одно над другим. Остальные свойства — строками
 * выше, во всю ширину. Промежутки между строками задают сами строки [content] (Spacer) — последним ребёнком
 * [content] должна быть строка свойства, не промежуток.
 */
@Composable
fun EditorAttributesRow(
    gap: Dp,
    icons: @Composable () -> Unit,
    stackGap: Dp = com.example.ui.theme.ThingsSpacing.NONE,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(contents = listOf(content, icons), modifier = modifier) { (rows, iconParts), constraints ->
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val iconPlaceables = iconParts.map { it.measure(loose) }
        val iconsWidth = iconPlaceables.maxOfOrNull { it.width } ?: 0
        val iconsHeight = iconPlaceables.maxOfOrNull { it.height } ?: 0

        val upper = rows.dropLast(1).map { it.measure(loose) }
        val last = rows.lastOrNull()
        val besideWidth = (width - iconsWidth - gap.roundToPx()).coerceAtLeast(0)
        // Нижнее свойство целиком в одну строку до иконок — на их уровне
        val fitsBeside = last != null && last.maxIntrinsicWidth(Constraints.Infinity) <= besideWidth
        val lastPlaceable = last?.measure(if (fitsBeside) Constraints(maxWidth = besideWidth) else loose)

        val upperHeight = upper.sumOf { it.height }
        val bottomLine = if (fitsBeside) maxOf(lastPlaceable!!.height, iconsHeight) else iconsHeight
        // Свойство поднялось над иконками — между ним и их строкой промежуток, как между свойствами
        val lastAbove = if (!fitsBeside) (lastPlaceable?.height?.plus(stackGap.roundToPx()) ?: 0) else 0
        val height = (upperHeight + lastAbove + bottomLine).coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, height) {
            var y = 0
            upper.forEach { it.place(0, y); y += it.height }
            if (lastPlaceable != null && !fitsBeside) {
                lastPlaceable.place(0, y)
                y += lastPlaceable.height + stackGap.roundToPx()
            }
            if (lastPlaceable != null && fitsBeside) lastPlaceable.place(0, y + (bottomLine - lastPlaceable.height) / 2)
            iconPlaceables.forEach { it.place(width - it.width, y + (bottomLine - it.height) / 2) }
        }
    }
}
