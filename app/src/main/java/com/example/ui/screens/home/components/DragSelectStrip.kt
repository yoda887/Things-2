package com.example.ui.screens.home.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp

/**
 * Полоса выбора протяжкой у правого края списка в режиме множественного выбора (задач или проектов): палец ведут
 * по кружкам выбора — каждая строка, по которой он прошёл, получает то же состояние, что и первая (выбрана, если
 * первая не была выбрана, и наоборот), с лёгким откликом на каждой.
 *
 * @param listOffsetY сдвиг списка по вертикали (оттяжка к поиску) — точка касания пересчитывается в координаты списка
 * @param isSelectable можно ли выбирать строку с таким ключом (ключи строк списка — id)
 * @param isSelected выбрана ли строка сейчас
 * @param onSelect поставить строке состояние выбора
 * @param onDraggingChange протяжка началась (true) или закончилась (false)
 */
@Composable
fun BoxScope.DragSelectStrip(
    lazyListState: LazyListState,
    width: Dp,
    topPadding: Dp,
    bottomPadding: Dp,
    listOffsetY: () -> Float,
    isSelectable: (String) -> Boolean,
    isSelected: (String) -> Boolean,
    onSelect: (id: String, selected: Boolean) -> Unit,
    onDraggingChange: (Boolean) -> Unit
) {
    val view = LocalView.current
    val currentListOffsetY by rememberUpdatedState(listOffsetY)
    val currentIsSelectable by rememberUpdatedState(isSelectable)
    val currentIsSelected by rememberUpdatedState(isSelected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnDraggingChange by rememberUpdatedState(onDraggingChange)

    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .fillMaxHeight()
            .width(width)
            .padding(top = topPadding, bottom = bottomPadding)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // Строки, пересекающие вертикальный интервал движения пальца [y1, y2], в порядке движения
                    fun idsInRange(y1: Float, y2: Float, movingDown: Boolean): List<String> {
                        val minY = minOf(y1, y2) - currentListOffsetY()
                        val maxY = maxOf(y1, y2) - currentListOffsetY()
                        val hit = lazyListState.layoutInfo.visibleItemsInfo.filter { info ->
                            info.offset.toFloat() <= maxY && info.offset + info.size.toFloat() >= minY
                        }
                        return (if (movingDown) hit else hit.asReversed())
                            .mapNotNull { (it.key as? String)?.takeIf { key -> currentIsSelectable(key) } }
                    }

                    var lastY = down.position.y
                    val first = idsInRange(lastY, lastY, movingDown = true).firstOrNull() ?: return@awaitEachGesture
                    down.consume()
                    currentOnDraggingChange(true)
                    try {
                        val select = !currentIsSelected(first)
                        val touched = mutableSetOf<String>()
                        fun apply(id: String) {
                            if (!touched.add(id)) return
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            currentOnSelect(id, select)
                        }
                        apply(first)
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            change.consume()
                            val y = change.position.y
                            idsInRange(lastY, y, movingDown = y >= lastY).forEach(::apply)
                            lastY = y
                        }
                    } finally {
                        currentOnDraggingChange(false)
                    }
                }
            }
    )
}
