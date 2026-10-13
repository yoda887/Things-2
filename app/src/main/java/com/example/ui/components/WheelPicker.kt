package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThingsTheme
import kotlinx.coroutines.flow.drop

/** Строка барабана в покое; видимая высота барабана — с учётом того, что к краям строки сжимаются */
internal val WHEEL_ROW_HEIGHT = 40.dp
internal val WHEEL_HEIGHT = 220.dp

/** Барабан — цилиндр: соседние строки повёрнуты на этот угол; дальше [WHEEL_MAX_ANGLE_DEG] не видно */
private const val WHEEL_ROW_ANGLE_DEG = 21f
private const val WHEEL_MAX_ANGLE_DEG = 80f

/** Строк, раскладываемых по каждую сторону от середины (сжатые края цилиндра) */
private const val WHEEL_SIDE_ROWS = 4

/** Барабан бесконечный: столько повторов списка, начинаем с середины */
private const val WHEEL_REPEATS = 1000

/** Прозрачность самых дальних строк барабана */
private const val WHEEL_EDGE_ALPHA = 0.2f

/**
 * Состояние колонки барабана. Цикличная колонка — бесконечная: список повторён [WHEEL_REPEATS] раз,
 * начинаем с середины; выбранное значение — номер строки посередине по модулю длины списка.
 */
internal class WheelState(val list: LazyListState, val size: Int, val cyclic: Boolean) {
    val count: Int get() = if (cyclic) size * WHEEL_REPEATS else size
    val selected: Int get() {
        val first = list.firstVisibleItemIndex
        val half = (list.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0) / 2
        val index = first + if (list.firstVisibleItemScrollOffset > half) 1 else 0
        return (index % size).coerceIn(0, size - 1)
    }

    /** Плавно повернуть барабан к значению [value] (по кратчайшему пути у цикличного) */
    suspend fun animateTo(value: Int) {
        val current = list.firstVisibleItemIndex
        val target = if (cyclic) current - (current % size) + value else value
        list.animateScrollToItem(target)
    }
}

@Composable
internal fun rememberWheelState(initial: Int, size: Int, cyclic: Boolean): WheelState {
    val start = if (cyclic) size * (WHEEL_REPEATS / 2) + initial else initial.coerceIn(0, size - 1)
    val list = rememberLazyListState(initialFirstVisibleItemIndex = start)
    return remember(list) { WheelState(list, size, cyclic) }
}

/**
 * Колонка барабана — цилиндр, как в Things: строки встают по местам, к краям сжимаются, бледнеют и
 * сходятся к середине; выбранная — крупнее и жирнее; тик на каждом значении, проходящем через середину.
 * Раскладка шире видимой части, чтобы сжатые крайние строки тоже были на месте.
 */
@Composable
internal fun Wheel(items: List<String>, state: WheelState, width: Dp) {
    val density = LocalDensity.current
    val rowPx = with(density) { WHEEL_ROW_HEIGHT.toPx() }
    val stepRad = Math.toRadians(WHEEL_ROW_ANGLE_DEG.toDouble()).toFloat()
    val radiusPx = rowPx / stepRad
    val maxRad = Math.toRadians(WHEEL_MAX_ANGLE_DEG.toDouble()).toFloat()
    val layoutHeight = WHEEL_ROW_HEIGHT * (2 * WHEEL_SIDE_ROWS + 1)
    val view = LocalView.current
    LaunchedEffect(state) {
        snapshotFlow { state.selected }
            .drop(1)
            .collect { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    }
    Box(modifier = Modifier.width(width).height(WHEEL_HEIGHT), contentAlignment = Alignment.Center) {
        LazyColumn(
            state = state.list,
            flingBehavior = rememberSnapFlingBehavior(state.list),
            contentPadding = PaddingValues(vertical = WHEEL_ROW_HEIGHT * WHEEL_SIDE_ROWS),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(width)
                .requiredHeight(layoutHeight)
        ) {
            items(state.count) { index ->
                val selected = remember { derivedStateOf { state.selected == index % state.size } }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WHEEL_ROW_HEIGHT)
                        .graphicsLayer {
                            // Угол строки на цилиндре — по её удалению от середины, в строках
                            val info = state.list.layoutInfo
                            val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                            val center = (info.viewportEndOffset + info.viewportStartOffset) / 2f
                            val offsetPx = item?.let { it.offset + it.size / 2f - center } ?: 0f
                            val angle = (offsetPx / rowPx) * stepRad
                            if (item == null || kotlin.math.abs(angle) > maxRad) {
                                alpha = 0f
                            } else {
                                // На цилиндре строка стоит на высоте R·sin(угла) и сжата по cos(угла)
                                translationY = radiusPx * kotlin.math.sin(angle) - offsetPx
                                val c = kotlin.math.cos(angle)
                                scaleY = c
                                alpha = WHEEL_EDGE_ALPHA + (1f - WHEEL_EDGE_ALPHA) * c * c
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items.getOrElse(index % state.size) { "" },
                        style = (if (selected.value) ThingsTheme.type.wheelSelected else ThingsTheme.type.wheel)
                            .copy(color = ThingsTheme.colors.overlayContent)
                    )
                }
            }
        }
    }
}
